package com.autoai.android.permission

/**
 * 描述系统控制所依赖的权限模式。
 * - ACCESSIBILITY：依赖无障碍服务执行操作，默认模式，更易启用。
 * - SHIZUKU：通过 Shizuku 获得系统级 shell 能力，适合需要更高权限的场景。
 */
enum class ControlMode {
    ACCESSIBILITY,
    SHIZUKU;

    fun isAccessibility(): Boolean = this == ACCESSIBILITY
    fun isShizuku(): Boolean = this == SHIZUKU
}
