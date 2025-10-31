package com.autoai.android.decision

import com.autoai.android.BuildConfig
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 多模态大模型（硅基流动）客户端。
 */
@Singleton
class VLMClient @Inject constructor() {

    companion object {
        private const val DEFAULT_BASE_URL = "https://api.siliconflow.cn/"
        private const val DEFAULT_MODEL = "Qwen/Qwen2.5-VL-7B-Instruct"
        private const val DEFAULT_TEMPERATURE = 0.3f
        private const val DEFAULT_MAX_TOKENS = 2000
        private const val TIMEOUT_SECONDS = 60L
        private const val TEST_SYSTEM_PROMPT = "你是一个用于健康检查的助手，只需判断服务是否可用。"
        private const val TEST_USER_PROMPT = "请仅回复“OK”。"
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.HEADERS
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var apiKey: String = ""
    private var baseUrl: String = DEFAULT_BASE_URL
    private var modelName: String = DEFAULT_MODEL
    private var serviceBaseUrl: String = DEFAULT_BASE_URL
    @Volatile
    private var apiService: VLMApiService = createService(DEFAULT_BASE_URL)

    fun configure(apiKey: String, baseUrl: String? = null, modelName: String? = null) {
        this.apiKey = apiKey.trim()

        var needsRebuild = false
        baseUrl?.takeIf { it.isNotBlank() }?.let {
            val normalized = normalizeBaseUrl(it)
            if (normalized != this.baseUrl) {
                this.baseUrl = normalized
                needsRebuild = true
            }
        }

        modelName?.takeIf { it.isNotBlank() }?.let {
            this.modelName = it.trim()
        }

        if (needsRebuild || serviceBaseUrl != this.baseUrl) {
            apiService = createService(this.baseUrl)
            serviceBaseUrl = this.baseUrl
        }

        Timber.d("VLMClient 配置完成: baseUrl=$baseUrl, model=$modelName")
    }

    suspend fun chat(
        systemPrompt: String,
        userPrompt: String,
        imageDataUri: String,
        temperature: Float = DEFAULT_TEMPERATURE,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("尚未配置 API Key"))
        }

        return@withContext runCatching {
            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(
                        role = "user",
                        content = listOf(
                            MessageContent.TextContent(type = "text", text = userPrompt),
                            MessageContent.ImageContent(
                                type = "image_url",
                                imageUrl = ImageUrl(imageDataUri)
                            )
                        )
                    )
                ),
                temperature = temperature,
                maxTokens = maxTokens
            )

            Timber.d("发送多模态请求，model=$modelName temperature=$temperature")
            val response = apiService.chat(authorization = "Bearer $apiKey", request = request)
            if (response.choices.isEmpty()) {
                error("API 响应为空")
            }
            response.choices.first().message.content
        }.onFailure { Timber.e(it, "VLM 多模态调用失败") }
    }

    suspend fun chatText(
        systemPrompt: String,
        userPrompt: String,
        temperature: Float = DEFAULT_TEMPERATURE,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("尚未配置 API Key"))
        }

        return@withContext runCatching {
            val request = ChatRequest(
                model = modelName,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                temperature = temperature,
                maxTokens = maxTokens
            )

            Timber.d("发送纯文本请求，model=$modelName temperature=$temperature")
            val response = apiService.chat(authorization = "Bearer $apiKey", request = request)
            if (response.choices.isEmpty()) {
                error("API 响应为空")
            }
            response.choices.first().message.content
        }.onFailure { Timber.e(it, "VLM 文本调用失败") }
    }

    suspend fun testConnection(
        temperature: Float = DEFAULT_TEMPERATURE,
        maxTokens: Int = 64
    ): Result<ConnectionDiagnostics> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("尚未配置 API Key"))
        }

        val sanitizedTemp = temperature.coerceIn(0f, 1f)
        val sanitizedTokens = maxTokens.coerceIn(16, 512)
        val start = System.currentTimeMillis()

        return@withContext chatText(
            systemPrompt = TEST_SYSTEM_PROMPT,
            userPrompt = TEST_USER_PROMPT,
            temperature = sanitizedTemp,
            maxTokens = sanitizedTokens
        ).map { response ->
            val latency = System.currentTimeMillis() - start
            ConnectionDiagnostics(
                latencyMs = latency,
                responsePreview = response.replace("\n", " ").take(64)
            )
        }
    }

    data class ConnectionDiagnostics(
        val latencyMs: Long,
        val responsePreview: String
    )

    private fun createService(baseUrl: String): VLMApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(VLMApiService::class.java)
    }

    private fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}

private interface VLMApiService {
    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse
}

private data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Float,
    @SerializedName("max_tokens")
    val maxTokens: Int
)

private data class Message(
    val role: String,
    val content: Any
) {
    constructor(role: String, content: String) : this(role, content as Any)
    constructor(role: String, content: List<MessageContent>) : this(role, content as Any)
}

private sealed class MessageContent {
    data class TextContent(
        val type: String,
        val text: String
    ) : MessageContent()

    data class ImageContent(
        val type: String,
        @SerializedName("image_url")
        val imageUrl: ImageUrl
    ) : MessageContent()
}

private data class ImageUrl(
    val url: String
)

private data class ChatResponse(
    val choices: List<Choice>,
    val usage: Usage?
)

private data class Choice(
    val message: ResponseMessage
)

private data class ResponseMessage(
    val content: String
)

private data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)
