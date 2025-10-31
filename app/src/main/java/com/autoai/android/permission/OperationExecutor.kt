package com.autoai.android.permission

import android.view.KeyEvent
import com.autoai.android.data.model.Action
import com.autoai.android.data.model.ActionResult
import com.autoai.android.utils.ShizukuShell
import com.autoai.android.utils.ShellResult
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通过 Shizuku 在系统层执行真实用户行为。
 */
@Singleton
class OperationExecutor @Inject constructor(
    private val shizukuManager: ShizukuManager
) {

    suspend fun executeAction(action: Action): ActionResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        if (!shizukuManager.isShizukuAvailable()) {
            return@withContext ActionResult.failure("Shizuku 未就绪，无法执行操作")
        }

        val result = runCatching {
            when (action) {
                is Action.Click -> executeClick(action.x, action.y)
                is Action.LongClick -> executeLongClick(action.x, action.y, action.durationMs)
                is Action.Swipe -> executeSwipe(action)
                is Action.Input -> executeInput(action.text)
                is Action.PressKey -> executePressKey(action.keyCode)
                is Action.OpenApp -> executeOpenApp(action.packageName)
                is Action.Wait -> executeWait(action.durationMs)
                is Action.GoBack -> executePressKey(KeyEvent.KEYCODE_BACK)
                is Action.Complete -> ActionResult.success(action.message)
                is Action.Error -> ActionResult.failure(action.message)
                is Action.RequestUserHelp -> ActionResult.failure(
                    message = "需要用户介入: ${action.reason}",
                    needsUserConfirmation = true
                )
            }
        }.getOrElse { error ->
            Timber.e(error, "执行动作失败: $action")
            ActionResult.failure("执行失败: ${error.message}")
        }

        result.copy(executionTimeMs = System.currentTimeMillis() - start)
    }

    suspend fun getCurrentApp(): Result<String> = withContext(Dispatchers.IO) {
        if (!shizukuManager.isShizukuAvailable()) {
            return@withContext Result.failure(IllegalStateException("Shizuku 未就绪"))
        }

        return@withContext runCatching {
            val command = "dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp'"
            val shell = executeShellCommand(command)
            if (!shell.isSuccess) error(shell.errorMessage.ifBlank { "无法获取前台应用" })

            parsePackageName(shell.output).also {
                require(it.isNotBlank()) { "未能解析前台应用包名" }
            }
        }.onFailure { Timber.e(it, "获取前台应用失败") }
    }

    private suspend fun executeClick(x: Int, y: Int): ActionResult {
        Timber.d("点击坐标: (%d, %d)", x, y)
        val result = executeShellCommand("input tap $x $y")
        return if (result.isSuccess) {
            delay(DELAY_AFTER_CLICK)
            ActionResult.success("已点击 ($x, $y)")
        } else {
            ActionResult.failure("点击失败: ${result.errorMessage}")
        }
    }

    private suspend fun executeLongClick(x: Int, y: Int, durationMs: Long): ActionResult {
        Timber.d("长按坐标: (%d, %d) 持续 %dms", x, y, durationMs)
        val result = executeShellCommand("input swipe $x $y $x $y $durationMs")
        return if (result.isSuccess) {
            delay(DELAY_AFTER_CLICK)
            ActionResult.success("已长按 ($x, $y)")
        } else {
            ActionResult.failure("长按失败: ${result.errorMessage}")
        }
    }

    private suspend fun executeSwipe(action: Action.Swipe): ActionResult {
        Timber.d(
            "滑动: (%d,%d) -> (%d,%d) %dms",
            action.fromX, action.fromY, action.toX, action.toY, action.durationMs
        )
        val result = executeShellCommand(
            "input swipe ${action.fromX} ${action.fromY} ${action.toX} ${action.toY} ${action.durationMs}"
        )
        return if (result.isSuccess) {
            delay(DELAY_AFTER_SWIPE)
            ActionResult.success("滑动完成")
        } else {
            ActionResult.failure("滑动失败: ${result.errorMessage}")
        }
    }

    private suspend fun executeInput(text: String): ActionResult {
        Timber.d("输入文本: %s", text)
        // 改进的文本转义处理，支持更多特殊字符
        val escaped = text
            .replace(" ", "%s")
            .replace("&", "%26") 
            .replace("<", "%3c")
            .replace(">", "%3e")
            .replace("|", "%7c")
            .replace(";", "%3b")
        val result = executeShellCommand("input text \"$escaped\"")
        return if (result.isSuccess) {
            delay(DELAY_AFTER_INPUT)
            ActionResult.success("输入文本完成")
        } else {
            ActionResult.failure("输入失败: ${result.errorMessage}")
        }
    }

    private suspend fun executePressKey(keyCode: Int): ActionResult {
        Timber.d("模拟按键: %d", keyCode)
        val result = executeShellCommand("input keyevent $keyCode")
        return if (result.isSuccess) {
            delay(DELAY_AFTER_KEY)
            ActionResult.success("按键完成")
        } else {
            ActionResult.failure("按键失败: ${result.errorMessage}")
        }
    }

    private suspend fun executeOpenApp(packageName: String): ActionResult {
        Timber.d("启动应用: %s", packageName)
        val result = executeShellCommand(
            "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
        )
        return if (result.isSuccess) {
            delay(DELAY_AFTER_LAUNCH_APP)
            ActionResult.success("已打开应用: $packageName")
        } else {
            ActionResult.failure("打开应用失败: ${result.errorMessage}")
        }
    }

    private suspend fun executeWait(durationMs: Long): ActionResult {
        Timber.d("等待 %dms", durationMs)
        delay(durationMs)
        return ActionResult.success("等待完成")
    }

    private fun executeShellCommand(command: String): ShellResult {
        return ShizukuShell.executeCommand("sh", "-c", command)
    }

    private fun parsePackageName(output: String): String {
        val regex = """([a-zA-Z0-9._]+)/(?:[a-zA-Z0-9._]+)""".toRegex()
        return regex.find(output)?.groupValues?.getOrNull(1).orEmpty()
    }

    companion object {
        private const val DELAY_AFTER_CLICK = 300L
        private const val DELAY_AFTER_SWIPE = 500L
        private const val DELAY_AFTER_INPUT = 300L
        private const val DELAY_AFTER_KEY = 200L
        private const val DELAY_AFTER_LAUNCH_APP = 2_000L
    }
}
