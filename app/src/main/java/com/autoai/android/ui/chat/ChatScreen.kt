@file:OptIn(ExperimentalMaterial3Api::class)

package com.autoai.android.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoai.android.data.model.Action
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
    val currentTask by viewModel.currentTask.collectAsState()
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "AI 自动控机",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        AnimatedVisibility(
                            visible = isProcessing,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = "正在执行任务...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "帮助")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "历史记录")
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
            AnimatedVisibility(
                visible = isProcessing || currentTask?.status == TaskStatus.RUNNING,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                TaskStatusBanner(
                    task = currentTask,
                    isProcessing = isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        EmptyChatHint(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    items(items = messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isProcessing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

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
private fun ChatInputBar(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = enabled && text.isNotBlank()

    TextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        placeholder = { Text("请输入要执行的任务") },
        enabled = enabled,
        maxLines = 4,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (text.isNotBlank()) {
                    IconButton(onClick = { onTextChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                FilledIconButton(
                    onClick = onSend,
                    enabled = canSend,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "发送")
                }
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = {
            if (canSend) onSend()
        })
    )
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val bubbleShape = RoundedCornerShape(
        topStart = if (isUser) 16.dp else 4.dp,
        topEnd = if (isUser) 4.dp else 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )

    Column(
        modifier = modifier,
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = bubbleShape,
            tonalElevation = if (isUser) 4.dp else 0.dp,
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.animateContentSize()
        ) {
            val contentModifier = if (isUser) {
                Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        ),
                        shape = bubbleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            } else {
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            }

            Column(
                modifier = contentModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                message.task?.let {
                    TaskSnapshot(task = it)
                }
            }
        }

        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TaskStatusBanner(
    task: Task?,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    val statusText = when {
        isProcessing -> "正在执行任务"
        task?.status == TaskStatus.RUNNING -> "任务执行中"
        task?.status == TaskStatus.COMPLETED -> "任务已完成"
        task?.status == TaskStatus.FAILED -> "任务失败"
        task?.status == TaskStatus.PAUSED -> "任务已暂停"
        task?.status == TaskStatus.CANCELLED -> "任务已取消"
        task?.status == TaskStatus.PENDING -> "任务排队中"
        else -> "准备就绪"
    }

    val description = when {
        isProcessing -> "正在分析屏幕并规划下一步操作，请稍候。"
        task == null -> "随时告诉我新的指令，我会立即开始执行。"
        task.status == TaskStatus.RUNNING -> "当前步骤：${task.currentStep}，最新动作将实时展示在对话中。"
        task.status == TaskStatus.COMPLETED -> task.result ?: "任务已成功结束。"
        task.status == TaskStatus.FAILED -> task.error ?: "请检查日志或稍后再试。"
        task.status == TaskStatus.PAUSED -> "任务已暂停，可在历史记录中继续。"
        task.status == TaskStatus.CANCELLED -> "任务已取消，可重新发起新的指令。"
        task.status == TaskStatus.PENDING -> "任务已入队，等待执行。"
        else -> task.description
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun TaskSnapshot(
    task: Task,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "当前任务：${task.description}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "状态：${task.status.toReadableText()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        task.history.lastOrNull()?.let { history ->
            Text(
                text = "最新动作：${history.action.describe()} -> ${history.result.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyChatHint(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "准备就绪",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "告诉我你想完成的任务，例如“打开微信并发送消息”。\n我会自动分析屏幕并执行操作。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun TaskStatus.toReadableText(): String = when (this) {
    TaskStatus.PENDING -> "排队中"
    TaskStatus.RUNNING -> "执行中"
    TaskStatus.PAUSED -> "已暂停"
    TaskStatus.COMPLETED -> "已完成"
    TaskStatus.FAILED -> "已失败"
    TaskStatus.CANCELLED -> "已取消"
}

private fun Action.describe(): String = when (this) {
    is Action.Click -> {
        val target = if (elementDescription.isNotBlank()) " [$elementDescription]" else ""
        "点击(${x}, ${y})$target"
    }
    is Action.LongClick -> "长按(${x}, ${y}) ${durationMs}ms"
    is Action.Swipe -> "滑动(${fromX}, ${fromY}) -> (${toX}, ${toY})"
    is Action.Input -> {
        val preview = text.take(20) + if (text.length > 20) "…" else ""
        "输入 \"$preview\""
    }
    is Action.PressKey -> "模拟按键：${keyCode}"
    is Action.OpenApp -> "打开应用：${packageName}"
    is Action.Wait -> "等待 ${durationMs}ms"
    is Action.GoBack -> "返回上一级"
    is Action.Complete -> "完成：${message}"
    is Action.Error -> "错误：${message}"
    is Action.RequestUserHelp -> "需要人工介入：${reason}"
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return formatter.format(timestamp)
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val taskManager: TaskManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                content = """
                    你好，我是 AutoAI 的执行助手。

                    你可以告诉我需要完成的任务，例如：
                    • 打开微信并发送消息
                    • 在浏览器中搜索资料
                    • 截取当前屏幕并保存

                    提示：复杂目标可以拆成多个步骤，我会依次完成。
                """.trimIndent(),
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val currentTask: StateFlow<Task?> = taskManager.currentTask

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
            val processingId = "${System.currentTimeMillis()}_processing"
            _messages.update { it + ChatMessage(processingId, "正在执行，请稍等...", false) }

            try {
                val result = taskManager.executeTask(content) { task ->
                    _messages.update { list ->
                        list.map { message ->
                            if (message.id == processingId) message.copy(task = task) else message
                        }
                    }
                }

                _messages.update { list ->
                    list.filterNot { it.id == processingId }
                }

                val feedback = if (result.isSuccess) {
                    val detail = result.getOrNull().orEmpty().ifBlank { "任务已成功完成。" }
                    ChatMessage(
                        id = "${System.currentTimeMillis()}_result",
                        content = "✅ 任务完成\n\n${detail}",
                        isUser = false,
                        task = taskManager.currentTask.value
                    )
                } else {
                    val reason = result.exceptionOrNull()?.message?.ifBlank { "发生未知错误。" }
                        ?: "发生未知错误。"
                    ChatMessage(
                        id = "${System.currentTimeMillis()}_error",
                        content = "⚠️ 任务失败\n\n${reason}",
                        isUser = false,
                        task = taskManager.currentTask.value
                    )
                }

                _messages.update { it + feedback }
            } catch (error: Throwable) {
                Timber.e(error, "execute task failed")
                _messages.update { list ->
                    list.filterNot { it.id == processingId } + ChatMessage(
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
                content = "对话已重置，请继续告诉我新的目标，我会立即安排执行。",
                isUser = false
            )
        )
    }
}

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val task: Task? = null
)

