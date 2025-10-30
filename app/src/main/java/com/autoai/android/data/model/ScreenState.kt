package com.autoai.android.data.model

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * 屏幕状态快照，聚合一次感知流程得到的所有上下文信息。
 */
data class ScreenState(
    /** 当前屏幕的原始位图，用于尺寸计算与本地分析。 */
    val screenshot: Bitmap,
    /** 经过压缩编码后的截图 Base64，用于发送给多模态大模型。 */
    val screenshotBase64: String,
    /** 当前前台应用包名。 */
    val currentApp: String,
    /** 当前前台应用名称，如果无法解析则为空。 */
    val currentAppName: String = "",
    /** 由 UIAutomator 导出的控件树。 */
    val viewHierarchy: ViewNode,
    /** 经过筛选的可交互 UI 元素列表。 */
    val uiElements: List<UiElement>,
    /** 屏幕上识别到的所有文本内容。 */
    val screenText: List<String>,
    /** 针对当前界面的结构化描述，供提示词直接使用。 */
    val description: String = "",
    /** 生成该快照的时间戳。 */
    val timestamp: Long = System.currentTimeMillis(),
    /** 当前屏幕宽度（像素）。 */
    val screenWidth: Int = screenshot.width,
    /** 当前屏幕高度（像素）。 */
    val screenHeight: Int = screenshot.height
) {
    /**
     * UI 元素抽象，便于在决策阶段快速定位控件。
     */
    data class UiElement(
        val id: Int,
        val type: String,
        val text: String = "",
        val contentDescription: String = "",
        val resourceId: String = "",
        val bounds: Rect,
        val clickable: Boolean,
        val enabled: Boolean,
        val scrollable: Boolean = false,
        val editable: Boolean = false,
        val identifier: String = ""
    ) {
        fun center(): Pair<Int, Int> = bounds.centerX() to bounds.centerY()

        fun hasLabel(): Boolean = text.isNotBlank() || contentDescription.isNotBlank()
    }

    /**
     * View Hierarchy 树节点。
     */
    data class ViewNode(
        val className: String,
        val text: String = "",
        val contentDescription: String = "",
        val resourceId: String = "",
        val bounds: Rect,
        val clickable: Boolean,
        val focusable: Boolean,
        val enabled: Boolean,
        val checkable: Boolean,
        val checked: Boolean,
        val scrollable: Boolean = false,
        val editable: Boolean = false,
        val children: MutableList<ViewNode> = mutableListOf()
    ) {
        /**
         * 递归查找所有可点击节点。
         */
        fun findClickableNodes(): List<ViewNode> {
            val result = mutableListOf<ViewNode>()
            if (clickable && enabled) {
                result.add(this)
            }
            children.forEach { child -> result.addAll(child.findClickableNodes()) }
            return result
        }

        /**
         * 根据文本模糊搜索节点。
         */
        fun findNodesByText(searchText: String, ignoreCase: Boolean = true): List<ViewNode> {
            if (searchText.isBlank()) return emptyList()

            val matcher = if (ignoreCase) searchText.lowercase() else searchText
            val nodeText = if (ignoreCase) text.lowercase() else text
            val nodeDesc = if (ignoreCase) contentDescription.lowercase() else contentDescription
            val matched = nodeText.contains(matcher) || nodeDesc.contains(matcher)

            val result = mutableListOf<ViewNode>()
            if (matched) {
                result.add(this)
            }
            children.forEach { child -> result.addAll(child.findNodesByText(searchText, ignoreCase)) }
            return result
        }

        /**
         * 转换为对外暴露的 UiElement。
         */
        fun toUiElement(id: Int, identifier: String = ""): UiElement = UiElement(
            id = id,
            type = className.substringAfterLast('.'),
            text = text,
            contentDescription = contentDescription,
            resourceId = resourceId,
            bounds = bounds,
            clickable = clickable,
            enabled = enabled,
            scrollable = scrollable,
            editable = editable,
            identifier = identifier.ifBlank { resourceId.ifBlank { "$className#$id" } }
        )
    }
}
