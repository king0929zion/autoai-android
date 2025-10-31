package com.autoai.android.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoai.android.data.model.Task
import com.autoai.android.data.model.TaskStatus
import com.autoai.android.task.TaskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 聊天界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val listState = rememberLazyListState()

    // 自动滚动到最新消息
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AI 自动控机")
                        if (isProcessing) {
                            Text(
                                text = "正在思考...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // 帮助按钮
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.Default.Info, "帮助")
                    }
                    // 历史记录按钮
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.List, "历史")
                    }
                    // 设置按钮
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 快捷任务栏
            QuickTaskBar(
                onTaskClick = { taskText ->
                    viewModel.updateInputText(taskText)
                }
            )
            
            // 消息列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(),
                        modifier = Modifier.animateItemPlacement()
                    ) {
                        MessageBubble(message)
                    }
                }
            }

            // 输入框区域
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = viewModel::updateInputText,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入任务，例如：打开微信") },
                        enabled = !isProcessing,
                        maxLines = 4,
                        shape = MaterialTheme.shapes.large,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.updateInputText("") }
                                ) {
                                    Icon(Icons.Default.Clear, "清空")
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 发送按钮带动画
                    FloatingActionButton(
                        onClick = { viewModel.sendMessage() },
                        modifier = Modifier.size(56.dp),
                        containerColor = if (inputText.isNotBlank() && !isProcessing) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        AnimatedContent(
                            targetState = isProcessing,
                            transitionSpec = {
                                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                            }
                        ) { processing ->
                            if (processing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Send, 
                                    "发送",
                                    tint = if (inputText.isNotBlank()) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 快捷任务栏
 */
@Composable
fun QuickTaskBar(
    onTaskClick: (String) -> Unit
) {
    val quickTasks = remember {
        listOf(
            QuickTask("📱 打开微信", "打开微信", Icons.Default.Phone),
            QuickTask("📧 打开邮箱", "打开邮箱", Icons.Default.Email),
            QuickTask("📷 截图", "截图并保存", Icons.Default.Face),
            QuickTask("🎵 播放音乐", "打开音乐播放器", Icons.Default.Star),
            QuickTask("🔍 搜索", "在浏览器搜索", Icons.Default.Search)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickTasks) { task ->
                ElevatedButton(
                    onClick = { onTaskClick(task.command) },
                    modifier = Modifier.height(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        task.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = task.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

data class QuickTask(
    val label: String,
    val command: String,
    val icon: ImageVector
)

/**
 * 消息气泡 - 增强版
 */
@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    
    // 使用渐变色作为背景
    val backgroundBrush = if (message.isUser) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )
        )
    } else {
        null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 16.dp else 4.dp,
                topEnd = if (message.isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (message.isUser) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            tonalElevation = if (message.isUser) 0.dp else 1.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(
                    if (backgroundBrush != null) {
                        Modifier.background(backgroundBrush, RoundedCornerShape(
                            topStart = if (message.isUser) 16.dp else 4.dp,
                            topEnd = if (message.isUser) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ))
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                
                // 显示任务状态
                if (message.task != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TaskStatusCard(message.task)
                }
                
                // 时间戳
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isUser) {
                        Color.White.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    }
                )
            }
        }
    }
}

/**
 * 格式化时间戳
 */
fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        diff < 86400_000 -> "${diff / 3600_000}小时前"
        else -> {
            val date = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            date.format(java.util.Date(timestamp))
        }
    }
}

/**
 * 任务状态卡片
 */
@Composable
fun TaskStatusCard(task: Task) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // 状态
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = when (task.status) {
                    TaskStatus.PENDING -> "等待中"
                    TaskStatus.RUNNING -> "执行中"
                    TaskStatus.PAUSED -> "已暂停"
                    TaskStatus.COMPLETED -> "已完成"
                    TaskStatus.FAILED -> "失败"
                    TaskStatus.CANCELLED -> "已取消"
                }
                
                Text(
                    text = "状态: $statusText",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (task.status) {
                        TaskStatus.RUNNING -> MaterialTheme.colorScheme.primary
                        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                        TaskStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            // 进度
            if (task.currentStep > 0) {
                Text(
                    text = "步骤: ${task.currentStep}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // 错误信息
            if (task.error != null) {
                Text(
                    text = "错误: ${task.error}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 聊天消息
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val task: Task? = null
)

/**
 * 聊天 ViewModel
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val taskManager: TaskManager
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    init {
        // 添加欢迎消息
        _messages.value = listOf(
            ChatMessage(
                id = "welcome",
                content = "你好！我是 AI 自动控机助手。\n\n请告诉我你想要完成的任务，例如：\n• 打开微信\n• 在淘宝搜索机械键盘\n• 截图保存\n\n💡 提示：复杂任务建议分步执行以提高成功率",
                isUser = false
            )
        )
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank() || _isProcessing.value) return

        // 添加用户消息
        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = text,
            isUser = true
        )
        _messages.value = _messages.value + userMessage
        _inputText.value = ""
        _isProcessing.value = true

        // 执行任务
        viewModelScope.launch {
            try {
                Timber.d("开始执行任务: $text")
                
                // 添加处理中消息
                val processingMessageId = "${System.currentTimeMillis()}_processing"
                val processingMessage = ChatMessage(
                    id = processingMessageId,
                    content = "🤖 正在分析您的请求...",
                    isUser = false
                )
                _messages.value = _messages.value + processingMessage
                
                // 执行任务
                val result = taskManager.executeTask(text) { task ->
                    // 更新任务进度
                    val progressContent = when (task.status) {
                        TaskStatus.RUNNING -> {
                            if (task.currentStep > 0) {
                                "⚡ 正在执行第 ${task.currentStep} 步..."
                            } else {
                                "🤖 AI 正在思考..."
                            }
                        }
                        else -> "🤖 正在处理..."
                    }
                    
                    val updatedMessages = _messages.value.dropLast(1) + processingMessage.copy(
                        content = progressContent,
                        task = task
                    )
                    _messages.value = updatedMessages
                }
                
                // 移除处理中消息
                _messages.value = _messages.value.dropLast(1)
                
                // 添加结果消息
                val resultMessage = if (result.isSuccess) {
                    val successContent = result.getOrNull() ?: "任务已完成"
                    ChatMessage(
                        id = "${System.currentTimeMillis()}_result",
                        content = "✅ 任务完成\n\n$successContent",
                        isUser = false,
                        task = taskManager.currentTask.value
                    )
                } else {
                    val error = result.exceptionOrNull()
                    val errorMsg = when {
                        error?.message?.contains("API", ignoreCase = true) == true -> 
                            "API 调用失败\n请检查网络连接和 API Key 配置"
                        error?.message?.contains("Shizuku", ignoreCase = true) == true -> 
                            "Shizuku 服务异常\n请确保 Shizuku 正在运行"
                        error?.message?.contains("Permission", ignoreCase = true) == true -> 
                            "权限不足\n请授予必要的权限"
                        error?.message?.contains("timeout", ignoreCase = true) == true ->
                            "操作超时\n请稍后重试或简化任务"
                        else -> error?.message ?: "未知错误"
                    }
                    
                    ChatMessage(
                        id = "${System.currentTimeMillis()}_error",
                        content = "❌ 任务失败\n\n$errorMsg\n\n💡 建议：尝试简化任务描述或分步执行",
                        isUser = false,
                        task = taskManager.currentTask.value
                    )
                }
                _messages.value = _messages.value + resultMessage
                
            } catch (e: Exception) {
                Timber.e(e, "执行任务异常")
                
                // 移除处理中的消息
                if (_messages.value.isNotEmpty() && !_messages.value.last().isUser) {
                    _messages.value = _messages.value.dropLast(1)
                }
                
                val errorMessage = ChatMessage(
                    id = "${System.currentTimeMillis()}_exception",
                    content = "⚠️ 发生异常\n\n${e.message}\n\n请查看日志或重启应用",
                    isUser = false
                )
                _messages.value = _messages.value + errorMessage
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * 清空消息历史
     */
    fun clearMessages() {
        _messages.value = listOf(
            ChatMessage(
                id = "welcome_${System.currentTimeMillis()}",
                content = "对话历史已清空，请告诉我新的任务需求！",
                isUser = false
            )
        )
    }
}
