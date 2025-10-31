package com.autoai.android.perception

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.autoai.android.permission.ShizukuManager
import com.autoai.android.utils.ShizukuShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责通过 Shizuku 执行 screencap 命令并获取屏幕截图。
 */
@Singleton
class ScreenCapture @Inject constructor(
    private val shizukuManager: ShizukuManager
) {

    suspend fun captureScreen(): Result<Bitmap> = withContext(Dispatchers.IO) {
        if (!shizukuManager.isShizukuAvailable()) {
            return@withContext Result.failure(Exception("Shizuku 未就绪"))
        }

        Timber.d("开始捕获屏幕…")

        val inMemoryResult = ShizukuShell.executeCommandWithTimeout(12, "screencap", "-p")
        val screenshotBytes = when {
            inMemoryResult.isSuccess && inMemoryResult.rawOutput.isNotEmpty() -> inMemoryResult.rawOutput
            else -> captureViaTempFile().getOrElse { error -> return@withContext Result.failure(error) }
        }

        val bitmap = runCatching {
            BitmapFactory.decodeByteArray(screenshotBytes, 0, screenshotBytes.size)
        }.getOrElse { error ->
            Timber.e(error, "解码截图数据失败")
            null
        }

        if (bitmap != null) {
            Timber.d("截图成功: ${bitmap.width}x${bitmap.height}")
            Result.success(bitmap)
        } else {
            Result.failure(Exception("无法解码截图数据"))
        }
    }

    fun compressBitmap(bitmap: Bitmap, maxSizeKB: Int = MAX_SIZE_KB): Bitmap {
        val output = ByteArrayOutputStream()
        var quality = DEFAULT_QUALITY

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        while (output.size() / 1024 > maxSizeKB && quality > 10) {
            output.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        }

        Timber.d("压缩完成: 质量=$quality, 大小=${output.size() / 1024}KB")
        return BitmapFactory.decodeByteArray(output.toByteArray(), 0, output.size())
    }

    fun bitmapToBase64(bitmap: Bitmap, compress: Boolean = true): String {
        val processed = if (compress) compressBitmap(bitmap) else bitmap
        val output = ByteArrayOutputStream()
        processed.compress(Bitmap.CompressFormat.JPEG, DEFAULT_QUALITY, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun saveScreenshot(bitmap: Bitmap, filename: String): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File("/sdcard/AutoAI/screenshots")
                if (!dir.exists()) dir.mkdirs()

                val file = File(dir, filename)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, DEFAULT_QUALITY, out)
                }
                Timber.d("截图已保存：${file.absolutePath}")
                file
            }
        }

    fun getDataUri(bitmap: Bitmap): String {
        return "data:image/jpeg;base64,${bitmapToBase64(bitmap)}"
    }

    private fun captureViaTempFile(): Result<ByteArray> {
        val tempDir = "/data/local/tmp/autoai"
        val tempFile = "$tempDir/screenshot.png"

        ShizukuShell.executeCommand("mkdir", "-p", tempDir)

        val capture = ShizukuShell.executeCommandWithTimeout(12, "screencap", "-p", tempFile)
        if (!capture.isSuccess) {
            Timber.e("截图命令失败: ${capture.errorMessage}")
            return Result.failure(Exception("截图失败：${capture.errorMessage}"))
        }

        val readResult = ShizukuShell.executeCommandWithTimeout(10, "cat", tempFile)
        ShizukuShell.executeCommand("rm", "-f", tempFile)

        return if (readResult.isSuccess && readResult.rawOutput.isNotEmpty()) {
            Result.success(readResult.rawOutput)
        } else {
            val reason = readResult.errorMessage.ifBlank { "读取截图失败" }
            Timber.e("读取临时截图失败: $reason")
            Result.failure(Exception(reason))
        }
    }

    companion object {
        private const val DEFAULT_QUALITY = 80
        private const val MAX_SIZE_KB = 500
    }
}
