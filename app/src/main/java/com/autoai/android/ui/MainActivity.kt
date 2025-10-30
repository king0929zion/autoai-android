package com.autoai.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.autoai.android.permission.ShizukuManager
import com.autoai.android.permission.ShizukuStatus
import com.autoai.android.ui.chat.ChatScreen
import com.autoai.android.ui.settings.SettingsScreen
import com.autoai.android.ui.history.HistoryScreen
import com.autoai.android.ui.help.HelpScreen
import com.autoai.android.ui.theme.AutoAITheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * 主活动
 * 应用的入口界面
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var shizukuManager: ShizukuManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity 创建")
        
        // 初始化 Shizuku
        shizukuManager.initialize()

        setContent {
            AutoAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(shizukuManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shizukuManager.cleanup()
        Timber.d("MainActivity 销毁")
    }
}

@Composable
fun AppNavigation(shizukuManager: ShizukuManager) {
    val navController = rememberNavController()
    val shizukuStatus by shizukuManager.shizukuStatus.collectAsState()

    // 如果 Shizuku 未就绪，显示状态页面
    if (shizukuStatus != ShizukuStatus.AVAILABLE) {
        ShizukuStatusScreen(shizukuManager)
    } else {
        // Shizuku 已就绪，显示主界面
        NavHost(
            navController = navController,
            startDestination = "chat"
        ) {
            composable("chat") {
                ChatScreen(
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToHistory = {
                        navController.navigate("history")
                    },
                    onNavigateToHelp = {
                        navController.navigate("help")
                    }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("history") {
                HistoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("help") {
                HelpScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun ShizukuStatusScreen(shizukuManager: ShizukuManager) {
    val shizukuStatus by shizukuManager.shizukuStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 应用标题
        Text(
            text = "AutoAI",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "AI 自主控机系统",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Shizuku 状态卡片
        ShizukuStatusCard(shizukuStatus, shizukuManager)

        Spacer(modifier = Modifier.height(24.dp))

        // 提示信息
        if (shizukuStatus == ShizukuStatus.AVAILABLE) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "✓ 系统就绪",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "现在可以开始使用 AI 自动控制功能",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ShizukuStatusCard(status: ShizukuStatus, shizukuManager: ShizukuManager) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Shizuku 状态",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态指示器
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (status) {
                        ShizukuStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
                        ShizukuStatus.NOT_INSTALLED, ShizukuStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.size(12.dp)
                ) {}

                Spacer(modifier = Modifier.width(12.dp))

                // 状态文本
                Text(
                    text = when (status) {
                        ShizukuStatus.UNKNOWN -> "检查中..."
                        ShizukuStatus.NOT_INSTALLED -> "Shizuku 未安装"
                        ShizukuStatus.NOT_RUNNING -> "Shizuku 未运行"
                        ShizukuStatus.PERMISSION_REQUIRED -> "需要授权"
                        ShizukuStatus.AVAILABLE -> "已就绪"
                        ShizukuStatus.ERROR -> "发生错误"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // 显示版本信息
            if (status == ShizukuStatus.AVAILABLE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "版本: ${shizukuManager.getShizukuVersion()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 操作按钮
            if (status == ShizukuStatus.PERMISSION_REQUIRED) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { shizukuManager.requestPermission() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("授予权限")
                }
            }

            // 说明文字
            if (status != ShizukuStatus.AVAILABLE) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when (status) {
                        ShizukuStatus.NOT_INSTALLED -> "请先安装 Shizuku 应用"
                        ShizukuStatus.NOT_RUNNING -> "请启动 Shizuku 服务"
                        ShizukuStatus.PERMISSION_REQUIRED -> "需要授予 Shizuku 权限才能使用自动控制功能"
                        ShizukuStatus.ERROR -> "请检查 Shizuku 是否正常运行"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
