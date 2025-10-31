package com.autoai.android.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator as MaterialCircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.autoai.android.data.model.Task
import com.autoai.android.data.model.TaskStatus
import com.autoai.android.task.TaskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import timber.log.Timber
import javax.inject.Inject

private const val QUICK_ACTION_OPEN_WECHAT = "\u6253\u5f00\u5fae\u4fe1"
private const val QUICK_ACTION_OPEN_SETTINGS = "\u6253\u5f00\u7cfb\u7edf\u8bbe\u7f6e"
private const val QUICK_ACTION_SCREENSHOT = "\u622a\u56fe\u5e76\u4fdd\u5b58"
private const val QUICK_ACTION_PLAY_MUSIC = "\u64ad\u653e\u97f3\u4e50"
private const val QUICK_ACTION_WEB_SEARCH = "\u5728\u6d4f\u89c8\u5668\u641c\u7d22"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
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
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "\u00A0AI \u81ea\u52a8\u63a7\u673a",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (isProcessing) {
                            Text(
                                text = "\u6b63\u5728\u601d\u8003...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "\u5e2e\u52a9")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "\u5386\u53f2")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "\u8bbe\u7f6e")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                onActionSelected = { action ->
                    viewModel.updateInputText(action)
                    coroutineScope.launch {
                        listState.animateScrollToItem(messages.lastIndex.coerceAtLeast(0))
                    }
                }
            )

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    MessageBubble(message = message)
                }
            }

            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
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
                        placeholder = { Text(text = "\u8bf7\u8f93\u5165\u8981\u6267\u884c\u7684\u4efb\u52a1") },
                        enabled = !isProcessing,
                        maxLines = 4,
                        shape = MaterialTheme.shapes.large,
                        trailingIcon = {
                            if (inputText.isNotBlank()) {
                                IconButton(onClick = { viewModel.updateInputText("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "\u6e05\u7a7a")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        color = if (inputText.isNotBlank() && !isProcessing) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (inputText.isNotBlank() && !isProcessing) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        shadowElevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        ) {
                            AnimatedContent(
                                targetState = isProcessing,
                                modifier = Modifier.align(Alignment.Center),
                                transitionSpec = {
                                    (fadeIn() + expandIn()) with (fadeOut() + shrinkOut())
                                },
                                label = "send_indicator"
                            ) { processing ->
                                if (processing) {
                                    MaterialCircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = { viewModel.sendMessage() },
                                        enabled = inputText.isNotBlank()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "\u53d1\u9001"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    modifier: Modifier = Modifier,
    onActionSelected: (String) -> Unit
) {
    val actions = remember {
        listOf(
            QuickAction(
                label = "\u6253\u5f00\u5fae\u4fe1",
                command = QUICK_ACTION_OPEN_WECHAT
            ),
            QuickAction(
                label = "\u7cfb\u7edf\u8bbe\u7f6e",
                command = QUICK_ACTION_OPEN_SETTINGS
            ),
            QuickAction(
                label = "\u622a\u56fe\u4fdd\u5b58",
                command = QUICK_ACTION_SCREENSHOT
            ),
            QuickAction(
                label = "\u64ad\u653e\u97f3\u4e50",
                command = QUICK_ACTION_PLAY_MUSIC
            ),
            QuickAction(
                label = "\u7f51\u9875\u641c\u7d22",
                command = QUICK_ACTION_WEB_SEARCH
            )
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            AssistChip(
                onClick = { onActionSelected(action.command) },
                label = { Text(action.label) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleShape = RoundedCornerShape(
        topStart = if (message.isUser) 16.dp else 4.dp,
        topEnd = if (message.isUser) 4.dp else 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        val backgroundBrush = if (message.isUser) {
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                )
            )
        } else null

        Surface(
            shape = bubbleShape,
            color = if (backgroundBrush == null) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color.Transparent
            },
            tonalElevation = if (message.isUser) 4.dp else 0.dp,
            shadowElevation = if (message.isUser) 4.dp else 0.dp,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .animateItemPlacement(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .background(backgroundBrush ?: Color.Transparent, bubbleShape)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (message.task != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TaskStatusCard(task = message.task)
                    }
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
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val statusText = when (task.status) {
                TaskStatus.PENDING -> "\u5df2\u6536\u5230\u4efb\u52a1"
                TaskStatus.RUNNING -> "\u4efb\u52a1\u6267\u884c\u4e2d"
                TaskStatus.PAUSED -> "\u4efb\u52a1\u5df2\u6682\u505c"
                TaskStatus.COMPLETED -> "\u5df2\u6210\u529f"
                TaskStatus.FAILED -> "\u6267\u884c\u5931\u8d25"
                TaskStatus.CANCELLED -> "\u5df2\u53d6\u6d88"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = when (task.status) {
                    TaskStatus.RUNNING -> MaterialTheme.colorScheme.primary
                    TaskStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
                    TaskStatus.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            if (task.currentStep > 0) {
                Text(
                    text = "\u5f53\u524d\u7b2c ${task.currentStep} \u6b65",
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
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "\u521a\u521a"
        diff < 3_600_000 -> "${diff / 60_000}\u5206\u949f\u524d"
        diff < 86_400_000 -> "${diff / 3_600_000}\u5c0f\u65f6\u524d"
        else -> {
            val date = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            date.format(java.util.Date(timestamp))
        }
    }
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

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                content = "\u4f60\u597d\uff01\u6211\u662f AI \u81ea\u52a8\u63a7\u673a\u52a9\u624b\u3002\n\n\u8bf7\u544a\u8bc9\u6211\u9700\u8981\u5e2e\u4f60\u5b8c\u6210\u7684\u4efb\u52a1\uff0c\u4f8b\u5982\uff1a\n\u2022 \u6253\u5f00\u5fae\u4fe1\n\u2022 \u5728\u5f00\u6e90\u6d4f\u89c8\u5668\u641c\u7d22\n\u2022 \u622a\u56fe\u4fdd\u5b58\n\n\u63d0\u793a\uff1a\u590d\u6742\u4efb\u52a1\u53ef\u4ee5\u5206\u6b65\u63d0\u4ea4\uff0c\u6210\u529f\u7387\u66f4\u9ad8\u3002",
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
        val trimmed = _inputText.value.trim()
        if (trimmed.isEmpty() || _isProcessing.value) return

        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = trimmed,
            isUser = true
        )
        _messages.update { it + userMessage }
        _inputText.value = ""
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                val processingMessageId = "${System.currentTimeMillis()}_processing"
                val processingMessage = ChatMessage(
                    id = processingMessageId,
                    content = "\u6b63\u5728\u5904\u7406\uff0c\u8bf7\u7a0d\u5019...",
                    isUser = false
                )
                _messages.update { it + processingMessage }

                val result = taskManager.executeTask(trimmed) { task ->
                    _messages.update { current ->
                        val updated = current.toMutableList()
                        val index = updated.indexOfFirst { it.id == processingMessageId }
                        if (index != -1) {
                            updated[index] = updated[index].copy(
                                content = when (task.status) {
                                    TaskStatus.RUNNING -> "\u4efb\u52a1\u6267\u884c\u4e2d..."
                                    TaskStatus.COMPLETED -> "\u4efb\u52a1\u5df2\u5b8c\u6210\uff0c\u6b63\u5728\u6536\u96c6\u7ed3\u679c..."
                                    TaskStatus.FAILED -> "\u4efb\u52a1\u51fa\u73b0\u95ee\u9898\uff0c\u6b63\u5728\u5904\u7406..."
                                    TaskStatus.PAUSED -> "\u4efb\u52a1\u5df2\u6682\u505c\uff0c\u7b49\u5f85\u5904\u7406..."
                                    TaskStatus.CANCELLED -> "\u4efb\u52a1\u5df2\u53d6\u6d88..."
                                    TaskStatus.PENDING -> "\u4efb\u52a1\u5f85\u5904\u7406..."
                                },
                                task = task
                            )
                        }
                        updated
                    }
                }

                _messages.update { current ->
                    current.filterNot { it.id == processingMessageId }
                }

                val resultMessage = if (result.isSuccess) {
                    val summary = result.getOrNull().orEmpty().ifBlank { "\u4efb\u52a1\u5df2\u6210\u529f\u6267\u884c" }
                    ChatMessage(
                        id = "${System.currentTimeMillis()}_result",
                        content = "\u2714\ufe0f \u4efb\u52a1\u5b8c\u6210\n\n$summary",
                        isUser = false,
                        task = taskManager.currentTask.value
                    )
                } else {
                    val errorText = result.exceptionOrNull()?.message.orEmpty().ifBlank { "\u672a\u77e5\u9519\u8bef" }
                    ChatMessage(
                        id = "${System.currentTimeMillis()}_error",
                        content = "\u274c \u4efb\u52a1\u5931\u8d25\n\n$errorText\n\n\u63d0\u793a\uff1a\u53ef\u4ee5\u8003\u8651\u5206\u6b65\u63d0\u4ea4\u4efb\u52a1\u6216\u7b80\u5316\u63cf\u8ff0\u3002",
                        isUser = false,
                        task = taskManager.currentTask.value
                    )
                }

                _messages.update { it + resultMessage }
            } catch (th: Throwable) {
                Timber.e(th, "Failed to execute task")
                _messages.update { current ->
                    val cleaned = current.dropLastWhile { it.id.endsWith("_processing") }
                    cleaned + ChatMessage(
                        id = "${System.currentTimeMillis()}_exception",
                        content = "\u26a0\ufe0f \u53d1\u751f\u5f02\u5e38\n\n${th.message ?: "Unexpected error"}",
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
                content = "\u804a\u5929\u5386\u53f2\u5df2\u6e05\u7a7a\uff0c\u544a\u8bc9\u6211\u65b0\u7684\u4efb\u52a1\u5427\uff01",
                isUser = false
            )
        )
    }
}
