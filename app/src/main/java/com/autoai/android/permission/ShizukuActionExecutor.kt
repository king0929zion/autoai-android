package com.autoai.android.permission

import android.view.KeyEvent
import com.autoai.android.data.model.Action
import com.autoai.android.data.model.ActionResult
import com.autoai.android.utils.ShizukuShell
import com.autoai.android.utils.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes privileged commands via Shizuku so AutoAI can control the device without ADB.
 */
@Singleton
class ShizukuActionExecutor @Inject constructor(
    private val shizukuManager: ShizukuManager
) : ControlActionExecutor {

    override val label: String = "Shizuku mode"

    override suspend fun isReady(): Boolean = shizukuManager.isShizukuAvailable()

    override suspend fun executeAction(action: Action): ActionResult = withContext(Dispatchers.IO) {
        if (!shizukuManager.isShizukuAvailable()) {
            return@withContext ActionResult.failure(
                message = "Shizuku permission is missing. Open the Shizuku app and grant AutoAI permission.",
                needsUserConfirmation = true
            )
        }

        val startedAt = System.currentTimeMillis()
        val operation = runCatching {
            when (action) {
                is Action.Click -> executeClick(action.x, action.y)
                is Action.LongClick -> executeLongClick(action.x, action.y, action.durationMs)
                is Action.Swipe -> executeSwipe(action)
                is Action.Input -> executeInput(action.text)
                is Action.PressKey -> executePressKey(action.keyCode, action.keyName)
                is Action.OpenApp -> executeOpenApp(action.packageName, action.appName)
                is Action.Wait -> executeWait(action.durationMs)
                Action.GoBack -> executePressKey(KeyEvent.KEYCODE_BACK, "Back")
                is Action.Complete -> ActionResult.success(action.message.ifBlank { "Task completed" })
                is Action.Error -> ActionResult.failure(action.message, needsUserConfirmation = !action.recoverable)
                is Action.RequestUserHelp -> ActionResult.failure(
                    message = "Manual help required: ${action.reason}",
                    needsUserConfirmation = true
                )
            }
        }.getOrElse { error ->
            Timber.e(error, "Shizuku command failed: $action")
            ActionResult.failure("Shizuku command failed: ${error.message ?: "unknown error"}")
        }

        operation.copy(executionTimeMs = System.currentTimeMillis() - startedAt)
    }

    override suspend fun getCurrentApp(): Result<String> = withContext(Dispatchers.IO) {
        if (!shizukuManager.isShizukuAvailable()) {
            return@withContext Result.failure(IllegalStateException("Shizuku is not ready"))
        }

        runCatching {
            val dump = executeShellCommand("dumpsys window windows")
            if (!dump.isSuccess) {
                error(dump.errorMessage.ifBlank { "Unable to read window manager state." })
            }
            parsePackageName(dump.output).also { pkg ->
                require(pkg.isNotBlank()) { "Unable to identify the foreground application." }
            }
        }.onFailure { Timber.e(it, "Failed to resolve foreground app with Shizuku") }
    }

    private suspend fun executeClick(x: Int, y: Int): ActionResult {
        Timber.d("Shizuku tap: (%d, %d)", x, y)
        val result = executeShellCommand("input tap $x $y")
        return if (result.isSuccess) {
            delay(DELAY_AFTER_CLICK)
            ActionResult.success("Tapped ($x, $y)")
        } else {
            ActionResult.failure("Tap failed: ${result.errorMessage}")
        }
    }

    private suspend fun executeLongClick(x: Int, y: Int, durationMs: Long): ActionResult {
        Timber.d("Shizuku long press: (%d, %d) for %dms", x, y, durationMs)
        val result = executeShellCommand("input swipe $x $y $x $y $durationMs")
        return if (result.isSuccess) {
            delay(DELAY_AFTER_CLICK)
            ActionResult.success("Long press executed at ($x, $y)")
        } else {
            ActionResult.failure("Long press failed: ${result.errorMessage}")
        }
    }

    private suspend fun executeSwipe(action: Action.Swipe): ActionResult {
        Timber.d(
            "Shizuku swipe: (%d,%d)->(%d,%d) duration=%d",
            action.fromX,
            action.fromY,
            action.toX,
            action.toY,
            action.durationMs
        )
        val result = executeShellCommand(
            "input swipe ${action.fromX} ${action.fromY} ${action.toX} ${action.toY} ${action.durationMs}"
        )
        return if (result.isSuccess) {
            delay(DELAY_AFTER_SWIPE)
            ActionResult.success("Swipe executed")
        } else {
            ActionResult.failure("Swipe failed: ${result.errorMessage}")
        }
    }

    private suspend fun executeInput(text: String): ActionResult {
        Timber.d("Shizuku input text: %s", text)
        if (text.isBlank()) {
            return ActionResult.failure("Input text is empty.")
        }

        val escaped = text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace(" ", "%s")
            .replace("&", "%26")
            .replace("<", "%3c")
            .replace(">", "%3e")
            .replace("|", "%7c")
            .replace(";", "%3b")

        val result = executeShellCommand("""input text "$escaped"""")
        return if (result.isSuccess) {
            delay(DELAY_AFTER_INPUT)
            ActionResult.success("Text input applied")
        } else {
            ActionResult.failure("Text input failed: ${result.errorMessage}")
        }
    }

    private suspend fun executePressKey(keyCode: Int, keyName: String): ActionResult {
        Timber.d("Shizuku key event: %d (%s)", keyCode, keyName)
        val result = executeShellCommand("input keyevent $keyCode")
        return if (result.isSuccess) {
            delay(DELAY_AFTER_KEY)
            val label = keyName.ifBlank { "keyCode=$keyCode" }
            ActionResult.success("Key event executed: $label")
        } else {
            ActionResult.failure("Key event failed: ${result.errorMessage}")
        }
    }

    private suspend fun executeOpenApp(packageName: String, appName: String): ActionResult {
        Timber.d("Shizuku launch app: %s", packageName)
        val result = executeShellCommand("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
        return if (result.isSuccess) {
            delay(DELAY_AFTER_LAUNCH_APP)
            ActionResult.success("Launch signal sent to ${appName.ifBlank { packageName }}")
        } else {
            ActionResult.failure("Launch failed: ${result.errorMessage}")
        }
    }

    private suspend fun executeWait(durationMs: Long): ActionResult {
        if (durationMs > 0) {
            delay(durationMs)
        }
        return ActionResult.success("Waited ${durationMs}ms")
    }

    private fun executeShellCommand(command: String): ShellResult =
        ShizukuShell.executeCommand("sh", "-c", command)

    private fun parsePackageName(output: String): String {
        val normalized = output.replace('\r', '\n')
        val patterns = listOf(
            """mCurrentFocus=Window\{[^}]+\s([a-zA-Z0-9._]+)/""".toRegex(),
            """mFocusedApp=AppWindowToken\{[^}]+\s([a-zA-Z0-9._]+)/""".toRegex(),
            """mFocusedApp=ActivityRecord\{[^}]+\s([a-zA-Z0-9._]+)/""".toRegex()
        )

        for (pattern in patterns) {
            pattern.find(normalized)?.groupValues?.getOrNull(1)?.let { return it }
        }

        Timber.w("Unable to parse foreground package. Dump head: %s", normalized.take(512))
        return ""
    }

    companion object {
        private const val DELAY_AFTER_CLICK = 300L
        private const val DELAY_AFTER_SWIPE = 500L
        private const val DELAY_AFTER_INPUT = 320L
        private const val DELAY_AFTER_KEY = 200L
        private const val DELAY_AFTER_LAUNCH_APP = 2_000L
    }
}

