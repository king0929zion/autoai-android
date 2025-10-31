package com.autoai.android.perception

import com.autoai.android.data.model.ScreenState
import com.autoai.android.data.model.ScreenState.UiElement
import com.autoai.android.data.model.ScreenState.ViewNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 多模态信息融合器，将截图、控件树等多源数据融合为统一的屏幕状态结构。
 */
@Singleton
class MultiModalFusion @Inject constructor(
    private val screenCapture: ScreenCapture,
    private val viewHierarchyAnalyzer: ViewHierarchyAnalyzer
) {

    companion object {
        private const val MAX_TEXT_LINES = 20
        private const val MAX_CLICKABLE_ELEMENTS = 15
    }

    /**
     * 生成完整的屏幕状态快照，用于提示词与策略引擎。
     */
    suspend fun generateScreenState(currentApp: String): Result<ScreenState> {
        return try {
            Timber.d("开始生成屏幕状态，当前前台应用：%s", currentApp)

            val screenshot = screenCapture.captureScreen().getOrElse { return Result.failure(it) }
            val viewHierarchy = viewHierarchyAnalyzer.getViewHierarchy().getOrElse { return Result.failure(it) }
            val uiElements = extractUiElements(viewHierarchy)
            val screenText = viewHierarchyAnalyzer.extractAllText(viewHierarchy)
            val description = generateDescription(uiElements, screenText)

            Result.success(
                ScreenState(
                    screenshot = screenshot,
                    screenshotBase64 = screenCapture.bitmapToBase64(screenshot),
                    currentApp = currentApp,
                    viewHierarchy = viewHierarchy,
                    uiElements = uiElements,
                    screenText = screenText,
                    description = description
                )
            )
        } catch (error: Exception) {
            Timber.e(error, "生成屏幕状态失败")
            Result.failure(error)
        }
    }

    private suspend fun extractUiElements(root: ViewNode): List<UiElement> =
        withContext(Dispatchers.Default) {
            val elements = mutableListOf<UiElement>()
            var nextId = 0

            fun traverse(node: ViewNode) {
                if (shouldInclude(node)) {
                    elements.add(
                        UiElement(
                            id = nextId++,
                            type = classify(node),
                            text = node.text,
                            contentDescription = node.contentDescription,
                            resourceId = node.resourceId,
                            bounds = node.bounds,
                            clickable = node.clickable,
                            enabled = node.enabled,
                            scrollable = node.scrollable,
                            editable = node.editable
                        )
                    )
                }
                node.children.forEach { traverse(it) }
            }

            traverse(root)
            elements
        }

    private fun shouldInclude(node: ViewNode): Boolean {
        if (node.clickable && node.enabled) return true
        if (node.text.isNotBlank()) return true
        if (node.contentDescription.isNotBlank()) return true
        if (node.editable) return true
        return false
    }

    private fun classify(node: ViewNode): String = when {
        node.className.contains("Button", ignoreCase = true) -> "button"
        node.className.contains("EditText", ignoreCase = true) -> "input"
        node.className.contains("TextView", ignoreCase = true) -> "text"
        node.className.contains("ImageView", ignoreCase = true) -> "image"
        node.className.contains("CheckBox", ignoreCase = true) -> "checkbox"
        node.className.contains("Switch", ignoreCase = true) -> "switch"
        node.className.contains("ListView", ignoreCase = true) -> "list"
        node.className.contains("RecyclerView", ignoreCase = true) -> "list"
        node.scrollable -> "scrollable"
        node.clickable -> "clickable"
        else -> "other"
    }

    private fun generateDescription(
        uiElements: List<UiElement>,
        screenText: List<String>
    ): String = buildString {
        appendLine("=== 屏幕概览 ===")
        appendLine("可交互元素：${uiElements.size} 个")
        appendLine("文本片段：${screenText.size} 条")
        appendLine()

        val nonEmptyTexts = screenText.filter { it.isNotBlank() }
        if (nonEmptyTexts.isNotEmpty()) {
            appendLine("=== 屏幕文本 ===")
            nonEmptyTexts.take(MAX_TEXT_LINES).forEach { appendLine("• $it") }
            if (nonEmptyTexts.size > MAX_TEXT_LINES) {
                appendLine("• …… 其余 ${nonEmptyTexts.size - MAX_TEXT_LINES} 条文本已省略")
            }
            appendLine()
        }

        val clickableElements = uiElements.filter { it.clickable && it.enabled }
        if (clickableElements.isNotEmpty()) {
            appendLine("=== 可点击元素 ===")
            clickableElements
                .take(MAX_CLICKABLE_ELEMENTS)
                .forEach { element ->
                    val label = when {
                        element.text.isNotBlank() -> element.text
                        element.contentDescription.isNotBlank() -> element.contentDescription
                        else -> element.type
                    }
                    appendLine("• [${element.type}] $label @ (${element.bounds.centerX()}, ${element.bounds.centerY()})")
                }
            if (clickableElements.size > MAX_CLICKABLE_ELEMENTS) {
                appendLine("• …… 其余 ${clickableElements.size - MAX_CLICKABLE_ELEMENTS} 个元素已省略")
            }
        }
    }

    fun findElements(screenState: ScreenState, query: String): List<UiElement> {
        if (query.isBlank()) return emptyList()
        return screenState.uiElements.filter { element ->
            element.text.contains(query, ignoreCase = true) ||
                element.contentDescription.contains(query, ignoreCase = true) ||
                element.resourceId.contains(query, ignoreCase = true)
        }
    }
}
