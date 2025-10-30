package com.autoai.android.data.model

import android.graphics.Rect

/**
 * 操作动作基类
 * 表示 AI 决策后需要执行的具体操作
 */
sealed class Action {
    /**
     * 点击操作
     * @param x X 坐标
     * @param y Y 坐标
     * @param elementDescription 被点击元素的描述（用于日志）
     */
    data class Click(
        val x: Int,
        val y: Int,
        val elementDescription: String = ""
    ) : Action()

    /**
     * 长按操作
     * @param x X 坐标
     * @param y Y 坐标
     * @param durationMs 长按时长（毫秒）
     */
    data class LongClick(
        val x: Int,
        val y: Int,
        val durationMs: Long = 1000
    ) : Action()

    /**
     * 滑动操作
     * @param fromX 起始 X 坐标
     * @param fromY 起始 Y 坐标
     * @param toX 结束 X 坐标
     * @param toY 结束 Y 坐标
     * @param durationMs 滑动时长（毫秒）
     */
    data class Swipe(
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int,
        val durationMs: Long = 300
    ) : Action()

    /**
     * 文本输入操作
     * @param text 要输入的文本
     */
    data class Input(
        val text: String
    ) : Action()

    /**
     * 按键操作
     * @param keyCode Android KeyEvent 键码
     */
    data class PressKey(
        val keyCode: Int,
        val keyName: String = ""
    ) : Action()

    /**
     * 打开应用
     * @param packageName 应用包名
     */
    data class OpenApp(
        val packageName: String,
        val appName: String = ""
    ) : Action()

    /**
     * 等待操作
     * @param durationMs 等待时长（毫秒）
     */
    data class Wait(
        val durationMs: Long
    ) : Action()

    /**
     * 返回操作
     */
    object GoBack : Action()

    /**
     * 任务完成
     * @param message 完成描述
     */
    data class Complete(
        val message: String = "任务完成"
    ) : Action()

    /**
     * 错误信息
     * @param message 错误描述
     * @param recoverable 是否可恢复
     */
    data class Error(
        val message: String,
        val recoverable: Boolean = false
    ) : Action()

    /**
     * 需要用户介入
     * @param reason 原因说明
     */
    data class RequestUserHelp(
        val reason: String
    ) : Action()
}

/**
 * 动作执行结果
 */
data class ActionResult(
    val success: Boolean,
    val message: String = "",
    val executionTimeMs: Long = 0,
    val error: Throwable? = null,
    val needsUserConfirmation: Boolean = false
) {
    companion object {
        fun success(
            message: String = "操作成功",
            timeMs: Long = 0,
            needsUserConfirmation: Boolean = false
        ) = ActionResult(true, message, timeMs, null, needsUserConfirmation)
        
        fun failure(
            message: String,
            error: Throwable? = null,
            timeMs: Long = 0,
            needsUserConfirmation: Boolean = false
        ) = ActionResult(false, message, timeMs, error, needsUserConfirmation)
    }
}
