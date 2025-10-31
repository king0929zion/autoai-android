package com.autoai.android.perception

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.autoai.android.permission.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 屏幕截图捕获器
 * 
 * 使用 Shizuku 执行 screencap 命令捕获屏幕内容
 */
@Singleton
class ScreenCapture @Inject constructor(
    private val shizukuManager: ShizukuManager
) {
    companion object {
        private const val TAG = "ScreenCapture"
        private const val DEFAULT_QUALITY = 80
        private const val MAX_SIZE_KB = 500
    }

    /**
     * 捕获当前屏幕
     * 
     * @return 成功返回 Bitmap，失败返回错误信息
     */
    suspend fun captureScreen(): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            if (!shizukuManager.isShizukuAvailable()) {
                return@withContext Result.failure(Exception("Shizuku 不可用"))
            }

            Timber.d("开始捕获屏幕...")
            
            // 使用 Shizuku 执行 screencap 命令，增加错误处理
            val process = Shizuku.newProcess(
                arrayOf("sh", "-c", "screencap -p"),
                null,
                null
            )
            
            // 增加超时检查
            val hasFinished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            if (!hasFinished) {
                process.destroy()
                return@withContext Result.failure(Exception("截图超时"))
            }
            
            // 读取输出流为 Bitmap，增加资源管理
            val bitmap = process.inputStream.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
            
            if (bitmap != null) {
                Timber.d("截图成功: ${bitmap.width}x${bitmap.height}")
                Result.success(bitmap)
            } else {
                Timber.e("截图失败: Bitmap 为 null")
                Result.failure(Exception("截图失败"))
            }
        } catch (e: Exception) {
            Timber.e(e, "截图捕获异常")
            Result.failure(e)
        }
    }

    /**
     * 压缩 Bitmap 到指定大小
     * 
     * @param bitmap 原始图片
     * @param maxSizeKB 最大大小（KB）
     * @return 压缩后的 Bitmap
     */
    fun compressBitmap(bitmap: Bitmap, maxSizeKB: Int = MAX_SIZE_KB): Bitmap {
        val baos = ByteArrayOutputStream()
        var quality = DEFAULT_QUALITY
        
        // 循环压缩，直到小于指定大小
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        
        while (baos.toByteArray().size / 1024 > maxSizeKB && quality > 10) {
            baos.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        }
        
        val compressedData = baos.toByteArray()
        Timber.d("压缩完成: 质量=$quality, 大小=${compressedData.size / 1024}KB")
        
        return BitmapFactory.decodeByteArray(compressedData, 0, compressedData.size)
    }

    /**
     * 将 Bitmap 转换为 Base64 字符串
     * 
     * @param bitmap 图片
     * @param compress 是否压缩
     * @return Base64 字符串
     */
    fun bitmapToBase64(bitmap: Bitmap, compress: Boolean = true): String {
        val processedBitmap = if (compress) compressBitmap(bitmap) else bitmap
        
        val baos = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, DEFAULT_QUALITY, baos)
        val bytes = baos.toByteArray()
        
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        Timber.d("转换为 Base64: ${base64.length} 字符")
        
        return base64
    }

    /**
     * 保存截图到本地文件
     * 
     * @param bitmap 图片
     * @param filename 文件名
     * @return 成功返回 File，失败返回错误信息
     */
    suspend fun saveScreenshot(bitmap: Bitmap, filename: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 保存到应用私有目录
            val dir = File("/sdcard/AutoAI/screenshots")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            
            val file = File(dir, filename)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, DEFAULT_QUALITY, out)
            }
            
            Timber.d("截图已保存: ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            Timber.e(e, "保存截图失败")
            Result.failure(e)
        }
    }

    /**
     * 获取截图的数据 URI（用于 API 调用）
     * 
     * @param bitmap 图片
     * @return data:image/jpeg;base64,xxx 格式的字符串
     */
    fun getDataUri(bitmap: Bitmap): String {
        val base64 = bitmapToBase64(bitmap)
        return "data:image/jpeg;base64,$base64"
    }
}
