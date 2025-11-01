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
 * Routes automation commands to the currently selected control mode (Accessibility or Shizuku).
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
        val executor = resolveExecutor(controlPreferencesRepository.getCurrentMode())
        return executor.executeAction(action)
    }

    suspend fun getCurrentApp(): Result<String> {
        val executor = resolveExecutor(controlPreferencesRepository.getCurrentMode())
        return executor.getCurrentApp()
    }

    suspend fun switchMode(mode: ControlMode) {
        controlPreferencesRepository.setControlMode(mode)
        Timber.i("Switched control mode to %s", mode)
    }

    suspend fun ensureReady(): Result<Unit> = when (controlPreferencesRepository.getCurrentMode()) {
        ControlMode.SHIZUKU -> {
            if (shizukuManager.isShizukuAvailable()) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Shizuku is not ready. Open the Shizuku app and grant AutoAI permission."))
            }
        }

        ControlMode.ACCESSIBILITY -> {
            if (accessibilityExecutor.isReady()) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Accessibility service is disabled. Enable the AutoAI accessibility service in system settings."))
            }
        }
    }

    suspend fun isReady(): Boolean = ensureReady().isSuccess

    fun observeAccessibilityStatus(): Flow<AccessibilityStatus> = accessibilityBridge.status

    private suspend fun resolveExecutor(mode: ControlMode): ControlActionExecutor = when (mode) {
        ControlMode.ACCESSIBILITY -> {
            if (accessibilityExecutor.isReady()) {
                accessibilityExecutor
            } else {
                object : ControlActionExecutor {
                    override val label: String = accessibilityExecutor.label
                    override suspend fun isReady(): Boolean = false
                    override suspend fun executeAction(action: Action): ActionResult =
                        ActionResult.failure("Accessibility service is disabled. Enable the AutoAI accessibility service in system settings.")

                    override suspend fun getCurrentApp(): Result<String> =
                        Result.failure(IllegalStateException("Accessibility service is not connected"))
                }
            }
        }

        ControlMode.SHIZUKU -> {
            if (shizukuExecutor.isReady()) {
                shizukuExecutor
            } else {
                object : ControlActionExecutor {
                    override val label: String = shizukuExecutor.label
                    override suspend fun isReady(): Boolean = false
                    override suspend fun executeAction(action: Action): ActionResult =
                        ActionResult.failure("Shizuku is not ready. Open the Shizuku app and grant AutoAI permission.")

                    override suspend fun getCurrentApp(): Result<String> =
                        Result.failure(IllegalStateException("Shizuku is not ready"))
                }
            }
        }
    }
}

