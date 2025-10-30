package com.autoai.android.ui.history

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoai.android.data.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 任务历史页面
 * 显示已完成和失败的任务记录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val taskHistory by viewModel.taskHistory.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val filterType by viewModel.filterType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务历史") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: 清空历史 */ }) {
                        Icon(Icons.Default.Delete, "清空")
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
            // 统计卡片
            StatisticsCard(statistics)
            
            // 过滤器
            FilterChips(
                currentFilter = filterType,
                onFilterChange = viewModel::updateFilter
            )
            
            // 历史记录列表
            if (taskHistory.isEmpty()) {
                EmptyHistoryView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(taskHistory) { task ->
                        HistoryTaskCard(task) {
                            // TODO: 点击查看详情
                        }
                    }
                }
            }
        }
    }
}

/**
 * 统计卡片
 */
@Composable
fun StatisticsCard(statistics: TaskStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "总任务",
                value = statistics.totalTasks.toString(),
                icon = Icons.Default.List,
                color = MaterialTheme.colorScheme.primary
            )
            
            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
            )
            
            StatItem(
                label = "成功",
                value = statistics.successTasks.toString(),
                icon = Icons.Default.CheckCircle,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp)
            )
            
            StatItem(
                label = "失败",
                value = statistics.failedTasks.toString(),
                icon = Icons.Default.Close,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * 统计项
 */
@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 过滤器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    currentFilter: FilterType,
    onFilterChange: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterType.values().forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.label) },
                leadingIcon = if (currentFilter == filter) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

/**
 * 历史任务卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTaskCard(
    task: Task,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态图标
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = when (task.status) {
                    Task.TaskStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                    Task.TaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                    Task.TaskStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (task.status) {
                            Task.TaskStatus.COMPLETED -> Icons.Default.CheckCircle
                            Task.TaskStatus.FAILED -> Icons.Default.Close
                            Task.TaskStatus.CANCELLED -> Icons.Default.Delete
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (task.status) {
                            Task.TaskStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
                            Task.TaskStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 任务信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTaskDate(task.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.currentStep > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${task.currentStep} 步",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 箭头
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 空状态视图
 */
@Composable
fun EmptyHistoryView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无历史记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "完成的任务将显示在这里",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 格式化任务日期
 */
fun formatTaskDate(timestamp: Long): String {
    val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return date.format(java.util.Date(timestamp))
}

/**
 * 过滤类型
 */
enum class FilterType(val label: String) {
    ALL("全部"),
    COMPLETED("成功"),
    FAILED("失败"),
    CANCELLED("已取消")
}

/**
 * 任务统计
 */
data class TaskStatistics(
    val totalTasks: Int = 0,
    val successTasks: Int = 0,
    val failedTasks: Int = 0,
    val cancelledTasks: Int = 0
)

/**
 * 历史 ViewModel
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    // TODO: 注入 TaskRepository
) : ViewModel() {
    
    private val _taskHistory = MutableStateFlow<List<Task>>(emptyList())
    val taskHistory: StateFlow<List<Task>> = _taskHistory
    
    private val _statistics = MutableStateFlow(TaskStatistics())
    val statistics: StateFlow<TaskStatistics> = _statistics
    
    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType
    
    init {
        loadHistory()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            // TODO: 从数据库加载历史记录
            // 模拟数据
            _statistics.value = TaskStatistics(
                totalTasks = 0,
                successTasks = 0,
                failedTasks = 0
            )
        }
    }
    
    fun updateFilter(type: FilterType) {
        _filterType.value = type
        // TODO: 根据过滤类型更新列表
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            // TODO: 清空历史记录
        }
    }
}
