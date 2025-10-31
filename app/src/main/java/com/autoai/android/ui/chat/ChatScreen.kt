package com.autoai.android.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoai.android.data.model.Task
import com.autoai.android.data.model.TaskStatus
import com.autoai.android.task.TaskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI 自动控机",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "帮助")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "历史")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
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
            QuickActionRow(
                actions = viewModel.quickActions,
                onActionSelected = { viewModel.updateInputText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            ChatInputBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                text = inputText,
                enabled = !isProcessing,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage
            )
        }
    }
}

@Composable
private fun QuickActionRow(
    actions: List<QuickAction>,
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(actions) { action ->
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onActionSelected(action.command) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = enabled && text.isNotBlank()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("请输入要执行的任务") },
            enabled = enabled,
            maxLines = 4,
            trailingIcon = {
                if (text.isNotBlank()) {
                    IconButton(onClick = { onTextChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Button(
            onClick = onSend,
            enabled = canSend,
            shape = CircleShape,
            modifier = Modifier.height(56.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = "发送")
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = if (enabled) "发送" else "执行中")
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleShape = RoundedCornerShape(
        topStart = if (isUser) 16.dp else 4.dp,
        topEnd = if (isUser) 4.dp else 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = if (isUser) 4.dp else 0.dp
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (isUser) Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        ) else Color.Transparent,
                        bubbleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    message.task?.let { TaskStatusCard(it) }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TaskStatusCard(task: Task) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val statusText = when (task.status) {
                TaskStatus.PENDING -> "已接收任务"
                TaskStatus.RUNNING -> "任务执行中"
                TaskStatus.PAUSED -> "任务已暂停"
                TaskStatus.COMPLETED -> "任务完成"
                TaskStatus.FAILED -> "执行失败"
                TaskStatus.CANCELLED -> "任务取消"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (task.currentStep > 0) {
                Text(
                    text = "当前第 ${task.currentStep} 步",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            task.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return formatter.format(timestamp)
}

data class QuickAction(
    val label: String,
    val command: String
)

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val task: Task? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val taskManager: TaskManager
) : ViewModel() {

    val quickActions = listOf(
        QuickAction("打开微信", "打开微信"),
        QuickAction("打开系统设置", "打开系统设置"),
        QuickAction("截图并保存", "截图并保存"),
        QuickAction("播放音乐", "播放音乐"),
        QuickAction("在浏览器搜索", "在浏览器搜索")
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                content = "你好！我是 AI 自动控机助手。\n\n可以告诉我需要完成的任务，例如：\n• 打开微信\n• 在浏览器搜索内容\n• 截图并保存\n\n提示：复杂任务建议拆分为多个步骤。",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val content = _inputText.value.trim()
        if (content.isEmpty() || _isProcessing.value) return

        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = content,
            isUser = true
        )
        _messages.update { it + userMessage }
        _inputText.value = ""
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                val processingId = "${System.currentTimeMillis()}_processing"
                _messages.update { it + ChatMessage(processingId, "正在执行，请稍候...", false) }

                val result = taskManager.executeTask(content) { task ->
                    _messages.update { list ->
                        list.map { message ->
                            if (message.id == processingId) message.copy(task = task) else message
                        }
                    }
                }

                _messages.update { it.filterNot { message -> message.id == processingId } }

                val feedback = if (result.isSuccess) {
                    val detail = result.getOrNull().orEmpty().ifBlank { "任务已成功完成" }
                    ChatMessage(
                        id = "${System.currentTimeMillis()}_result",
                        content = "✅ 任务完成\n\n$detail",
                        isUser = false,
                        task = taskManager.currentTask.value
                    )
                } else {
                    val reason = result.exceptionOrNull()?.message ?: "发生未知错误"
                    ChatMessage(
                        id = "${System.currentTimeMillis()}_error",
                        content = "❌ 任务失败\n\n$reason",
                        isUser = false,
                        task = taskManager.currentTask.value
                    )
                }
                _messages.update { it + feedback }
            } catch (error: Throwable) {
                Timber.e(error, "execute task failed")
                _messages.update { list ->
                    val cleaned = list.dropLastWhile { it.content.contains("正在执行") }
                    cleaned + ChatMessage(
                        id = "${System.currentTimeMillis()}_exception",
                        content = "⚠️ 发生异常\n\n${error.message ?: "请查看日志"}",
                        isUser = false
                    )
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearMessages() {
        _messages.value = listOf(
            ChatMessage(
                id = "welcome_${System.currentTimeMillis()}",
                content = "对话已重置，可以继续告诉我新的任务。",
                isUser = false
            )
        )
    }
}
