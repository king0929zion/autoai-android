package com.autoai.android.permission

/**
 * Available control strategies for automating the device.
 *
 * - ACCESSIBILITY performs actions through accessibility gestures (default, no elevated privilege).
 * - SHIZUKU executes privileged shell commands via the Shizuku service.
 */
enum class ControlMode {
    ACCESSIBILITY,
    SHIZUKU;

    fun isAccessibility(): Boolean = this == ACCESSIBILITY
    fun isShizuku(): Boolean = this == SHIZUKU
}

