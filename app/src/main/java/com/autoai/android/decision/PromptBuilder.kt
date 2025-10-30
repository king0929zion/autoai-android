package com.autoai.android.decision

import com.autoai.android.data.model.Action
import com.autoai.android.data.model.ScreenState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 构建发往多模态大模型的系统/用户提示词。
 */
@Singleton
class PromptBuilder @Inject constructor() {

    companion object {
        private const val MAX_ELEMENTS = 20
        private const val MAX_TEXT_LINES = 15
        private const val MAX_HISTORY = 3
    }

    fun buildSystemPrompt(): String = """
你是一名专业的 Android 自动化助手，负责读取屏幕截图与控件树信息，规划出最安全、最高效的下一步操作。

## 输出格式
仅输出一个 JSON 对象，不要附带任何解释。示例：
```json
{"action":"click","x":540,"y":1280}
```

## 可用操作
1. 点击控件：`{"action":"click","x":540,"y":1280}`
2. 长按控件：`{"action":"long_click","x":540,"y":1280,"duration":800}`
3. 滑动操作：`{"action":"swipe","from_x":500,"from_y":1500,"to_x":500,"to_y":600,"duration":300}`
4. 输入文本：`{"action":"input","text":"要输入的内容"}` —— 输入前请先点击输入框
5. 模拟按键：`{"action":"press_key","key_code":4}`（常用：返回=4，Home=3，最近任务=187）
6. 启动应用：`{"action":"open_app","package":"com.tencent.mm"}`
7. 等待：`{"action":"wait","duration":1500}`（等待界面加载）
8. 返回：`{"action":"go_back"}`
9. 任务完成：`{"action":"complete","message":"说明文字"}`
10. 无法处理：`{"action":"error","message":"原因说明"}`

## 决策要求
- 仔细阅读提供的控件、文本、历史操作。
- 一次只执行一个动作，必要时先点击再输入。
- 坐标必须在屏幕范围内，优先选择带标签的控件中心坐标。
- 遇到权限弹窗/安全提示，请优先处理。
- 若任务已完成或需要人工介入，请使用 `complete` 或 `error` 行为告知。
""".trimIndent()

    fun buildUserPrompt(
        task: String,
        screenState: ScreenState,
        history: List<Action> = emptyList()
    ): String = buildString {
        appendLine("## 当前任务")
        appendLine(task)
        appendLine()

        appendLine("## 屏幕信息")
        appendLine("前台应用: ${screenState.currentApp}")
        appendLine("屏幕尺寸: ${screenState.screenWidth} x ${screenState.screenHeight}")
        appendLine("截图已附带，以 data URI 形式提供给模型。")
        appendLine()

        if (screenState.description.isNotBlank()) {
            appendLine("## 屏幕概览")
            appendLine(screenState.description.trim())
            appendLine()
        }

        val elements = screenState.uiElements.take(MAX_ELEMENTS)
        if (elements.isNotEmpty()) {
            appendLine("## 重点元素（最多 $MAX_ELEMENTS 个）")
            elements.forEach { element ->
                val label = when {
                    element.text.isNotBlank() -> element.text
                    element.contentDescription.isNotBlank() -> element.contentDescription
                    else -> element.type
                }
                val clickable = if (element.clickable && element.enabled) "[可点击]" else ""
                appendLine(
                    "- $clickable[${element.type}] $label @ (${element.bounds.centerX()}, ${element.bounds.centerY()})"
                )
            }
            if (screenState.uiElements.size > MAX_ELEMENTS) {
                appendLine("… 其余 ${screenState.uiElements.size - MAX_ELEMENTS} 个元素已省略")
            }
            appendLine()
        }

        val texts = screenState.screenText.filter { it.isNotBlank() }
        if (texts.isNotEmpty()) {
            appendLine("## 屏幕文本（最多 $MAX_TEXT_LINES 条）")
            texts.take(MAX_TEXT_LINES).forEach { appendLine("- $it") }
            if (texts.size > MAX_TEXT_LINES) {
                appendLine("… 其余 ${texts.size - MAX_TEXT_LINES} 条文本已省略")
            }
            appendLine()
        }

        if (history.isNotEmpty()) {
            appendLine("## 最近执行的操作")
            history.takeLast(MAX_HISTORY).forEach { appendLine("- ${formatAction(it)}") }
            appendLine()
        }

        appendLine("## 决策指令")
        appendLine("请结合截图、控件树与历史操作，输出一个 JSON 对象，描述下一步要执行的动作，不要附加其他文本。")
    }

    fun buildSimplePrompt(task: String, currentApp: String): String = """
## 任务
$task

## 当前应用
$currentApp

请基于截图与控件树判断下一步操作，仅输出一个 JSON 行为指令。
""".trimIndent()

    fun buildErrorRecoveryPrompt(
        task: String,
        errorMessage: String,
        screenState: ScreenState
    ): String = """
## 任务
$task

## 遇到的问题
$errorMessage

## 当前屏幕概览
${screenState.description.ifBlank { "未提供文字描述，请结合截图分析。" }}

请给出一条 JSON 操作指令，用于恢复或继续任务。
""".trimIndent()

    private fun formatAction(action: Action): String = when (action) {
        is Action.Click -> "点击 (${action.x}, ${action.y})"
        is Action.LongClick -> "长按 (${action.x}, ${action.y})，${action.durationMs}ms"
        is Action.Swipe -> "滑动 (${action.fromX}, ${action.fromY}) -> (${action.toX}, ${action.toY})，${action.durationMs}ms"
        is Action.Input -> "输入文本: ${action.text.take(40)}"
        is Action.PressKey -> "按键: ${action.keyCode}"
        is Action.OpenApp -> "打开应用: ${action.packageName}"
        is Action.Wait -> "等待 ${action.durationMs}ms"
        is Action.GoBack -> "返回上一层"
        is Action.Complete -> "任务完成: ${action.message}"
        is Action.Error -> "任务错误: ${action.message}"
        is Action.RequestUserHelp -> "请求人工协助: ${action.reason}"
    }
}
