package com.autoai.android.utils

import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.reflect.Method
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Execute shell commands via Shizuku with unified result wrapping and timeout handling.
 */
object ShizukuShell {

    private val streamCharset: Charset = Charsets.ISO_8859_1
    private val errorStreamCharset: Charset = Charsets.UTF_8

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

    private fun runCommand(timeoutSeconds: Long?, command: Array<out String>): ShellResult {
        if (command.isEmpty()) {
            Timber.w("Attempted to execute empty command")
            return ShellResult(isSuccess = false, output = "", errorMessage = "Command cannot be empty")
        }

        val commandLine = command.joinToString(" ")
        return try {
            val process = spawnProcess(command)
            try {
                val stdoutBuffer = ByteArrayOutputStream()
                val stderrBuffer = ByteArrayOutputStream()

                val stdoutThread = thread(start = true, isDaemon = true, name = "ShizukuShell-stdout") {
                    runCatching {
                        process.inputStream.use { input ->
                            input.copyTo(stdoutBuffer)
                        }
                    }.onFailure {
                        Timber.w(it, "Failed to read command standard output: $commandLine")
                    }
                }

                val stderrThread = thread(start = true, isDaemon = true, name = "ShizukuShell-stderr") {
                    runCatching {
                        process.errorStream.use { input ->
                            input.copyTo(stderrBuffer)
                        }
                    }.onFailure {
                        Timber.w(it, "Failed to read command error output: $commandLine")
                    }
                }

                val finished = timeoutSeconds?.let {
                    process.waitFor(it, TimeUnit.SECONDS)
                } ?: run {
                    process.waitFor()
                    true
                }

                if (!finished) {
                    Timber.w("Command timed out: $commandLine (timeout=${timeoutSeconds}s)")
                    process.destroy()
                    runCatching { process.inputStream.close() }
                    runCatching { process.errorStream.close() }
                    stdoutThread.join(200)
                    stderrThread.join(200)
                    return ShellResult(isSuccess = false, output = "", errorMessage = "Command timed out")
                }

                stdoutThread.join()
                stderrThread.join()

                val stdoutBytes = stdoutBuffer.toByteArray()
                val stderrBytes = stderrBuffer.toByteArray()
                val stdout = stdoutBytes.toString(streamCharset)
                val stderr = stderrBytes.toString(errorStreamCharset)

                val exitCode = process.exitValue()
                if (exitCode == 0) {
                    ShellResult(
                        isSuccess = true,
                        output = stdout,
                        errorMessage = "",
                        rawOutput = stdoutBytes
                    )
                } else {
                    Timber.w("Command failed: $commandLine, exitCode=$exitCode")
                    ShellResult(
                        isSuccess = false,
                        output = stdout,
                        errorMessage = stderr.ifBlank { "Command failed (exitCode=$exitCode)" },
                        rawOutput = stdoutBytes
                    )
                }
            } finally {
                if (process.isAlive) {
                    process.destroyForcibly()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Shell command execution error: $commandLine")
            ShellResult(isSuccess = false, output = "", errorMessage = e.message ?: "Unknown error")
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
    val errorMessage: String,
    val rawOutput: ByteArray = ByteArray(0)
)
