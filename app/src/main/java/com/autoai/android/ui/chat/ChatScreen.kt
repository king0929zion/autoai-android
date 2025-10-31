@file:OptIn(ExperimentalMaterial3Api::class)

package com.autoai.android.ui.chat

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoai.android.R
import com.autoai.android.data.model.Task
import com.autoai.android.data.model.TaskStatus
import com.autoai.android.task.TaskManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToHelp: () -> Unit,
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

    val title = stringResource(R.string.chat_toolbar_title)
    val subtitleReady = stringResource(R.string.chat_toolbar_subtitle_ready)
    val subtitleRunning = stringResource(R.string.chat_toolbar_subtitle_running)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isProcessing) subtitleRunning else subtitleReady,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = stringResource(R.string.chat_action_history))
                    }
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(Icons.Default.HelpOutline, contentDescription = stringResource(R.string.chat_action_help))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            if (isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }

            InputBar(
                text = inputText,
                onTextChange = viewModel::updateInputText,
                onSend = viewModel::sendMessage,
                onClear = viewModel::clearMessages,
                enabled = !isProcessing
            )

            if (currentTask != null) {
                TaskSummary(task = currentTask!!)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Arrangement.End else Arrangement.Start
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
                val task = message.task
                if (task != null && !message.isUser) {
                    Text(
                        text = stringResource(R.string.chat_summary_status, stringResource(task.status.toLabelRes())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit,
    enabled: Boolean
) {
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.chat_placeholder_input)) },
                singleLine = false,
                maxLines = 3,
                enabled = enabled
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                FilledIconButton(
                    onClick = onSend,
                    enabled = enabled && text.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = stringResource(R.string.chat_action_send))
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.chat_action_clear))
                }
            }
        }
    }
}

@Composable
private fun TaskSummary(task: Task) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.chat_summary_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.chat_summary_status, stringResource(task.status.toLabelRes())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val task: Task? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val taskManager: TaskManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome",
                content = appContext.getString(R.string.chat_message_welcome),
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
            val processingId = "{System.currentTimeMillis()}_processing"
            _messages.update { it + ChatMessage(processingId, appContext.getString(R.string.chat_message_processing), false) }

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
                    val detail = result.getOrNull().orEmpty().ifBlank { appContext.getString(R.string.chat_message_default_success) }
                    ChatMessage(
                        id = "{System.currentTimeMillis()}_success",
                        content = appContext.getString(R.string.chat_result_success, detail),
                        isUser = false,
                        task = taskManager.currentTask.value
                    )
                } else {
                    val reason = result.exceptionOrNull()?.message?.ifBlank { appContext.getString(R.string.chat_message_unknown_reason) }
                        ?: appContext.getString(R.string.chat_message_unknown_reason)
                    ChatMessage(
                        id = "{System.currentTimeMillis()}_error",
                        content = appContext.getString(R.string.chat_result_failure, reason),
                        isUser = false,
                        task = taskManager.currentTask.value
                    )
                }

                _messages.update { it + feedback }
            } catch (error: Throwable) {
                Timber.e(error, "execute task failed")
                val message = error.message?.ifBlank { appContext.getString(R.string.chat_message_unknown_error_hint) }
                    ?: appContext.getString(R.string.chat_message_unknown_error_hint)
                _messages.update { list ->
                    list.filterNot { it.id == processingId } + ChatMessage(
                        id = "{System.currentTimeMillis()}_exception",
                        content = appContext.getString(R.string.chat_result_exception, message),
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
                id = "{System.currentTimeMillis()}_welcome",
                content = appContext.getString(R.string.chat_message_clear),
                isUser = false
            )
        )
    }
}

@StringRes
private fun TaskStatus.toLabelRes(): Int = when (this) {
    TaskStatus.PENDING -> R.string.task_pending
    TaskStatus.RUNNING -> R.string.task_executing
    TaskStatus.PAUSED -> R.string.task_paused
    TaskStatus.CANCELLED -> R.string.task_terminated
    TaskStatus.COMPLETED -> R.string.task_completed
    TaskStatus.FAILED -> R.string.task_failed
}
