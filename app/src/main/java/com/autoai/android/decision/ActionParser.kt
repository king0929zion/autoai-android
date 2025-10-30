package com.autoai.android.decision

import com.autoai.android.data.model.Action
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 动作解析器
 * 
 * 将 AI 返回的 JSON 格式指令解析为 Action 对象
 */
@Singleton
class ActionParser @Inject constructor() {
    
    companion object {
        private const val TAG = "ActionParser"
    }
    
    private val gson = Gson()

    /**
     * 解析 AI 响应为 Action
     * 
     * @param response AI 响应内容
     * @return Action 对象，如果解析失败返回错误
     */
    fun parse(response: String): Result<Action> {
        try {
            Timber.d("开始解析 AI 响应: ${response.take(200)}")
            
            // 提取 JSON（有时 AI 会返回带说明的文本）
            val jsonString = extractJson(response)
            if (jsonString.isEmpty()) {
                return Result.failure(Exception("未找到有效的 JSON"))
            }
            
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            
            val action = when (val actionType = jsonObject.get("action")?.asString) {
                "click" -> parseClick(jsonObject)
                "long_click" -> parseLongClick(jsonObject)
                "swipe" -> parseSwipe(jsonObject)
                "input" -> parseInput(jsonObject)
                "press_key" -> parsePressKey(jsonObject)
                "open_app" -> parseOpenApp(jsonObject)
                "wait" -> parseWait(jsonObject)
                "go_back" -> Action.GoBack
                "complete" -> parseComplete(jsonObject)
                "error" -> parseError(jsonObject)
                else -> {
                    Timber.w("未知的动作类型: $actionType")
                    Action.Error("未知的动作类型: $actionType")
                }
            }
            
            Timber.d("解析成功: ${action::class.simpleName}")
            Result.success(action)
        } catch (e: Exception) {
            Timber.e(e, "解析 AI 响应失败")
            Result.failure(e)
        }
    }

    /**
     * 从文本中提取 JSON
     */
    private fun extractJson(text: String): String {
        // 尝试直接解析
        if (text.trim().startsWith("{")) {
            return text.trim()
        }
        
        // 查找 JSON 代码块
        val jsonBlockRegex = """```json\s*(.*?)\s*```""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val jsonMatch = jsonBlockRegex.find(text)
        if (jsonMatch != null) {
            return jsonMatch.groupValues[1].trim()
        }
        
        // 查找大括号包围的内容
        val braceRegex = """\{.*?\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val braceMatch = braceRegex.find(text)
        if (braceMatch != null) {
            return braceMatch.value
        }
        
        return ""
    }

    private fun parseClick(json: JsonObject): Action {
        val x = json.get("x")?.asInt ?: 0
        val y = json.get("y")?.asInt ?: 0
        return Action.Click(x, y)
    }

    private fun parseLongClick(json: JsonObject): Action {
        val x = json.get("x")?.asInt ?: 0
        val y = json.get("y")?.asInt ?: 0
        val duration = json.get("duration")?.asLong ?: 1000L
        return Action.LongClick(x, y, duration)
    }

    private fun parseSwipe(json: JsonObject): Action {
        val fromX = json.get("from_x")?.asInt ?: json.get("fromX")?.asInt ?: 0
        val fromY = json.get("from_y")?.asInt ?: json.get("fromY")?.asInt ?: 0
        val toX = json.get("to_x")?.asInt ?: json.get("toX")?.asInt ?: 0
        val toY = json.get("to_y")?.asInt ?: json.get("toY")?.asInt ?: 0
        val duration = json.get("duration")?.asLong ?: 300L
        return Action.Swipe(fromX, fromY, toX, toY, duration)
    }

    private fun parseInput(json: JsonObject): Action {
        val text = json.get("text")?.asString ?: ""
        return Action.Input(text)
    }

    private fun parsePressKey(json: JsonObject): Action {
        val keyCode = json.get("key_code")?.asInt 
            ?: json.get("keyCode")?.asInt 
            ?: 0
        return Action.PressKey(keyCode)
    }

    private fun parseOpenApp(json: JsonObject): Action {
        val packageName = json.get("package")?.asString 
            ?: json.get("package_name")?.asString 
            ?: json.get("packageName")?.asString 
            ?: ""
        return Action.OpenApp(packageName)
    }

    private fun parseWait(json: JsonObject): Action {
        val duration = json.get("duration")?.asLong ?: 1000L
        return Action.Wait(duration)
    }

    private fun parseComplete(json: JsonObject): Action {
        val message = json.get("message")?.asString ?: "任务完成"
        return Action.Complete(message)
    }

    private fun parseError(json: JsonObject): Action {
        val message = json.get("message")?.asString ?: "未知错误"
        return Action.Error(message)
    }

    /**
     * 验证 Action 是否有效
     * 
     * @param action 动作
     * @return 是否有效
     */
    fun validate(action: Action): Boolean {
        return when (action) {
            is Action.Click -> action.x >= 0 && action.y >= 0
            is Action.LongClick -> action.x >= 0 && action.y >= 0
            is Action.Swipe -> action.fromX >= 0 && action.fromY >= 0 && 
                              action.toX >= 0 && action.toY >= 0
            is Action.Input -> action.text.isNotBlank()
            is Action.PressKey -> action.keyCode > 0
            is Action.OpenApp -> action.packageName.isNotBlank()
            is Action.Wait -> action.durationMs > 0
            is Action.GoBack -> true
            is Action.Complete -> true
            is Action.Error -> true
            is Action.RequestUserHelp -> true
        }
    }
}
