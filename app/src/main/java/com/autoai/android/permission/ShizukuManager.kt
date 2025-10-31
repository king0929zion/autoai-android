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
 * Manages Shizuku binder state and permission status, exposing updates to the UI layer.
 */
@Singleton
class ShizukuManager @Inject constructor() {

    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.UNKNOWN)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus.asStateFlow()

    @Volatile
    private var listenersRegistered = false

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        Timber.d("Shizuku permission callback: requestCode=%d granted=%s", requestCode, grantResult == PackageManager.PERMISSION_GRANTED)
        refreshStatus()
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Timber.d("Shizuku binder connected")
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Timber.w("Shizuku binder disconnected")
        _shizukuStatus.value = ShizukuStatus.NOT_RUNNING
    }

    @Synchronized
    fun initialize() {
        if (listenersRegistered) {
            Timber.d("ShizukuManager already initialized, refreshing status")
            refreshStatus()
            return
        }

        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
            listenersRegistered = true
            refreshStatus()
            Timber.i("ShizukuManager initialization complete")
        }.onFailure {
            listenersRegistered = false
            Timber.e(it, "Failed to initialize ShizukuManager")
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
            Timber.d("ShizukuManager cleaned up")
        }.onFailure {
            Timber.e(it, "Failed to clean up ShizukuManager")
        }
    }

    fun isShizukuAvailable(): Boolean {
        if (_shizukuStatus.value == ShizukuStatus.AVAILABLE) {
            return true
        }

        return runCatching {
            if (!Shizuku.pingBinder()) {
                Timber.w("Shizuku binder not responding")
                return@runCatching false
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Timber.w("Shizuku permission not granted")
                return@runCatching false
            }
            true
        }.getOrElse {
            Timber.e(it, "Failed to check Shizuku availability")
            false
        }
    }

    fun requestPermission(): Boolean = runCatching {
        if (!Shizuku.pingBinder()) {
            Timber.w("Shizuku not running, cannot request permission")
            _shizukuStatus.value = ShizukuStatus.NOT_RUNNING
            return@runCatching false
        }

        when (Shizuku.checkSelfPermission()) {
            PackageManager.PERMISSION_GRANTED -> {
                Timber.d("Shizuku permission already granted")
                _shizukuStatus.value = ShizukuStatus.AVAILABLE
                true
            }
            PackageManager.PERMISSION_DENIED -> {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    Timber.d("Should show Shizuku permission rationale")
                }
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
                _shizukuStatus.value = ShizukuStatus.PERMISSION_REQUIRED
                Timber.d("Requested Shizuku permission")
                true
            }
            else -> false
        }
    }.getOrElse {
        Timber.e(it, "Failed to request Shizuku permission")
        _shizukuStatus.value = ShizukuStatus.ERROR
        false
    }

    fun getShizukuVersion(): Int = runCatching {
        Shizuku.getVersion()
    }.getOrElse {
        Timber.e(it, "Failed to obtain Shizuku version")
        -1
    }

    private fun refreshStatus() {
        val status = runCatching {
            when {
                !isInstalled() -> {
                    Timber.d("Shizuku not installed")
                    ShizukuStatus.NOT_INSTALLED
                }
                !Shizuku.pingBinder() -> {
                    Timber.d("Shizuku not running")
                    ShizukuStatus.NOT_RUNNING
                }
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> {
                    Timber.d("Shizuku permission missing")
                    ShizukuStatus.PERMISSION_REQUIRED
                }
                else -> {
                    Timber.d("Shizuku is available")
                    ShizukuStatus.AVAILABLE
                }
            }
        }.getOrElse {
            Timber.e(it, "Failed to refresh Shizuku status")
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
