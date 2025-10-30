package com.autoai.android.execution

import com.autoai.android.data.model.Action
import com.autoai.android.data.model.ScreenState
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 负责任务执行过程中的安全策略校验。
 * 核心目标：资金安全、隐私保护、用户可控。
 */
@Singleton
class SafetyChecker @Inject constructor() {

    data class SafetyCheckResult(
        val shouldBlock: Boolean,
        val reason: String = "",
        val level: SafetyLevel = SafetyLevel.GREEN,
        val needsConfirmation: Boolean = false
    )

    enum class SafetyLevel {
        GREEN,   // 安全，可自动执行
        YELLOW,  // 需要用户确认
        RED      // 禁止自动执行
    }

    fun checkScreenState(screenState: ScreenState): SafetyCheckResult {
        if (isPaymentScene(screenState)) {
            Timber.w("检测到支付场景，自动阻断后续操作")
            return SafetyCheckResult(
                shouldBlock = true,
                reason = "检测到支付场景，已暂停自动执行，请手动确认支付。",
                level = SafetyLevel.RED
            )
        }

        if (containsSensitiveOperation(screenState)) {
            Timber.w("检测到敏感操作内容，提示用户确认")
            return SafetyCheckResult(
                shouldBlock = false,
                reason = "检测到潜在敏感操作，请确认是否继续。",
                level = SafetyLevel.YELLOW,
                needsConfirmation = true
            )
        }

        if (needsUserConfirmation(screenState)) {
            Timber.d("当前界面需要用户确认")
            return SafetyCheckResult(
                shouldBlock = false,
                reason = "当前操作涉及授权或发送信息，请人工确认。",
                level = SafetyLevel.YELLOW,
                needsConfirmation = true
            )
        }

        return SafetyCheckResult(shouldBlock = false, level = SafetyLevel.GREEN)
    }

    fun checkAction(action: Action, screenState: ScreenState): SafetyCheckResult = when (action) {
        is Action.Input -> {
            if (isPaymentScene(screenState)) {
                SafetyCheckResult(
                    shouldBlock = true,
                    reason = "检测到支付界面，禁止自动输入。",
                    level = SafetyLevel.RED
                )
            } else {
                SafetyCheckResult(false)
            }
        }
        is Action.Click, is Action.LongClick, is Action.Swipe, is Action.Wait,
        is Action.PressKey, is Action.GoBack, is Action.OpenApp, is Action.Complete,
        is Action.Error, is Action.RequestUserHelp -> SafetyCheckResult(false)
    }

    fun containsSensitiveInfo(text: String): Boolean {
        val trimmed = text.replace(" ", "")
        if (CARD_PATTERN.containsMatchIn(trimmed)) return true
        if (ID_PATTERN.containsMatchIn(trimmed)) return true
        if (PHONE_PATTERN.containsMatchIn(trimmed)) return true
        return false
    }

    fun getSafetyLevel(screenState: ScreenState): SafetyLevel = when {
        isPaymentScene(screenState) -> SafetyLevel.RED
        containsSensitiveOperation(screenState) -> SafetyLevel.YELLOW
        else -> SafetyLevel.GREEN
    }

    private fun isPaymentScene(screenState: ScreenState): Boolean {
        val onPaymentApp = PAYMENT_APPS.any { screenState.currentApp.contains(it, ignoreCase = true) }

        val textMatches = screenState.screenText.any { text ->
            PAYMENT_KEYWORDS.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }

        val elementMatches = screenState.uiElements.any { element ->
            PAYMENT_KEYWORDS.any { keyword ->
                element.text.contains(keyword, ignoreCase = true) ||
                    element.contentDescription.contains(keyword, ignoreCase = true)
            }
        }

        val matched = onPaymentApp && (textMatches || elementMatches)
        if (matched) {
            Timber.w("支付场景命中: app=$onPaymentApp text=$textMatches element=$elementMatches")
        }
        return matched
    }

    private fun containsSensitiveOperation(screenState: ScreenState): Boolean {
        return screenState.screenText.any { text ->
            SENSITIVE_KEYWORDS.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }
    }

    private fun needsUserConfirmation(screenState: ScreenState): Boolean {
        val keywordAppears = screenState.screenText.any { text ->
            CONFIRMATION_KEYWORDS.any { keyword -> text.contains(keyword, ignoreCase = true) }
        }
        if (!keywordAppears) return false

        val hasConfirmButton = screenState.uiElements.any { element ->
            element.clickable && element.enabled &&
                CONFIRMATION_KEYWORDS.any { keyword -> element.text.contains(keyword, ignoreCase = true) }
        }
        return hasConfirmButton
    }

    companion object {
        private val PAYMENT_KEYWORDS = listOf(
            "支付", "付款", "确认支付", "输入密码", "验证码", "pay", "payment", "确认订单",
            "立即购买", "提交订单", "金额", "¥"
        )

        private val PAYMENT_APPS = listOf(
            "com.eg.android.AlipayGphone",
            "com.tencent.mm",
            "com.unionpay",
            "com.android.vending"
        )

        private val SENSITIVE_KEYWORDS = listOf(
            "删除", "卸载", "清除数据", "恢复出厂", "格式化",
            "delete", "uninstall", "factory reset", "format"
        )

        private val CONFIRMATION_KEYWORDS = listOf(
            "发送", "分享", "授权", "允许", "同意",
            "提交", "确认", "继续",
            "send", "share", "grant", "allow", "agree", "confirm"
        )

        private val CARD_PATTERN = """\d{16,19}""".toRegex()
        private val ID_PATTERN = """\d{17}[\dxX]""".toRegex()
        private val PHONE_PATTERN = """1[3-9]\d{9}""".toRegex()
    }
}
