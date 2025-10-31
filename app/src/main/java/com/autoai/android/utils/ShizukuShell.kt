package com.autoai.android.utils

import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku Shell命令执行工具
 * 使用反射访问私有API，确保兼容性
 */
object ShizukuShell {
    
    /**
     * 执行Shell命令
     */
    fun executeCommand(vararg command: String): ShellResult {
        return try {
            // 使用反射调用私有方法newProcess
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            
            val process = newProcessMethod.invoke(null, command, null, null) as Process
            
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            process.destroy()
            
            if (exitCode == 0) {
                ShellResult(true, stdout, "")
            } else {
                Timber.w("命令执行失败: ${command.joinToString(" ")}, exitCode=$exitCode")
                ShellResult(false, stdout, stderr.ifBlank { "命令执行失败 (exitCode=$exitCode)" })
            }
        } catch (e: Exception) {
            Timber.e(e, "执行Shell命令异常: ${command.joinToString(" ")}")
            ShellResult(false, "", e.message ?: "未知错误")
        }
    }
    
    /**
     * 执行Shell命令（带超时）
     */
    fun executeCommandWithTimeout(
        timeoutSeconds: Long,
        vararg command: String
    ): ShellResult {
        return try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            
            val process = newProcessMethod.invoke(null, command, null, null) as Process
            
            // 等待进程完成，带超时
            val finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
            
            if (!finished) {
                process.destroy()
                Timber.w("命令执行超时: ${command.joinToString(" ")}")
                return ShellResult(false, "", "命令执行超时")
            }
            
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.exitValue()
            
            if (exitCode == 0) {
                ShellResult(true, stdout, "")
            } else {
                Timber.w("命令执行失败: ${command.joinToString(" ")}, exitCode=$exitCode")
                ShellResult(false, stdout, stderr.ifBlank { "命令执行失败 (exitCode=$exitCode)" })
            }
        } catch (e: Exception) {
            Timber.e(e, "执行Shell命令异常: ${command.joinToString(" ")}")
            ShellResult(false, "", e.message ?: "未知错误")
        }
    }
}

/**
 * Shell命令执行结果
 */
data class ShellResult(
    val isSuccess: Boolean,
    val output: String,
    val errorMessage: String
)
