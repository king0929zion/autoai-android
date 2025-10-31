package com.autoai.android.perception

import android.graphics.Rect
import com.autoai.android.accessibility.AccessibilityBridge
import com.autoai.android.data.model.ScreenState
import com.autoai.android.permission.ControlMode
import com.autoai.android.permission.ControlPreferencesRepository
import com.autoai.android.permission.ShizukuManager
import com.autoai.android.utils.ShizukuShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import timber.log.Timber
import java.io.File
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 控件树分析器，支持 Shizuku + uiautomator 与无障碍直接遍历两种模式。
 */
@Singleton
class ViewHierarchyAnalyzer @Inject constructor(
    private val controlPreferencesRepository: ControlPreferencesRepository,
    private val shizukuManager: ShizukuManager,
    private val accessibilityBridge: AccessibilityBridge
) {

    suspend fun getViewHierarchy(): Result<ScreenState.ViewNode> {
        return when (controlPreferencesRepository.getCurrentMode()) {
            ControlMode.SHIZUKU -> getViewHierarchyViaShizuku()
            ControlMode.ACCESSIBILITY -> accessibilityBridge.buildViewHierarchy()
        }
    }

    suspend fun extractAllText(root: ScreenState.ViewNode): List<String> =
        withContext(Dispatchers.Default) {
            accessibilityBridge.extractAllTexts(root)
        }

    private suspend fun getViewHierarchyViaShizuku(): Result<ScreenState.ViewNode> = withContext(Dispatchers.IO) {
        if (!shizukuManager.isShizukuAvailable()) {
            return@withContext Result.failure(IllegalStateException("Shizuku 未就绪，无法获取控件树"))
        }

        runCatching {
            ensureDumpDirectory()
            dumpWindowHierarchy()
            val xmlContent = readDumpFile()
            parseXml(xmlContent)
        }.onFailure { Timber.e(it, "解析控件树失败") }
    }

    private fun ensureDumpDirectory() {
        val dir = File(DUMP_DIR)
        if (dir.exists()) return

        val result = ShizukuShell.executeCommand("mkdir", "-p", DUMP_DIR)
        if (!result.isSuccess) {
            val reason = result.errorMessage.ifBlank { result.output.ifBlank { "未知错误" } }
            throw IllegalStateException("无法创建控件树导出目录：$reason")
        }
    }

    private fun dumpWindowHierarchy() {
        Timber.d("执行 uiautomator dump")
        ShizukuShell.executeCommand("rm", "-f", DUMP_FILE)
        val result = ShizukuShell.executeCommandWithTimeout(15, "uiautomator", "dump", DUMP_FILE)
        if (!result.isSuccess) {
            throw IllegalStateException("uiautomator dump 执行失败：${result.errorMessage}")
        }
    }

    private fun readDumpFile(): String {
        val readResult = ShizukuShell.executeCommandWithTimeout(10, "cat", DUMP_FILE)
        if (!readResult.isSuccess || readResult.output.isBlank()) {
            val message = readResult.errorMessage.ifBlank { "控件树文件内容为空" }
            throw IllegalStateException("读取控件树文件失败：$message")
        }

        runCatching { ShizukuShell.executeCommand("rm", "-f", DUMP_FILE) }
            .onFailure { Timber.w(it, "删除控件树临时文件失败: $DUMP_FILE") }

        return readResult.output
    }

    private fun parseXml(xml: String): ScreenState.ViewNode {
        val parser = XmlPullParserFactory.newInstance()
            .newPullParser()
            .apply { setInput(StringReader(xml)) }

        val stack = ArrayDeque<ScreenState.ViewNode>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "node") {
                    val node = parseNode(parser)
                    stack.lastOrNull()?.children?.add(node)
                    stack.addLast(node)
                }
                XmlPullParser.END_TAG -> if (parser.name == "node") {
                    val finished = stack.removeLast()
                    if (stack.isEmpty()) return finished
                }
            }
            eventType = parser.next()
        }
        throw IllegalStateException("未能解析出根节点")
    }

    private fun parseNode(parser: XmlPullParser): ScreenState.ViewNode {
        val className = parser.getAttributeValue(null, "class") ?: ""
        val text = parser.getAttributeValue(null, "text") ?: ""
        val contentDesc = parser.getAttributeValue(null, "content-desc") ?: ""
        val resourceId = parser.getAttributeValue(null, "resource-id") ?: ""
        val bounds = parser.getAttributeValue(null, "bounds") ?: "[0,0][0,0]"
        val clickable = parser.getAttributeValue(null, "clickable") == "true"
        val focusable = parser.getAttributeValue(null, "focusable") == "true"
        val enabled = parser.getAttributeValue(null, "enabled") == "true"
        val checkable = parser.getAttributeValue(null, "checkable") == "true"
        val checked = parser.getAttributeValue(null, "checked") == "true"
        val scrollable = parser.getAttributeValue(null, "scrollable") == "true"
        val editable = parser.getAttributeValue(null, "editable") == "true" ||
            className.contains("EditText", ignoreCase = true)

        return ScreenState.ViewNode(
            className = className,
            text = text,
            contentDescription = contentDesc,
            resourceId = resourceId,
            bounds = parseBounds(bounds),
            clickable = clickable,
            focusable = focusable,
            enabled = enabled,
            checkable = checkable,
            checked = checked,
            scrollable = scrollable,
            editable = editable
        )
    }

    private fun parseBounds(bounds: String): Rect {
        return try {
            val regex = """\[(\d+),(\d+)\]\[(\d+),(\d+)\]""".toRegex()
            val (left, top, right, bottom) = regex.find(bounds)?.destructured ?: return Rect()
            Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        } catch (e: Exception) {
            Timber.w(e, "解析 bounds 失败: %s", bounds)
            Rect()
        }
    }

    companion object {
        private const val DUMP_DIR = "/data/local/tmp/autoai"
        private const val DUMP_FILE = "$DUMP_DIR/window_dump.xml"
    }
}
