package com.autoai.android.task

import com.autoai.android.data.model.Action
import com.autoai.android.data.model.ActionResult
import com.autoai.android.data.model.Task
import com.autoai.android.data.model.TaskStatus
import com.autoai.android.execution.ExecutionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责管理任务生命周期与状态同步。
 */
@Singleton
class TaskManager @Inject constructor(
    private val executionEngine: ExecutionEngine
) {

    private val _currentTask = MutableStateFlow<Task?>(null)
    val currentTask: StateFlow<Task?> = _currentTask.asStateFlow()

    private val taskHistory = mutableListOf<Task>()

    suspend fun executeTask(
        description: String,
        onProgress: ((Task) -> Unit)? = null
    ): Result<String> {
        return try {
            var task = Task(
                id = UUID.randomUUID().toString(),
                description = description,
                status = TaskStatus.RUNNING,
                createdAt = System.currentTimeMillis(),
                startedAt = System.currentTimeMillis()
            )
            _currentTask.value = task
            Timber.d("任务开始: %s - %s", task.id, description)

            val result = executionEngine.executeTask(
                taskDescription = description,
                maxSteps = 30
            ) { step, action, actionResult ->
                val actionHistory = Task.ActionHistory(
                    step = step,
                    action = action,
                    result = actionResult,
                    timestamp = System.currentTimeMillis()
                )
                task = task.copy(
                    currentStep = step,
                    history = task.history + actionHistory
                )
                _currentTask.value = task
                onProgress?.invoke(task)
                Timber.d("任务进度: step=%d action=%s", step, action::class.simpleName)
            }

            task = if (result.isSuccess) {
                task.copy(
                    status = TaskStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                    result = result.getOrNull()
                )
            } else {
                task.copy(
                    status = TaskStatus.FAILED,
                    completedAt = System.currentTimeMillis(),
                    error = result.exceptionOrNull()?.message
                )
            }

            _currentTask.value = task
            taskHistory.add(task)
            Timber.d("任务结束: %s 状态=%s", task.id, task.status)

            result
        } catch (error: Exception) {
            Timber.e(error, "任务执行异常")
            _currentTask.value = _currentTask.value?.copy(
                status = TaskStatus.FAILED,
                completedAt = System.currentTimeMillis(),
                error = error.message
            )
            Result.failure(error)
        }
    }

    suspend fun executeSingleTask(description: String): Result<ActionResult> {
        return try {
            var task = Task(
                id = UUID.randomUUID().toString(),
                description = description,
                status = TaskStatus.RUNNING,
                createdAt = System.currentTimeMillis(),
                startedAt = System.currentTimeMillis()
            )
            _currentTask.value = task

            val result = executionEngine.executeSingleStep(description)
            task = if (result.isSuccess) {
                val action = executionEngine.getHistory().lastOrNull() ?: Action.Error("未记录动作")
                task.copy(
                    status = TaskStatus.COMPLETED,
                    completedAt = System.currentTimeMillis(),
                    currentStep = 1,
                    history = listOf(
                        Task.ActionHistory(
                            step = 1,
                            action = action,
                            result = result.getOrThrow(),
                            timestamp = System.currentTimeMillis()
                        )
                    )
                )
            } else {
                task.copy(
                    status = TaskStatus.FAILED,
                    completedAt = System.currentTimeMillis(),
                    error = result.exceptionOrNull()?.message
                )
            }

            _currentTask.value = task
            taskHistory.add(task)
            result
        } catch (error: Exception) {
            Timber.e(error, "单步任务执行异常")
            _currentTask.value = _currentTask.value?.copy(
                status = TaskStatus.FAILED,
                completedAt = System.currentTimeMillis(),
                error = error.message
            )
            Result.failure(error)
        }
    }

    fun pauseTask() {
        val task = _currentTask.value ?: return
        if (task.status == TaskStatus.RUNNING) {
            _currentTask.value = task.copy(status = TaskStatus.PAUSED)
            executionEngine.stop()
            Timber.d("任务已暂停: %s", task.id)
        }
    }

    fun cancelTask() {
        val task = _currentTask.value ?: return
        _currentTask.value = task.copy(
            status = TaskStatus.CANCELLED,
            completedAt = System.currentTimeMillis()
        )
        executionEngine.stop()
        Timber.d("任务已取消: %s", task.id)
    }

    fun getTaskHistory(): List<Task> = taskHistory.toList()

    fun clearHistory() {
        taskHistory.clear()
        Timber.d("任务历史已清空")
    }

    fun isComplexTask(description: String): Boolean {
        val keywords = listOf("并且", "然后", "接着", "之后", "最后", "搜索", "找到", "截图")
        val containsKeyword = keywords.any { description.contains(it) }
        val longSentence = description.length > 20
        return containsKeyword || longSentence
    }
}
