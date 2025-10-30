package com.autoai.android.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Task 数据模型单元测试
 */
class TaskTest {

    @Test
    fun `任务创建测试`() {
        val task = Task(
            instruction = "打开微信",
            status = TaskStatus.PENDING
        )
        
        assertNotNull(task.id)
        assertEquals("打开微信", task.instruction)
        assertEquals(TaskStatus.PENDING, task.status)
        assertNull(task.todoList)
        assertTrue(task.history.isEmpty())
    }

    @Test
    fun `任务状态判断测试`() {
        assertTrue(TaskStatus.COMPLETED.isFinal())
        assertTrue(TaskStatus.FAILED.isFinal())
        assertTrue(TaskStatus.TERMINATED.isFinal())
        assertFalse(TaskStatus.EXECUTING.isFinal())
        
        assertTrue(TaskStatus.PAUSED.canContinue())
        assertFalse(TaskStatus.COMPLETED.canContinue())
    }

    @Test
    fun `TodoList 进度计算测试`() {
        val steps = listOf(
            TodoStep("步骤1", isCompleted = true),
            TodoStep("步骤2", isCompleted = true),
            TodoStep("步骤3", isCompleted = false),
            TodoStep("步骤4", isCompleted = false)
        )
        
        val todoList = TodoList(steps)
        
        assertEquals(0.5f, todoList.getProgress(), 0.01f)
        assertFalse(todoList.isAllCompleted())
    }

    @Test
    fun `TodoList 标记完成测试`() {
        val steps = listOf(
            TodoStep("步骤1"),
            TodoStep("步骤2")
        )
        
        var todoList = TodoList(steps, currentStepIndex = 0)
        assertEquals(0, todoList.currentStepIndex)
        assertFalse(todoList.steps[0].isCompleted)
        
        todoList = todoList.markCurrentStepCompleted()
        assertEquals(1, todoList.currentStepIndex)
        assertTrue(todoList.steps[0].isCompleted)
    }
}
