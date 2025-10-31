package com.autoai.android.accessibility

/**
 * Represents the availability state of the accessibility control service.
 */
enum class AccessibilityStatus {
    /** Accessibility service is disabled in system settings. */
    SERVICE_DISABLED,

    /** Service is attempting to connect. */
    CONNECTING,

    /** Service is connected and ready to perform gestures. */
    READY,

    /** Service is unavailable due to an error. */
    ERROR;

    fun isReady(): Boolean = this == READY
}
