package com.autoai.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.AccessibilityService.ScreenshotResult
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.Path
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.autoai.android.data.model.ScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Bridge layer that exposes high level accessibility interactions such as gestures,
 * text input, screenshot capture and view hierarchy inspection.
 */
@Singleton
class AccessibilityBridge @Inject constructor(
    private val context: Context
) {

    private val serviceRef = AtomicReference<AccessibilityControlService?>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _status = MutableStateFlow(if (isServiceEnabled(context)) AccessibilityStatus.CONNECTING else AccessibilityStatus.SERVICE_DISABLED)
    val status: StateFlow<AccessibilityStatus> = _status

    private val lastEventChannel = Channel<AccessibilityEvent>(capacity = Channel.CONFLATED)

    fun onServiceConnected(service: AccessibilityControlService) {
        serviceRef.set(service)
        _status.value = AccessibilityStatus.READY
        Timber.i("Accessibility service connected")
    }

    fun onServiceDisconnected(service: AccessibilityControlService) {
        serviceRef.compareAndSet(service, null)
        _status.value = if (isServiceEnabled(context)) AccessibilityStatus.CONNECTING else AccessibilityStatus.SERVICE_DISABLED
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) {
            scope.launch { lastEventChannel.send(event) }
        }
    }

    fun refreshStatus() {
        val enabled = isServiceEnabled(context)
        val service = serviceRef.get()
        _status.value = when {
            service != null -> AccessibilityStatus.READY
            enabled -> AccessibilityStatus.CONNECTING
            else -> AccessibilityStatus.SERVICE_DISABLED
        }
    }

    fun isReady(): Boolean = serviceRef.get() != null

    fun isServiceEnabled(context: Context = this.context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        if (!manager.isEnabled) return false
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val expectedServiceId = "${context.packageName}/${AccessibilityControlService::class.java.name}"
        return enabledServices?.split(':')?.any { it.equals(expectedServiceId, ignoreCase = true) } ?: false
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
    }

    suspend fun performClick(x: Int, y: Int, durationMs: Long = 120L): Boolean =
        performGesture(
            GestureDescription.Builder().apply {
                val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
                addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            }.build()
        )

    suspend fun performLongClick(x: Int, y: Int, durationMs: Long): Boolean =
        performGesture(
            GestureDescription.Builder().apply {
                val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
                addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            }.build()
        )

    suspend fun performSwipe(fromX: Int, fromY: Int, toX: Int, toY: Int, durationMs: Long): Boolean =
        performGesture(
            GestureDescription.Builder().apply {
                val path = Path().apply {
                    moveTo(fromX.toFloat(), fromY.toFloat())
                    lineTo(toX.toFloat(), toY.toFloat())
                }
                addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            }.build()
        )

    private suspend fun performGesture(gesture: GestureDescription): Boolean = suspendCoroutine { continuation ->
        val service = serviceRef.get()
        if (service == null) {
            continuation.resume(false)
            return@suspendCoroutine
        }
        val callback = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                continuation.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                continuation.resume(false)
            }
        }
        val dispatched = service.dispatchGesture(gesture, callback, null)
        if (!dispatched) {
            continuation.resume(false)
        }
    }

    suspend fun setText(text: String): Boolean {
        val service = serviceRef.get() ?: return false
        val focused = service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findEditableNode(service.rootInActiveWindow)
            ?: return false

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }

        val success = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!success) {
            val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("autoai", text))
            focused.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
        return success
    }

    private fun findEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val editable = findEditableNode(child)
            if (editable != null) {
                return editable
            }
            child.recycle()
        }
        return null
    }

    fun performGlobalAction(action: Int): Boolean = serviceRef.get()?.performGlobalAction(action) ?: false

    fun launchApp(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(launchIntent); true }.getOrDefault(false)
    }

    fun currentPackageName(): String? = serviceRef.get()?.rootInActiveWindow?.packageName?.toString()

    fun rootViewNode(): AccessibilityNodeInfo? = serviceRef.get()?.rootInActiveWindow

    suspend fun captureScreenshot(): Result<Bitmap> {
        val service = serviceRef.get() ?: return Result.failure(IllegalStateException("Accessibility service not connected"))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return Result.failure(UnsupportedOperationException("Accessibility screenshots require Android 11 or later"))
        }
        return suspendCoroutine { continuation ->
            val displayId = resolveDisplayId(service)
            val executor = ContextCompat.getMainExecutor(context)
            service.takeScreenshot(displayId, executor, object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    try {
                        val hardwareBuffer = result.hardwareBuffer
                        val colorSpace: ColorSpace? = result.colorSpace
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)?.copy(Bitmap.Config.ARGB_8888, false)
                        hardwareBuffer.close()
                        
                        if (bitmap != null) {
                            continuation.resume(Result.success(bitmap))
                        } else {
                            continuation.resume(Result.failure(IllegalStateException("Unable to create bitmap from screenshot buffer")))
                        }
                    } catch (error: Exception) {
                        continuation.resume(Result.failure(error))
                    }
                }

                override fun onFailure(errorCode: Int) {
                    continuation.resume(Result.failure(IllegalStateException("Screenshot failed: code=$errorCode")))
                }
            })
        }
    }

    private fun resolveDisplayId(service: AccessibilityService): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            service.display?.displayId ?: Display.DEFAULT_DISPLAY
        } else {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.displayId ?: Display.DEFAULT_DISPLAY
        }
    }

    fun buildViewHierarchy(): Result<ScreenState.ViewNode> {
        val root = serviceRef.get()?.rootInActiveWindow ?: return Result.failure(IllegalStateException("Unable to access current window"))
        return runCatching { convertNode(root) }
    }

    private fun convertNode(node: AccessibilityNodeInfo): ScreenState.ViewNode {
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        val children = mutableListOf<ScreenState.ViewNode>()
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            try {
                children.add(convertNode(child))
            } finally {
                child.recycle()
            }
        }
        return ScreenState.ViewNode(
            className = node.className?.toString().orEmpty(),
            text = node.text?.toString().orEmpty(),
            contentDescription = node.contentDescription?.toString().orEmpty(),
            resourceId = node.viewIdResourceName.orEmpty(),
            bounds = bounds,
            clickable = node.isClickable,
            focusable = node.isFocusable,
            enabled = node.isEnabled,
            checkable = node.isCheckable,
            checked = node.isChecked,
            scrollable = node.isScrollable,
            editable = node.isEditable,
            children = children
        )
    }

    fun extractAllTexts(node: ScreenState.ViewNode): List<String> {
        val buffer = mutableListOf<String>()
        fun traverse(current: ScreenState.ViewNode) {
            if (current.text.isNotBlank()) buffer.add(current.text)
            if (current.contentDescription.isNotBlank()) buffer.add(current.contentDescription)
            current.children.forEach { traverse(it) }
        }
        traverse(node)
        return buffer
    }
}
