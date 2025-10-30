package com.autoai.android.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Action 数据模型单元测试
 */
class ActionTest {

    @Test
    fun `点击动作创建测试`() {
        val action = Action.Click(100, 200, "测试按钮")
        
        assertTrue(action is Action.Click)
        assertEquals(100, action.x)
        assertEquals(200, action.y)
        assertEquals("测试按钮", action.elementDescription)
    }

    @Test
    fun `输入动作创建测试`() {
        val action = Action.Input("Hello World")
        
        assertTrue(action is Action.Input)
        assertEquals("Hello World", action.text)
    }

    @Test
    fun `ActionResult 成功测试`() {
        val result = ActionResult.success("操作成功", 100)
        
        assertTrue(result.success)
        assertEquals("操作成功", result.message)
        assertEquals(100, result.executionTimeMs)
        assertNull(result.error)
    }

    @Test
    fun `ActionResult 失败测试`() {
        val exception = Exception("测试错误")
        val result = ActionResult.failure("操作失败", exception, 50)
        
        assertFalse(result.success)
        assertEquals("操作失败", result.message)
        assertEquals(50, result.executionTimeMs)
        assertEquals(exception, result.error)
    }
}
