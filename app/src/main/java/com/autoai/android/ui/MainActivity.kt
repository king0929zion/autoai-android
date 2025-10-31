package com.autoai.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autoai.android.accessibility.AccessibilityBridge
import com.autoai.android.accessibility.AccessibilityStatus
import com.autoai.android.permission.ControlMode
import com.autoai.android.permission.OperationExecutor
import com.autoai.android.permission.ShizukuManager
import com.autoai.android.permission.ShizukuStatus
import com.autoai.android.ui.chat.ChatScreen
import com.autoai.android.ui.help.HelpScreen
import com.autoai.android.ui.history.HistoryScreen
import com.autoai.android.ui.settings.SettingsScreen
import com.autoai.android.ui.theme.AutoAITheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Main activity that loads the appropriate navigation stack based on the current control mode.
 * Main activity that selects the proper navigation stack based on the active control mode.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var shizukuManager: ShizukuManager
    @Inject lateinit var operationExecutor: OperationExecutor
    @Inject lateinit var accessibilityBridge: AccessibilityBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity created")

        // Pre-initialize Shizuku state listeners to avoid stale status when switching modes
        shizukuManager.initialize()
        // Pre-initialize Shizuku so mode switches have up-to-date status information
        setContent {
            AutoAITheme {
                val controlMode by operationExecutor.controlModeFlow.collectAsState(initial = ControlMode.ACCESSIBILITY)
                val shizukuStatus by shizukuManager.shizukuStatus.collectAsState()
                val accessibilityStatus by operationExecutor.observeAccessibilityStatus()
                    .collectAsState(initial = AccessibilityStatus.SERVICE_DISABLED)

                LaunchedEffect(Unit) {
                    accessibilityBridge.refreshStatus()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(
                        controlMode = controlMode,
                        shizukuStatus = shizukuStatus,
                        accessibilityStatus = accessibilityStatus,
                        shizukuManager = shizukuManager,
                        accessibilityBridge = accessibilityBridge
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shizukuManager.cleanup()
        Timber.d("MainActivity destroyed")
    }
}

@Composable
private fun MainContent(
    controlMode: ControlMode,
    shizukuStatus: ShizukuStatus,
    accessibilityStatus: AccessibilityStatus,
    shizukuManager: ShizukuManager,
    accessibilityBridge: AccessibilityBridge
) {
    val navController = rememberNavController()

    when (controlMode) {
        ControlMode.ACCESSIBILITY -> {
            when {
                accessibilityStatus.isReady() -> {
                    AppNavigation(navController)
                }
                accessibilityStatus == AccessibilityStatus.CONNECTING -> {
                    StatusScreen(
                        title = "正在连接无障碍服务…",
                        description = "请稍候，AutoAI 正在等待系统完成无障碍服务的绑定。",
                        primaryButtonLabel = null,
                        onPrimaryClick = null,
                        secondaryDescription = "若长时间无响应，请到设置 > 辅助功能 > 下载的应用中重新启用 AutoAI。"
                    )
                }
                accessibilityStatus == AccessibilityStatus.SERVICE_DISABLED -> {
                    StatusScreen(
                        title = "请启用无障碍服务",
                        description = "AutoAI 现已支持无障碍模式，无需 Shizuku 也能执行大部分自动化操作。请点击下方按钮，前往系统设置启用 AutoAI 无障碍服务。",
                        primaryButtonLabel = "打开无障碍设置",
                        onPrimaryClick = { accessibilityBridge.openAccessibilitySettings() },
                        secondaryDescription = "打开设置后，请在“已下载的服务”列表中找到 AutoAI 并启用。"
                    )
                }
                else -> {
                    StatusScreen(
                        title = "无障碍服务出现异常",
                        description = "无法连接到无障碍服务，请尝试在系统设置中关闭后重新启用 AutoAI 服务。",
                        primaryButtonLabel = "前往无障碍设置",
                        onPrimaryClick = { accessibilityBridge.openAccessibilitySettings() },
                        secondaryDescription = "如果问题持续，请切换到 Shizuku 模式或重启设备后重试。"
                    )
                }
            }
        }
        ControlMode.SHIZUKU -> {
            if (shizukuStatus.canExecute()) {
                AppNavigation(navController)
            } else {
                ShizukuStatusScreen(
                    status = shizukuStatus,
                    shizukuManager = shizukuManager
                )
            }
        }
    }
}

@Composable
private fun AppNavigation(navController: androidx.navigation.NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "chat"
    ) {
        composable("chat") {
            ChatScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToHelp = { navController.navigate("help") }
            )
        }
        composable("settings") {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("history") {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("help") {
            HelpScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun StatusScreen(
    title: String,
    description: String,
    primaryButtonLabel: String?,
    onPrimaryClick: (() -> Unit)?,
    secondaryDescription: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AutoAI",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (primaryButtonLabel != null && onPrimaryClick != null) {
            Button(
                onClick = onPrimaryClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(primaryButtonLabel)
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (!secondaryDescription.isNullOrBlank()) {
            Text(
                text = secondaryDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShizukuStatusScreen(
    status: ShizukuStatus,
    shizukuManager: ShizukuManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Shizuku 权限未就绪",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        ShizukuStatusCard(status = status, shizukuManager = shizukuManager)
    }
}

@Composable
private fun ShizukuStatusCard(
    status: ShizukuStatus,
    shizukuManager: ShizukuManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "当前状态：${status.label}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = status.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (status == ShizukuStatus.PERMISSION_REQUIRED) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { shizukuManager.requestPermission() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("在 Shizuku 中授予权限")
                }
            }
        }
    }
}

private val ShizukuStatus.label: String
    get() = when (this) {
        ShizukuStatus.UNKNOWN -> "检测中"
        ShizukuStatus.NOT_INSTALLED -> "未安装"
        ShizukuStatus.NOT_RUNNING -> "未运行"
        ShizukuStatus.PERMISSION_REQUIRED -> "待授权"
        ShizukuStatus.AVAILABLE -> "已就绪"
        ShizukuStatus.ERROR -> "异常"
    }

private val ShizukuStatus.description: String
    get() = when (this) {
        ShizukuStatus.UNKNOWN -> "正在检测 Shizuku 服务状态，请稍候…"
        ShizukuStatus.NOT_INSTALLED -> "请先在设备上安装并激活 Shizuku，再返回此界面。"
        ShizukuStatus.NOT_RUNNING -> "Shizuku 服务未运行，请在 Shizuku 应用中启动服务。"
        ShizukuStatus.PERMISSION_REQUIRED -> "需要在 Shizuku 应用中授予 AutoAI 权限才能继续。"
        ShizukuStatus.AVAILABLE -> "Shizuku 权限已就绪，AutoAI 可以获得系统级控制能力。"
        ShizukuStatus.ERROR -> "连接 Shizuku 时出现异常，请尝试重启 Shizuku 或切换到无障碍模式。"
    }
