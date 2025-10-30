package com.autoai.android.data.model

import java.util.UUID

/**
 * 表示一次完整的用户任务
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val status: TaskStatus = TaskStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val currentStep: Int = 0,
    val history: List<ActionHistory> = emptyList(),
    val result: String? = null,
    val error: String? = null,
    val todoList: TodoList? = null
) {
    /**
     * 任务持续时长（毫秒）
     */
    fun durationMs(now: Long = System.currentTimeMillis()): Long {
        val start = startedAt ?: createdAt
        val end = completedAt ?: now
        return (end - start).coerceAtLeast(0L)
    }

    /**
     * 任务进度（0.0 - 1.0）
     */
    fun progress(): Float = todoList?.getProgress() ?: 0f

    /**
     * 单步执行历史
     */
    data class ActionHistory(
        val step: Int,
        val action: Action,
        val result: ActionResult,
        val timestamp: Long = System.currentTimeMillis(),
        val screenStateBefore: ScreenState? = null,
        val screenStateAfter: ScreenState? = null,
        val reasoning: String = ""
    ) {
        fun summary(): String {
            val actionDesc = when (action) {
                is Action.Click -> "Click (${action.x}, ${action.y})"
                is Action.LongClick -> "Long click (${action.x}, ${action.y})"
                is Action.Swipe -> "Swipe ${action.fromX},${action.fromY} -> ${action.toX},${action.toY}"
                is Action.Input -> "Input text"
                is Action.PressKey -> "Press key ${action.keyCode}"
                is Action.OpenApp -> "Open ${action.appName.ifBlank { action.packageName }}"
                is Action.Wait -> "Wait ${action.durationMs}ms"
                is Action.GoBack -> "Go back"
                is Action.Complete -> "Complete"
                is Action.Error -> "Error"
                else -> action::class.simpleName ?: "Action"
            }
            val prefix = if (result.success) "[OK]" else "[X]"
            return "$prefix $actionDesc - ${result.message}"
        }
    }
}

/**
 * 任务状态
 */
enum class TaskStatus {
    /** 等待执行 */
    PENDING,

    /** 正在执行 */
    RUNNING,

    /** 已暂停，等待用户继续 */
    PAUSED,

    /** 已完成 */
    COMPLETED,

    /** 执行失败 */
    FAILED,

    /** 已取消 */
    CANCELLED;

    fun isTerminal(): Boolean = this in setOf(COMPLETED, FAILED, CANCELLED)

    fun canResume(): Boolean = this == PAUSED
}

/**
 * 待办清单（可选）
 */
data class TodoList(
    val steps: List<TodoStep>,
    val currentStepIndex: Int = 0,
    val lastUpdateTime: Long = System.currentTimeMillis()
) {
    fun currentStep(): TodoStep? = steps.getOrNull(currentStepIndex)

    fun getProgress(): Float {
        if (steps.isEmpty()) return 0f
        val completedCount = steps.count { it.isCompleted }
        return completedCount.toFloat() / steps.size
    }

    fun markCurrentStepCompleted(): TodoList {
        if (currentStepIndex >= steps.size) return this

        val updatedSteps = steps.toMutableList()
        updatedSteps[currentStepIndex] = updatedSteps[currentStepIndex].copy(
            isCompleted = true,
            completedTime = System.currentTimeMillis()
        )

        return copy(
            steps = updatedSteps,
            currentStepIndex = currentStepIndex + 1,
            lastUpdateTime = System.currentTimeMillis()
        )
    }

    fun isAllCompleted(): Boolean = steps.all { it.isCompleted }
}

/**
 * 待办步骤
 */
data class TodoStep(
    val description: String,
    val expectedResult: String = "",
    val isCompleted: Boolean = false,
    val isKeyStep: Boolean = false,
    val completedTime: Long? = null
)
