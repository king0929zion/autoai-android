package com.autoai.android.execution

import com.autoai.android.data.model.Action
import com.autoai.android.data.model.ActionResult
import com.autoai.android.decision.ActionParser
import com.autoai.android.decision.PromptBuilder
import com.autoai.android.decision.VLMClient
import com.autoai.android.perception.MultiModalFusion
import com.autoai.android.permission.OperationExecutor
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 感知 -> 决策 -> 执行 -> 校验 的主循环实现。
 */
@Singleton
class ExecutionEngine @Inject constructor(
    private val multiModalFusion: MultiModalFusion,
    private val vlmClient: VLMClient,
    private val promptBuilder: PromptBuilder,
    private val actionParser: ActionParser,
    private val operationExecutor: OperationExecutor,
    private val safetyChecker: SafetyChecker
) {

    companion object {
        private const val MAX_RETRY = 3
        private const val WAIT_AFTER_ACTION_MS = 1_500L
        private const val RETRY_INTERVAL_MS = 2_000L
    }

    private val actionHistory = mutableListOf<Action>()
    private var running = false

    suspend fun executeSingleStep(taskDescription: String): Result<ActionResult> = runCatching {
        Timber.d("执行单步任务: %s", taskDescription)

        val currentApp = operationExecutor.getCurrentApp().getOrElse { "unknown" }
        val screenState = multiModalFusion.generateScreenState(currentApp).getOrElse { error ->
            throw IllegalStateException("无法感知当前界面: ${error.message}", error)
        }

        val screenSafety = safetyChecker.checkScreenState(screenState)
        if (screenSafety.shouldBlock) {
            return@runCatching ActionResult.failure(
                message = screenSafety.reason.ifBlank { "安全策略阻止了此次操作" },
                needsUserConfirmation = screenSafety.needsConfirmation
            )
        }

        val systemPrompt = promptBuilder.buildSystemPrompt()
        val userPrompt = promptBuilder.buildUserPrompt(
            task = taskDescription,
            screenState = screenState,
            history = actionHistory.takeLast(3)
        )
        val imageDataUri = "data:image/jpeg;base64,${screenState.screenshotBase64}"

        val aiResponse = vlmClient.chat(systemPrompt, userPrompt, imageDataUri)
            .getOrElse { error -> throw IllegalStateException("AI 决策失败: ${error.message}", error) }

        val action = actionParser.parse(aiResponse)
            .getOrElse { error -> throw IllegalStateException("无法解析 AI 响应: ${error.message}", error) }

        require(actionParser.validate(action)) { "AI 返回的动作无效: $action" }

        val actionSafety = safetyChecker.checkAction(action, screenState)
        if (actionSafety.shouldBlock) {
            return@runCatching ActionResult.failure(
                message = actionSafety.reason.ifBlank { "操作被安全策略阻止" },
                needsUserConfirmation = actionSafety.needsConfirmation
            )
        }

        Timber.d("执行动作: %s", action::class.simpleName)
        val result = operationExecutor.executeAction(action)
        actionHistory.add(action)

        delay(WAIT_AFTER_ACTION_MS)
        result
    }.onFailure { Timber.e(it, "单步任务执行失败") }

    suspend fun executeTask(
        taskDescription: String,
        maxSteps: Int = 20,
        onProgress: ((step: Int, action: Action, result: ActionResult) -> Unit)? = null
    ): Result<String> = runCatching {
        running = true
        actionHistory.clear()

        var step = 0
        var retryCount = 0

        while (running && step < maxSteps) {
            val stepResult = executeSingleStep(taskDescription)
            if (stepResult.isFailure) {
                val error = stepResult.exceptionOrNull()!!
                if (retryCount < MAX_RETRY) {
                    retryCount++
                    Timber.w(error, "执行失败，准备第 %d 次重试", retryCount)
                    delay(RETRY_INTERVAL_MS)
                    continue
                } else {
                    throw error
                }
            }

            retryCount = 0
            val actionResult = stepResult.getOrThrow()
            val lastAction = actionHistory.lastOrNull()

            if (lastAction != null) {
                step++
                onProgress?.invoke(step, lastAction, actionResult)

                if (actionResult.needsUserConfirmation) {
                    return@runCatching "需要用户确认: ${actionResult.message}"
                }

                when (lastAction) {
                    is Action.Complete -> return@runCatching lastAction.message
                    is Action.Error -> error("AI 主动上报错误: ${lastAction.message}")
                    else -> if (isStuck()) {
                        error("检测到任务可能卡死，连续执行相同动作")
                    }
                }
            }
        }

        if (step >= maxSteps) {
            error("达到最大步数限制 ($maxSteps)")
        }

        "任务执行完成"
    }.onFailure { Timber.e(it, "任务执行失败") }
        .also { running = false }

    fun stop() {
        running = false
        Timber.d("执行引擎收到停止请求")
    }

    fun clearHistory() {
        actionHistory.clear()
    }

    fun getHistory(): List<Action> = actionHistory.toList()

    private fun isStuck(): Boolean {
        if (actionHistory.size < 5) return false
        val recent = actionHistory.takeLast(5)
        return recent.all { it::class == recent.first()::class }
    }
}
