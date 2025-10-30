package com.autoai.android.utils

import timber.log.Timber

/**
 * 错误处理工具类
 */
object ErrorHandler {
    
    /**
     * 将错误转换为用户友好的消息
     */
    fun getUserFriendlyMessage(error: Throwable): String {
        return when {
            error.message?.contains("API", ignoreCase = true) == true -> {
                "API 调用失败，请检查网络连接和 API Key 配置"
            }
            error.message?.contains("Shizuku", ignoreCase = true) == true -> {
                "Shizuku 服务异常，请确保 Shizuku 正在运行"
            }
            error.message?.contains("Permission", ignoreCase = true) == true -> {
                "权限不足，请授予必要的权限"
            }
            error.message?.contains("Network", ignoreCase = true) == true -> {
                "网络连接失败，请检查网络设置"
            }
            error.message?.contains("timeout", ignoreCase = true) == true -> {
                "操作超时，请稍后重试"
            }
            error is java.net.UnknownHostException -> {
                "无法连接到服务器，请检查网络"
            }
            error is java.net.SocketTimeoutException -> {
                "连接超时，请重试"
            }
            error is java.io.IOException -> {
                "IO 操作失败: ${error.message}"
            }
            else -> {
                error.message ?: "未知错误，请查看日志"
            }
        }
    }

    /**
     * 记录错误并返回友好消息
     */
    fun handleError(tag: String, error: Throwable, context: String = ""): String {
        val message = if (context.isNotBlank()) {
            "$context: ${error.message}"
        } else {
            error.message ?: "Unknown error"
        }
        
        Timber.tag(tag).e(error, message)
        return getUserFriendlyMessage(error)
    }

    /**
     * 安全执行代码块
     */
    inline fun <T> safely(
        tag: String,
        defaultValue: T,
        block: () -> T
    ): T {
        return try {
            block()
        } catch (e: Exception) {
            Timber.tag(tag).e(e, "执行失败")
            defaultValue
        }
    }
}
