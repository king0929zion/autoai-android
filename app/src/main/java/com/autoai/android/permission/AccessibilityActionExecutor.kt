package com.autoai.android.permission

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import com.autoai.android.accessibility.AccessibilityBridge
import com.autoai.android.data.model.Action
import com.autoai.android.data.model.ActionResult
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes system actions via Android accessibility service gestures and APIs.
 */
@Singleton
class AccessibilityActionExecutor @Inject constructor(
    private val bridge: AccessibilityBridge
) : ControlActionExecutor {

    override val label: String = "Accessibility mode"

    override suspend fun isReady(): Boolean = bridge.isReady()

    override suspend fun executeAction(action: Action): ActionResult {
        if (!bridge.isReady()) {
            return ActionResult.failure("Accessibility service is not enabled. Please enable the AutoAI accessibility service in system settings.")
        }

        return when (action) {
            is Action.Click -> performClick(action)
            is Action.LongClick -> performLongClick(action)
            is Action.Swipe -> performSwipe(action)
            is Action.Input -> performInput(action)
            is Action.PressKey -> performPressKey(action.keyCode, action.keyName)
            is Action.OpenApp -> performOpenApp(action.packageName, action.appName)
            is Action.Wait -> performWait(action.durationMs)
            Action.GoBack -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK, "Back")
            is Action.Complete -> ActionResult.success(action.message.ifBlank { "Task completed" })
            is Action.Error -> ActionResult.failure(action.message, needsUserConfirmation = !action.recoverable)
            is Action.RequestUserHelp -> ActionResult.failure("User assistance required: ${action.reason}", needsUserConfirmation = true)
        }
    }

    override suspend fun getCurrentApp(): Result<String> {
        if (!bridge.isReady()) {
            return Result.failure(IllegalStateException("Accessibility service is not enabled"))
        }
        val packageName = bridge.currentPackageName()
        return if (!packageName.isNullOrBlank()) {
            Result.success(packageName)
        } else {
            Result.failure(IllegalStateException("Unable to determine the foreground application. Try opening the target screen again."))
        }
    }

    private suspend fun performClick(action: Action.Click): ActionResult {
        val success = bridge.performClick(action.x, action.y)
        return if (success) {
            delay(DEFAULT_ACTION_DELAY)
            ActionResult.success("Tapped (${action.x}, ${action.y})")
        } else {
            ActionResult.failure("Accessibility tap failed. Ensure the target element is visible.")
        }
    }

    private suspend fun performLongClick(action: Action.LongClick): ActionResult {
        val success = bridge.performLongClick(action.x, action.y, action.durationMs)
        return if (success) {
            delay(DEFAULT_ACTION_DELAY)
            ActionResult.success("Long press executed at (${action.x}, ${action.y})")
        } else {
            ActionResult.failure("Accessibility long press failed.")
        }
    }

    private suspend fun performSwipe(action: Action.Swipe): ActionResult {
        val success = bridge.performSwipe(action.fromX, action.fromY, action.toX, action.toY, action.durationMs)
        return if (success) {
            delay(DEFAULT_ACTION_DELAY)
            ActionResult.success("Swipe completed")
        } else {
            ActionResult.failure("Accessibility swipe failed.")
        }
    }

    private suspend fun performInput(action: Action.Input): ActionResult {
        val success = bridge.setText(action.text)
        return if (success) {
            delay(DEFAULT_ACTION_DELAY)
            ActionResult.success("Text input applied")
        } else {
            ActionResult.failure("Text input failed. Focus the text field and try again.")
        }
    }

    private suspend fun performPressKey(keyCode: Int, keyName: String): ActionResult {
        val mapped = when (keyCode) {
            KeyEvent.KEYCODE_BACK -> AccessibilityService.GLOBAL_ACTION_BACK
            KeyEvent.KEYCODE_HOME -> AccessibilityService.GLOBAL_ACTION_HOME
            KeyEvent.KEYCODE_APP_SWITCH -> AccessibilityService.GLOBAL_ACTION_RECENTS
            else -> null
        }

        return if (mapped != null && bridge.performGlobalAction(mapped)) {
            delay(DEFAULT_ACTION_DELAY)
            val label = keyName.ifBlank { "keyCode=$keyCode" }
            ActionResult.success("Executed key action: $label")
        } else {
            val label = keyName.ifBlank { "keyCode=$keyCode" }
            ActionResult.failure("Unable to simulate key action: $label")
        }
    }

    private suspend fun performOpenApp(packageName: String, appName: String): ActionResult {
        val opened = bridge.launchApp(packageName)
        return if (opened) {
            delay(APP_LAUNCH_DELAY)
            ActionResult.success("Attempted to launch ${appName.ifBlank { packageName }}")
        } else {
            ActionResult.failure("Failed to launch $packageName. Ensure the app is installed.")
        }
    }

    private suspend fun performWait(durationMs: Long): ActionResult {
        delay(durationMs)
        return ActionResult.success("Waited ${durationMs}ms")
    }

    private suspend fun performGlobalAction(action: Int, description: String): ActionResult {
        val executed = bridge.performGlobalAction(action)
        return if (executed) {
            delay(DEFAULT_ACTION_DELAY)
            ActionResult.success("Executed $description action")
        } else {
            ActionResult.failure("Failed to execute $description action. Please try manually.")
        }
    }

    companion object {
        private const val DEFAULT_ACTION_DELAY = 360L
        private const val APP_LAUNCH_DELAY = 2_000L
    }
}
