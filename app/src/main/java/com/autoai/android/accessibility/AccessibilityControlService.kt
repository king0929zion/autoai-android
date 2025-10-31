package com.autoai.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * 无障碍控制服务，实现基础的事件监听并向 [AccessibilityBridge] 提供运行时上下文。
 */
@AndroidEntryPoint
class AccessibilityControlService : AccessibilityService() {

    @Inject lateinit var bridge: AccessibilityBridge

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.i("AccessibilityControlService connected")
        serviceInfo = serviceInfo?.apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        } ?: AccessibilityServiceInfo().apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        bridge.onServiceConnected(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        bridge.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        Timber.w("AccessibilityControlService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("AccessibilityControlService destroyed")
        bridge.onServiceDisconnected(this)
    }
}
