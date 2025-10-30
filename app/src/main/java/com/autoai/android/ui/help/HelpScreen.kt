package com.autoai.android.ui.help

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 帮助和教程页面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("快速开始", "常见问题", "功能介绍", "关于")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("帮助中心") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 标签栏
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // 内容区域
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith 
                    fadeOut() + slideOutVertically()
                }
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> QuickStartContent()
                    1 -> FAQContent()
                    2 -> FeaturesContent()
                    3 -> AboutContent()
                }
            }
        }
    }
}

/**
 * 快速开始内容
 */
@Composable
fun QuickStartContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "快速开始",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        
        items(getQuickStartSteps()) { step ->
            StepCard(step)
        }
    }
}

/**
 * 常见问题内容
 */
@Composable
fun FAQContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "常见问题",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        
        items(getFAQList()) { faq ->
            FAQCard(faq)
        }
    }
}

/**
 * 功能介绍内容
 */
@Composable
fun FeaturesContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "功能介绍",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        
        items(getFeatureList()) { feature ->
            FeatureCard(feature)
        }
    }
}

/**
 * 关于内容
 */
@Composable
fun AboutContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "AutoAI Android",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "版本 0.1.0-alpha",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow("开发者", "AI Assistant")
                Divider()
                InfoRow("技术栈", "Kotlin + Jetpack Compose")
                Divider()
                InfoRow("AI 模型", "Qwen2-VL-7B-Instruct")
                Divider()
                InfoRow("权限框架", "Shizuku")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "© 2025 AutoAI Project",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 步骤卡片
 */
@Composable
fun StepCard(step: HelpStep) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = step.number.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * FAQ 卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQCard(faq: FAQ) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = faq.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 功能卡片
 */
@Composable
fun FeatureCard(feature: Feature) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                feature.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 信息行
 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// 数据类
data class HelpStep(
    val number: Int,
    val title: String,
    val description: String
)

data class FAQ(
    val question: String,
    val answer: String
)

data class Feature(
    val icon: ImageVector,
    val title: String,
    val description: String
)

// 辅助函数
fun getQuickStartSteps() = listOf(
    HelpStep(
        1,
        "安装 Shizuku",
        "从 GitHub 或应用商店下载并安装 Shizuku 应用"
    ),
    HelpStep(
        2,
        "激活 Shizuku",
        "Android 11+ 可使用无线调试方式，其他版本使用 ADB 命令激活"
    ),
    HelpStep(
        3,
        "配置 API",
        "进入设置页面，填写硅基流动 API Key 和相关配置"
    ),
    HelpStep(
        4,
        "开始使用",
        "在聊天界面输入任务描述，AI 将自动执行"
    )
)

fun getFAQList() = listOf(
    FAQ(
        "为什么需要 Shizuku？",
        "Shizuku 提供系统级权限，使应用能够模拟真实的人类操作。相比 root 权限，Shizuku 更安全且易于使用。"
    ),
    FAQ(
        "支付操作安全吗？",
        "系统会自动检测支付场景并暂停执行，AI 永远不会自动完成支付操作，确保资金安全。"
    ),
    FAQ(
        "API 费用如何？",
        "使用硅基流动 API，费用约 ¥0.002/千 tokens，日常使用每月约 10 元左右。"
    ),
    FAQ(
        "支持哪些应用？",
        "理论上支持所有 Android 应用，使用标准 UI 组件的应用效果最好。"
    ),
    FAQ(
        "如何提高任务成功率？",
        "1. 使用清晰简洁的任务描述\n2. 将复杂任务分解为多个简单步骤\n3. 确保 Shizuku 正常运行\n4. 保持网络连接稳定"
    )
)

fun getFeatureList() = listOf(
    Feature(
        Icons.Default.Star,
        "智能任务规划",
        "AI 自动分析任务需求，生成详细的执行步骤"
    ),
    Feature(
        Icons.Default.Face,
        "多模态感知",
        "结合视觉识别和控件树分析，准确理解屏幕状态"
    ),
    Feature(
        Icons.Default.Lock,
        "安全保护",
        "三级权限体系，自动识别敏感操作并保护隐私"
    ),
    Feature(
        Icons.Default.Build,
        "智能重试",
        "执行失败时自动分析原因并尝试修正"
    ),
    Feature(
        Icons.Default.Info,
        "实时反馈",
        "任务执行过程透明，实时显示进度和状态"
    ),
    Feature(
        Icons.Default.DateRange,
        "历史记录",
        "保存任务历史，方便回顾和统计分析"
    )
)
