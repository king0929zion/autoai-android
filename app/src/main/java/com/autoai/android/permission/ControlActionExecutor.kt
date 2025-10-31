package com.autoai.android.permission

import com.autoai.android.data.model.Action
import com.autoai.android.data.model.ActionResult

/**
 * 抽象出的系统控制执行器，屏蔽不同权限模式的实现差异。
 */
interface ControlActionExecutor {
    val label: String
    suspend fun isReady(): Boolean
    suspend fun executeAction(action: Action): ActionResult
    suspend fun getCurrentApp(): Result<String>
}
