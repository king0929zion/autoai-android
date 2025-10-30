package com.autoai.android.permission

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shizuku 服务管理与状态同步。
 */
@Singleton
class ShizukuManager @Inject constructor() {

    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.UNKNOWN)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus.asStateFlow()

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        Timber.d("Shizuku 权限回调: requestCode=%d granted=%s", requestCode, grantResult == PackageManager.PERMISSION_GRANTED)
        refreshStatus()
    }

    fun initialize() {
        runCatching {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            refreshStatus()
            Timber.i("ShizukuManager 初始化完成")
        }.onFailure {
            Timber.e(it, "ShizukuManager 初始化失败")
            _shizukuStatus.value = ShizukuStatus.NOT_INSTALLED
        }
    }

    fun cleanup() {
        runCatching {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Timber.d("ShizukuManager 清理完成")
        }.onFailure {
            Timber.e(it, "ShizukuManager 清理失败")
        }
    }

    fun isShizukuAvailable(): Boolean = runCatching {
        if (!Shizuku.pingBinder()) {
            Timber.w("Shizuku binder 未响应")
            return false
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Timber.w("尚未授予 Shizuku 权限")
            return false
        }
        true
    }.getOrElse {
        Timber.e(it, "检测 Shizuku 可用性失败")
        false
    }

    fun requestPermission(): Boolean = runCatching {
        if (!Shizuku.pingBinder()) {
            Timber.w("Shizuku 未运行，无法申请权限")
            _shizukuStatus.value = ShizukuStatus.NOT_RUNNING
            return false
        }

        when (Shizuku.checkSelfPermission()) {
            PackageManager.PERMISSION_GRANTED -> {
                Timber.d("Shizuku 权限已授予")
                _shizukuStatus.value = ShizukuStatus.AVAILABLE
                true
            }
            PackageManager.PERMISSION_DENIED -> {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    Timber.d("需要显示权限说明")
                }
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
                _shizukuStatus.value = ShizukuStatus.PERMISSION_REQUIRED
                Timber.d("已发起 Shizuku 权限请求")
                true
            }
            else -> false
        }
    }.getOrElse {
        Timber.e(it, "申请 Shizuku 权限失败")
        _shizukuStatus.value = ShizukuStatus.ERROR
        false
    }

    fun getShizukuVersion(): Int = runCatching {
        Shizuku.getVersion()
    }.getOrElse {
        Timber.e(it, "获取 Shizuku 版本失败")
        -1
    }

    private fun refreshStatus() {
        val status = runCatching {
            when {
                !isInstalled() -> {
                    Timber.d("Shizuku 未安装")
                    ShizukuStatus.NOT_INSTALLED
                }
                !Shizuku.pingBinder() -> {
                    Timber.d("Shizuku 未运行")
                    ShizukuStatus.NOT_RUNNING
                }
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> {
                    Timber.d("Shizuku 权限未授予")
                    ShizukuStatus.PERMISSION_REQUIRED
                }
                else -> {
                    Timber.d("Shizuku 可正常使用")
                    ShizukuStatus.AVAILABLE
                }
            }
        }.getOrElse {
            Timber.e(it, "刷新 Shizuku 状态失败")
            ShizukuStatus.ERROR
        }

        _shizukuStatus.value = status
    }

    private fun isInstalled(): Boolean = runCatching {
        Shizuku.getVersion() >= 0
    }.getOrDefault(false)

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 1001
    }
}

enum class ShizukuStatus {
    UNKNOWN,
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    AVAILABLE,
    ERROR;

    fun canExecute(): Boolean = this == AVAILABLE
}
