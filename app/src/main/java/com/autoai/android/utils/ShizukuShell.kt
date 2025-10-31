package com.autoai.android.utils

import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.reflect.Method
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * 使用 Shizuku 执行 Shell 命令，提供统一的结果封装与超时处理。
 */
object ShizukuShell {

    private val streamCharset: Charset = Charsets.ISO_8859_1

    private val newProcessMethod: Method by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }
    }

    fun executeCommand(vararg command: String): ShellResult =
        runCommand(timeoutSeconds = null, command = command)

    fun executeCommandWithTimeout(timeoutSeconds: Long, vararg command: String): ShellResult =
        runCommand(timeoutSeconds = timeoutSeconds, command = command)

    fun executeCommandForBinary(vararg command: String): ShellBinaryResult =
        runCommandBinary(timeoutSeconds = null, command = command)

    fun executeCommandForBinaryWithTimeout(
        timeoutSeconds: Long,
        vararg command: String
    ): ShellBinaryResult = runCommandBinary(timeoutSeconds = timeoutSeconds, command = command)

    private fun runCommand(timeoutSeconds: Long?, command: Array<out String>): ShellResult {
        if (command.isEmpty()) {
            Timber.w("尝试执行空命令")
            return ShellResult(isSuccess = false, output = "", errorMessage = "命令不能为空")
        }

        val commandLine = command.joinToString(" ")
        return try {
            val process = spawnProcess(command)
            try {
                val stdoutBuilder = StringBuilder()
                val stderrBuilder = StringBuilder()

                val stdoutThread = thread(start = true, isDaemon = true, name = "ShizukuShell-stdout") {
                    runCatching {
                        process.inputStream.bufferedReader(streamCharset).use { reader ->
                            stdoutBuilder.append(reader.readText())
                        }
                    }.onFailure {
                        Timber.w(it, "读取命令输出失败: $commandLine")
                    }
                }

                val stderrThread = thread(start = true, isDaemon = true, name = "ShizukuShell-stderr") {
                    runCatching {
                        process.errorStream.bufferedReader(streamCharset).use { reader ->
                            stderrBuilder.append(reader.readText())
                        }
                    }.onFailure {
                        Timber.w(it, "读取命令错误输出失败: $commandLine")
                    }
                }

                val finished = timeoutSeconds?.let {
                    process.waitFor(it, TimeUnit.SECONDS)
                } ?: run {
                    process.waitFor()
                    true
                }

                if (!finished) {
                    Timber.w("命令执行超时: $commandLine (timeout=${timeoutSeconds}s)")
                    process.destroy()
                    process.inputStream.close()
                    process.errorStream.close()
                    stdoutThread.join(200)
                    stderrThread.join(200)
                    return ShellResult(isSuccess = false, output = "", errorMessage = "命令执行超时")
                }

                stdoutThread.join()
                stderrThread.join()

                val exitCode = process.exitValue()
                val stdout = stdoutBuilder.toString()
                val stderr = stderrBuilder.toString()

                if (exitCode == 0) {
                    ShellResult(isSuccess = true, output = stdout, errorMessage = "")
                } else {
                    Timber.w("命令执行失败: $commandLine, exitCode=$exitCode")
                    ShellResult(
                        isSuccess = false,
                        output = stdout,
                        errorMessage = stderr.ifBlank { "命令执行失败 (exitCode=$exitCode)" }
                    )
                }
            } finally {
                if (process.isAlive) {
                    process.destroyForcibly()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "执行Shell命令异常: $commandLine")
            ShellResult(isSuccess = false, output = "", errorMessage = e.message ?: "未知错误")
        }
    }

    private fun runCommandBinary(timeoutSeconds: Long?, command: Array<out String>): ShellBinaryResult {
        if (command.isEmpty()) {
            Timber.w("尝试执行空命令")
            return ShellBinaryResult(isSuccess = false, output = byteArrayOf(), errorMessage = "命令不能为空")
        }

        val commandLine = command.joinToString(" ")
        return try {
            val process = spawnProcess(command)
            try {
                val stdoutBytes = ByteArrayOutputStream()
                val stderrBuilder = StringBuilder()

                val stdoutThread = thread(start = true, isDaemon = true, name = "ShizukuShell-stdout-bin") {
                    runCatching {
                        process.inputStream.use { input ->
                            input.copyTo(stdoutBytes)
                        }
                    }.onFailure {
                        Timber.w(it, "读取命令输出失败: $commandLine")
                    }
                }

                val stderrThread = thread(start = true, isDaemon = true, name = "ShizukuShell-stderr-bin") {
                    runCatching {
                        process.errorStream.bufferedReader(streamCharset).use { reader ->
                            stderrBuilder.append(reader.readText())
                        }
                    }.onFailure {
                        Timber.w(it, "读取命令错误输出失败: $commandLine")
                    }
                }

                val finished = timeoutSeconds?.let {
                    process.waitFor(it, TimeUnit.SECONDS)
                } ?: run {
                    process.waitFor()
                    true
                }

                if (!finished) {
                    Timber.w("命令执行超时: $commandLine (timeout=${timeoutSeconds}s)")
                    process.destroy()
                    process.inputStream.close()
                    process.errorStream.close()
                    stdoutThread.join(200)
                    stderrThread.join(200)
                    return ShellBinaryResult(
                        isSuccess = false,
                        output = byteArrayOf(),
                        errorMessage = "命令执行超时"
                    )
                }

                stdoutThread.join()
                stderrThread.join()

                val exitCode = process.exitValue()
                val stdout = stdoutBytes.toByteArray()
                val stderr = stderrBuilder.toString()

                if (exitCode == 0) {
                    ShellBinaryResult(isSuccess = true, output = stdout, errorMessage = "")
                } else {
                    Timber.w("命令执行失败: $commandLine, exitCode=$exitCode")
                    ShellBinaryResult(
                        isSuccess = false,
                        output = stdout,
                        errorMessage = stderr.ifBlank { "命令执行失败 (exitCode=$exitCode)" }
                    )
                }
            } finally {
                if (process.isAlive) {
                    process.destroyForcibly()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "执行Shell命令异常: $commandLine")
            ShellBinaryResult(isSuccess = false, output = byteArrayOf(), errorMessage = e.message ?: "未知错误")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun spawnProcess(command: Array<out String>): Process {
        val args = command.map { it }.toTypedArray()
        return newProcessMethod.invoke(null, args, null, null) as Process
    }
}

data class ShellResult(
    val isSuccess: Boolean,
    val output: String,
    val errorMessage: String
)

data class ShellBinaryResult(
    val isSuccess: Boolean,
    val output: ByteArray,
    val errorMessage: String
)
