@file:OptIn(ExperimentalMaterial3Api::class)

package com.autoai.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoai.android.decision.TestConnectionResult
import com.autoai.android.decision.VLMClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置界面
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val modelName by viewModel.modelName.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "API 配置",
                style = MaterialTheme.typography.headlineSmall
            )

            // API Key
            OutlinedTextField(
                value = apiKey,
                onValueChange = viewModel::updateApiKey,
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("输入硅基流动 API Key") }
            )

            // Base URL
            OutlinedTextField(
                value = baseUrl,
                onValueChange = viewModel::updateBaseUrl,
                label = { Text("API 基础 URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("https://api.siliconflow.cn/") }
            )

            // Model Name
            OutlinedTextField(
                value = modelName,
                onValueChange = viewModel::updateModelName,
                label = { Text("模型名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Qwen/Qwen2-VL-7B-Instruct") }
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 高级配置
            Text(
                text = "高级配置",
                style = MaterialTheme.typography.headlineSmall
            )
            
            // Temperature 滑动条
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Temperature: ${String.format("%.2f", temperature)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "控制随机性，较低值更稳定，较高值更创意",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = temperature,
                    onValueChange = viewModel::updateTemperature,
                    valueRange = 0f..1f,
                    steps = 19
                )
            }
            
            // Max Tokens 滑动条
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Max Tokens: $maxTokens",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "最大生成长度，较高值支持更长的回复",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { viewModel.updateMaxTokens(it.toInt()) },
                    valueRange = 100f..4000f,
                    steps = 39
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 外观设置
            Text(
                text = "外观设置",
                style = MaterialTheme.typography.headlineSmall
            )
            
            // 深色模式开关
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "深色模式",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "保护眼睛，节省电量",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = viewModel::updateDarkTheme
                    )
                }
            }

            // 操作按钮
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.saveSettings() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存设置")
                }

                OutlinedButton(
                    onClick = { viewModel.testConnection() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在测试…")
                    } else {
                        Text("测试 API 连接")
                    }
                }
            }

            testResult?.let { status ->
                ApiTestResultCard(status)
            }

            // 错误提示
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 保存成功提示
            if (isSaved) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅ 设置已保存",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 使用说明
            Text(
                text = "使用说明",
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. 注册硅基流动账号",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "访问 https://cloud.siliconflow.cn/ 注册账号",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "2. 获取 API Key",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "在控制台创建 API Key 并复制",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "3. 激活 Shizuku",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "确保 Shizuku 应用已安装并正在运行",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 推荐配置
            Text(
                text = "推荐配置",
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• 模型: Qwen/Qwen2-VL-7B-Instruct",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Temperature: 0.3",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• 费用: 约 ¥0.002/千 tokens",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * 设置 ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val vlmClient: VLMClient
) : ViewModel() {

    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val MODEL_NAME = stringPreferencesKey("model_name")
        
        private const val DEFAULT_BASE_URL = "https://api.siliconflow.cn/"
        private const val DEFAULT_MODEL = "Qwen/Qwen2-VL-7B-Instruct"
    }

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _baseUrl = MutableStateFlow(DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl

    private val _modelName = MutableStateFlow(DEFAULT_MODEL)
    val modelName: StateFlow<String> = _modelName
    
    private val _temperature = MutableStateFlow(0.3f)
    val temperature: StateFlow<Float> = _temperature
    
    private val _maxTokens = MutableStateFlow(1000)
    val maxTokens: StateFlow<Int> = _maxTokens
    
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection

    private val _testResult = MutableStateFlow<TestConnectionResult?>(null)
    val testResult: StateFlow<TestConnectionResult?> = _testResult

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                dataStore.data.collect { preferences ->
                    _apiKey.value = preferences[API_KEY] ?: ""
                    _baseUrl.value = preferences[BASE_URL] ?: DEFAULT_BASE_URL
                    _modelName.value = preferences[MODEL_NAME] ?: DEFAULT_MODEL
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载设置失败: ${e.message}"
            }
        }
    }

    fun updateApiKey(value: String) {
        _apiKey.value = value
        _isSaved.value = false
        _errorMessage.value = null
        _testResult.value = null
    }

    fun updateBaseUrl(value: String) {
        _baseUrl.value = value
        _isSaved.value = false
        _errorMessage.value = null
        _testResult.value = null
    }

    fun updateModelName(value: String) {
        _modelName.value = value
        _isSaved.value = false
        _errorMessage.value = null
        _testResult.value = null
    }
    
    fun updateTemperature(value: Float) {
        _temperature.value = value
        _isSaved.value = false
    }
    
    fun updateMaxTokens(value: Int) {
        _maxTokens.value = value
        _isSaved.value = false
    }
    
    fun updateDarkTheme(value: Boolean) {
        _isDarkTheme.value = value
        _isSaved.value = false
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                // 验证输入
                if (_apiKey.value.isBlank()) {
                    _errorMessage.value = "API Key 不能为空"
                    return@launch
                }
                
                if (_baseUrl.value.isBlank()) {
                    _errorMessage.value = "Base URL 不能为空"
                    return@launch
                }
                
                if (!_baseUrl.value.startsWith("http://") && !_baseUrl.value.startsWith("https://")) {
                    _errorMessage.value = "Base URL 必须以 http:// 或 https:// 开头"
                    return@launch
                }
                
                if (_modelName.value.isBlank()) {
                    _errorMessage.value = "模型名称不能为空"
                    return@launch
                }
                
                // 保存配置
                dataStore.edit { preferences ->
                    preferences[API_KEY] = _apiKey.value
                    preferences[BASE_URL] = _baseUrl.value
                    preferences[MODEL_NAME] = _modelName.value
                }
                
                // 更新 VLMClient 配置
                vlmClient.configure(
                    apiKey = _apiKey.value,
                    baseUrl = _baseUrl.value,
                    modelName = _modelName.value
                )
                
                _isSaved.value = true
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "保存失败: ${e.message}"
            }
        }
    }

    fun testConnection() {
        if (_isTestingConnection.value) return

        viewModelScope.launch {
            try {
                if (_apiKey.value.isBlank()) {
                    _errorMessage.value = "请先填写 API Key"
                    return@launch
                }

                if (_baseUrl.value.isBlank()) {
                    _errorMessage.value = "请先填写 Base URL"
                    return@launch
                }

                if (_modelName.value.isBlank()) {
                    _errorMessage.value = "请先填写模型名称"
                    return@launch
                }

                _isTestingConnection.value = true
                _errorMessage.value = null
                _testResult.value = null

                vlmClient.configure(
                    apiKey = _apiKey.value,
                    baseUrl = _baseUrl.value,
                    modelName = _modelName.value
                )

                vlmClient.testConnection()
                    .onSuccess { result ->
                        _testResult.value = result
                    }
                    .onFailure { throwable ->
                        val reason = throwable.message?.takeIf { it.isNotBlank() } ?: "未知错误"
                        _errorMessage.value = "连接测试失败: $reason"
                    }
            } catch (e: Exception) {
                val reason = e.message?.takeIf { it.isNotBlank() } ?: "未知错误"
                _errorMessage.value = "连接测试失败: $reason"
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

@Composable
private fun ApiTestResultCard(result: TestConnectionResult) {
    val (icon, message, containerColor, contentColor) = if (result.targetModelAvailable) {
        TestResultVisuals(
            icon = "✅",
            message = "API 连接正常，检测到 ${result.availableModelCount} 个模型",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    } else {
        TestResultVisuals(
            icon = "⚠️",
            message = "API 可访问，但未找到配置的模型",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
                Text(
                    text = "当前模型: ${if (result.targetModelAvailable) "可用" else "未找到"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

private data class TestResultVisuals(
    val icon: String,
    val message: String,
    val containerColor: Color,
    val contentColor: Color
)
