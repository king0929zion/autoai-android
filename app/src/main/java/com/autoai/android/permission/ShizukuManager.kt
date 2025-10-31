package com.autoai.android.permission

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.Volatile

/**
 * Shizuku 服务状态管理器
 * 负责监听权限状态与 binder 生命周期，向 UI 暴露实时状态。
 */
@Singleton
class ShizukuManager @Inject constructor() {

    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.UNKNOWN)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus.asStateFlow()

    @Volatile
    private var listenersRegistered = false

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        Timber.d(
            "Shizuku 权限回调: requestCode=%d granted=%s",
            requestCode,
            grantResult == PackageManager.PERMISSION_GRANTED
        )
        refreshStatus()
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Timber.d("Shizuku binder 已连接")
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Timber.w("Shizuku binder 已断开")
        _shizukuStatus.value = ShizukuStatus.NOT_RUNNING
    }

    @Synchronized
    fun initialize() {
        if (listenersRegistered) {
            Timber.d("ShizukuManager 已初始化，刷新当前状态")
            refreshStatus()
            return
        }

        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
            listenersRegistered = true
            refreshStatus()
            Timber.i("ShizukuManager 初始化完成")
        }.onFailure {
            listenersRegistered = false
            Timber.e(it, "ShizukuManager 初始化失败")
            _shizukuStatus.value = ShizukuStatus.NOT_INSTALLED
        }
    }

    @Synchronized
    fun cleanup() {
        if (!listenersRegistered) return

        runCatching {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            listenersRegistered = false
            Timber.d("ShizukuManager 清理完成")
        }.onFailure {
            Timber.e(it, "ShizukuManager 清理失败")
        }
    }

    fun isShizukuAvailable(): Boolean {
        if (_shizukuStatus.value == ShizukuStatus.AVAILABLE) {
            return true
        }

        return runCatching {
            if (!Shizuku.pingBinder()) {
                Timber.w("Shizuku binder 未响应")
                return@runCatching false
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Timber.w("Shizuku 权限未授予")
                return@runCatching false
            }
            true
        }.getOrElse {
            Timber.e(it, "检查 Shizuku 可用性失败")
            false
        }
    }

    fun requestPermission(): Boolean = runCatching {
        if (!Shizuku.pingBinder()) {
            Timber.w("Shizuku 未运行，无法申请权限")
            _shizukuStatus.value = ShizukuStatus.NOT_RUNNING
            return@runCatching false
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

        if (_shizukuStatus.value != status) {
            _shizukuStatus.value = status
        }
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
