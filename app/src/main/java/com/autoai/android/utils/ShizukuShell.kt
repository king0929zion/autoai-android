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

    private val newProcessMethod: Method? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val candidates: List<Array<Class<*>>> = listOf(
            arrayOf(
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
                Int::class.javaPrimitiveType!!
            ),
            arrayOf(
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ),
            arrayOf(
                Array<String>::class.java,
                Array<String>::class.java
            ),
            arrayOf(Array<String>::class.java)
        )

        val clazz = Shizuku::class.java
        for (signature in candidates) {
            val method = runCatching {
                clazz.getDeclaredMethod("newProcess", *signature).apply { isAccessible = true }
            }.getOrNull()
            if (method != null) {
                Timber.i("ShizukuShell resolved newProcess signature with ${signature.size} parameters")
                return@lazy method
            }
        }
        Timber.e("Unable to resolve Shizuku.newProcess reflection signature")
        null
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
            if (newProcessMethod == null) {
                Timber.e("Cannot execute command because Shizuku.newProcess could not be resolved")
                return ShellResult(
                    isSuccess = false,
                    output = "",
                    errorMessage = "Shizuku command execution is not supported on this version of Shizuku"
                )
            }

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
        val method = newProcessMethod
            ?: error("Shizuku newProcess method not available. Check Shizuku version compatibility.")

        val args = command.map { it }.toTypedArray()
        val parameters = method.parameterTypes.size

        @Suppress("UNCHECKED_CAST")
        val process = when (parameters) {
            4 -> method.invoke(null, args, null, null, 0) as Process
            3 -> method.invoke(null, args, null, null) as Process
            2 -> method.invoke(null, args, null) as Process
            1 -> method.invoke(null, args) as Process
            else -> throw IllegalStateException("Unsupported Shizuku.newProcess signature with $parameters parameters")
        }
        return process
    }
}

data class ShellResult(
    val isSuccess: Boolean,
    val output: String,
    val errorMessage: String,
    val rawOutput: ByteArray = ByteArray(0)
)
