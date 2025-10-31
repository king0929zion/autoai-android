package com.autoai.android.permission

import com.autoai.android.accessibility.AccessibilityBridge
import com.autoai.android.accessibility.AccessibilityStatus
import com.autoai.android.data.model.Action
import com.autoai.android.data.model.ActionResult
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified operation executor that routes requests to the active control mode.
 */
@Singleton
class OperationExecutor @Inject constructor(
    private val controlPreferencesRepository: ControlPreferencesRepository,
    private val accessibilityExecutor: AccessibilityActionExecutor,
    private val shizukuExecutor: ShizukuActionExecutor,
    private val shizukuManager: ShizukuManager,
    private val accessibilityBridge: AccessibilityBridge
) {

    val controlModeFlow: Flow<ControlMode> = controlPreferencesRepository.controlModeFlow

    suspend fun executeAction(action: Action): ActionResult {
        val mode = controlPreferencesRepository.getCurrentMode()
        val executor = resolveExecutor(mode)
        return executor.executeAction(action)
    }

    suspend fun getCurrentApp(): Result<String> {
        val mode = controlPreferencesRepository.getCurrentMode()
        val executor = resolveExecutor(mode)
        return executor.getCurrentApp()
    }

    suspend fun switchMode(mode: ControlMode) {
        controlPreferencesRepository.setControlMode(mode)
        Timber.i("切换控制模式：%s", mode)
    }

    suspend fun ensureReady(): Result<Unit> = when (controlPreferencesRepository.getCurrentMode()) {
        ControlMode.SHIZUKU -> {
            if (shizukuManager.isShizukuAvailable()) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Shizuku 未就绪，请先在 Shizuku 应用中授予 AutoAI 权限"))
            }
        }
        ControlMode.ACCESSIBILITY -> {
            if (accessibilityExecutor.isReady()) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("无障碍服务未启用，请在系统设置中开启 AutoAI 无障碍服务"))
            }
        }
    }

    suspend fun isReady(): Boolean = ensureReady().isSuccess

    fun observeAccessibilityStatus(): Flow<AccessibilityStatus> = accessibilityBridge.status

    private suspend fun resolveExecutor(mode: ControlMode): ControlActionExecutor {
        return when (mode) {
            ControlMode.ACCESSIBILITY -> {
                if (!accessibilityExecutor.isReady()) {
                    return object : ControlActionExecutor {
                        override val label: String = accessibilityExecutor.label
                        override suspend fun isReady(): Boolean = false
                        override suspend fun executeAction(action: Action): ActionResult =
                            ActionResult.failure("无障碍服务未启用，请在系统设置中开启 AutoAI 无障碍服务")

                        override suspend fun getCurrentApp(): Result<String> =
                            Result.failure(IllegalStateException("无障碍服务未启用"))
                    }
                }
                accessibilityExecutor
            }
            ControlMode.SHIZUKU -> {
                if (!shizukuExecutor.isReady()) {
                    return object : ControlActionExecutor {
                        override val label: String = shizukuExecutor.label
                        override suspend fun isReady(): Boolean = false
                        override suspend fun executeAction(action: Action): ActionResult =
                            ActionResult.failure("Shizuku 未就绪，请先在 Shizuku 应用中授予 AutoAI 权限")

                        override suspend fun getCurrentApp(): Result<String> =
                            Result.failure(IllegalStateException("Shizuku 未就绪"))
                    }
                }
                shizukuExecutor
            }
        }
    }
}
