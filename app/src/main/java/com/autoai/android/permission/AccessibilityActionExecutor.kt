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
 * 通过无障碍服务执行系统控制操作。
 */
@Singleton
class AccessibilityActionExecutor @Inject constructor(
    private val bridge: AccessibilityBridge
) : ControlActionExecutor {

    override val label: String = "无障碍模式"

    override suspend fun isReady(): Boolean = bridge.isReady()

    override suspend fun executeAction(action: Action): ActionResult {
        if (!bridge.isReady()) {
            return ActionResult.failure("无障碍服务未就绪，请先在系统设置中启用 AutoAI 无障碍服务")
        }

        return when (action) {
            is Action.Click -> performClick(action)
            is Action.LongClick -> performLongClick(action)
            is Action.Swipe -> performSwipe(action)
            is Action.Input -> performInput(action)
            is Action.PressKey -> performPressKey(action.keyCode, action.keyName)
            is Action.OpenApp -> performOpenApp(action.packageName, action.appName)
            is Action.Wait -> performWait(action.durationMs)
            Action.GoBack -> performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK, "返回")
            is Action.Complete -> ActionResult.success(action.message.ifBlank { "任务已完成" })
            is Action.Error -> ActionResult.failure(action.message, needsUserConfirmation = !action.recoverable)
            is Action.RequestUserHelp -> ActionResult.failure("需要用户协助：${action.reason}", needsUserConfirmation = true)
        }
    }

    override suspend fun getCurrentApp(): Result<String> {
        if (!bridge.isReady()) {
            return Result.failure(IllegalStateException("无障碍服务未启用"))
        }
        val packageName = bridge.currentPackageName()
        return if (!packageName.isNullOrBlank()) {
            Result.success(packageName)
        } else {
            Result.failure(IllegalStateException("未能识别前台应用，请尝试重新打开界面"))
        }
    }

    private suspend fun performClick(action: Action.Click): ActionResult {
        val success = bridge.performClick(action.x, action.y)
        return if (success) {
            delay(DEFAULT_ACTION_DELAY)
            ActionResult.success("已点击 (${action.x}, ${action.y})")
        } else {
            ActionResult.failure("无障碍点击失败，请确认目标控件可见")
        }
    }

    private suspend fun performLongClick(action: Action.LongClick): ActionResult {
        val success = bridge.performLongClick(action.x, action.y, action.durationMs)
        return if (success) {
            delay(DEFAULT_ACTION_DELAY)
            ActionResult.success("已长按 (${action.x}, ${action.y})")
        } else {
            ActionResult.failure("无障碍长按失败")
        }
    }

    private suspend fun performSwipe(action: Action.Swipe): ActionResult {
        val success = bridge.performSwipe(action.fromX, action.fromY, action.toX, action.toY, action.durationMs)
        return if (success) {
            delay(DEFAULT_ACTION_DELAY)
            ActionResult.success("滑动已完成")
        } else {
            ActionResult.failure("无障碍滑动失败")
        }
    }

    private suspend fun performInput(action: Action.Input): ActionResult {
        val success = bridge.setText(action.text)
        return if (success) {
            delay(DEFAULT_ACTION_DELAY)
            ActionResult.success("输入文本成功")
        } else {
            ActionResult.failure("文本输入失败，请先点击输入框后重试")
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
            ActionResult.success("已执行按键${if (keyName.isNotBlank()) "：$keyName" else ""}")
        } else {
            ActionResult.failure("当前模式无法模拟该按键：${keyName.ifBlank { "keyCode=$keyCode" }}")
        }
    }

    private suspend fun performOpenApp(packageName: String, appName: String): ActionResult {
        val opened = bridge.launchApp(packageName)
        return if (opened) {
            delay(APP_LAUNCH_DELAY)
            ActionResult.success("已尝试打开${appName.ifBlank { packageName }}")
        } else {
            ActionResult.failure("无法打开目标应用，请确认已安装：$packageName")
        }
    }

    private suspend fun performWait(durationMs: Long): ActionResult {
        delay(durationMs)
        return ActionResult.success("已等待 ${durationMs}ms")
    }

    private suspend fun performGlobalAction(action: Int, description: String): ActionResult {
        val executed = bridge.performGlobalAction(action)
        return if (executed) {
            delay(DEFAULT_ACTION_DELAY)
            ActionResult.success("已执行$description操作")
        } else {
            ActionResult.failure("执行$description操作失败，请手动尝试")
        }
    }

    companion object {
        private const val DEFAULT_ACTION_DELAY = 360L
        private const val APP_LAUNCH_DELAY = 2_000L
    }
}
