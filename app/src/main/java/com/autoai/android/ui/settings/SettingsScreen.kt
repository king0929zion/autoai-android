@file:OptIn(ExperimentalMaterial3Api::class)

package com.autoai.android.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.autoai.android.accessibility.AccessibilityBridge
import com.autoai.android.accessibility.AccessibilityStatus
import com.autoai.android.decision.VLMClient
import com.autoai.android.permission.ControlMode
import com.autoai.android.permission.OperationExecutor
import com.autoai.android.permission.ShizukuManager
import com.autoai.android.permission.ShizukuStatus
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val modelName by viewModel.modelName.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val apiTestState by viewModel.apiTestState.collectAsState()
    val controlMode by viewModel.controlMode.collectAsState()
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val accessibilityStatus by viewModel.accessibilityStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            scope.launch {
                snackbarHostState.showSnackbar(errorMessage!!)
                viewModel.clearError()
            }
        }
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            scope.launch { snackbarHostState.showSnackbar("配置已保存") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ControlModeSection(
                controlMode = controlMode,
                shizukuStatus = shizukuStatus,
                accessibilityStatus = accessibilityStatus,
                onModeSelected = { mode -> viewModel.switchControlMode(mode) },
                onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                onOpenShizuku = { openShizukuApp(context) }
            )

            ApiSettingsSection(
                apiKey = apiKey,
                onApiKeyChange = viewModel::updateApiKey,
                baseUrl = baseUrl,
                onBaseUrlChange = viewModel::updateBaseUrl,
                modelName = modelName,
                onModelNameChange = viewModel::updateModelName,
                temperature = temperature,
                onTemperatureChange = viewModel::updateTemperature,
                maxTokens = maxTokens,
                onMaxTokensChange = viewModel::updateMaxTokens,
                isDarkTheme = isDarkTheme,
                onDarkThemeChange = viewModel::updateDarkTheme,
                apiTestState = apiTestState,
                onTestConnection = viewModel::testApiConnection,
                onSave = viewModel::saveSettings
            )
        }
    }
}

@Composable
private fun ControlModeSection(
    controlMode: ControlMode,
    shizukuStatus: ShizukuStatus,
    accessibilityStatus: AccessibilityStatus,
    onModeSelected: (ControlMode) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenShizuku: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "控制模式",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "可在无障碍模式和 Shizuku 模式之间切换。默认推荐无障碍模式，更易启用且无需额外应用。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow {
                androidx.compose.material3.SegmentedButton(
                    selected = controlMode.isAccessibility(),
                    onClick = { onModeSelected(ControlMode.ACCESSIBILITY) },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { Icon(Icons.Default.AccessibilityNew, contentDescription = null) },
                    label = { Text("无障碍模式") }
                )
                androidx.compose.material3.SegmentedButton(
                    selected = controlMode.isShizuku(),
                    onClick = { onModeSelected(ControlMode.SHIZUKU) },
                    shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(1, 2),
                    icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    label = { Text("Shizuku 模式") }
                )
            }

            val (statusLabel, statusDescription, actionLabel, action) = when (controlMode) {
                ControlMode.ACCESSIBILITY -> {
                    val label = when {
                        accessibilityStatus.isReady() -> "已就绪"
                        accessibilityStatus == AccessibilityStatus.SERVICE_DISABLED -> "未启用"
                        accessibilityStatus == AccessibilityStatus.CONNECTING -> "连接中"
                        else -> "异常"
                    }
                    val description = when (accessibilityStatus) {
                        AccessibilityStatus.SERVICE_DISABLED ->
                            "请在系统设置 > 辅助功能 > 已下载的服务中启用 AutoAI。"
                        AccessibilityStatus.CONNECTING ->
                            "正在等待系统完成无障碍服务绑定…"
                        AccessibilityStatus.READY ->
                            "无障碍服务已连接，可直接下发操作指令。"
                        AccessibilityStatus.ERROR ->
                            "无障碍服务异常，请重新启用或重启设备后再试。"
                    }
                    val actionText = if (accessibilityStatus.isReady()) null else "打开无障碍设置"
                    Quad(label, description, actionText, onOpenAccessibilitySettings)
                }

                ControlMode.SHIZUKU -> {
                    val label = shizukuStatus.label
                    val description = shizukuStatus.description
                    val actionText = if (shizukuStatus == ShizukuStatus.PERMISSION_REQUIRED) "打开 Shizuku 授权" else "启动 Shizuku 应用"
                    Quad(label, description, actionText, onOpenShizuku)
                }
            }

            StatusBlock(
                statusLabel = statusLabel,
                statusDescription = statusDescription,
                actionLabel = actionLabel,
                onAction = action
            )
        }
    }
}

@Composable
private fun StatusBlock(
    statusLabel: String,
    statusDescription: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = statusDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!actionLabel.isNullOrBlank()) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun ApiSettingsSection(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    modelName: String,
    onModelNameChange: (String) -> Unit,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    maxTokens: Int,
    onMaxTokensChange: (Int) -> Unit,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    apiTestState: ApiTestState,
    onTestConnection: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "API 配置",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text("API 基础 URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = modelName,
                onValueChange = onModelNameChange,
                label = { Text("模型名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "高级参数",
                    style = MaterialTheme.typography.titleSmall
                )
                TemperatureSlider(
                    temperature = temperature,
                    onTemperatureChange = onTemperatureChange
                )
                MaxTokenSelector(
                    maxTokens = maxTokens,
                    onMaxTokensChange = onMaxTokensChange
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "深色主题", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onDarkThemeChange
                    )
                }
            }
        }

        ConnectionTestBlock(
            apiTestState = apiTestState,
            onTestConnection = onTestConnection
        )

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CloudDone, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("保存配置")
        }
        }
    }
}

@Composable
private fun TemperatureSlider(
    temperature: Float,
    onTemperatureChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Temperature：${String.format("%.2f", temperature)}", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "较低数值更稳重，较高数值更富创造力。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.Slider(
            value = temperature,
            onValueChange = onTemperatureChange,
            valueRange = 0f..1f,
            steps = 19
        )
    }
}

@Composable
private fun MaxTokenSelector(
    maxTokens: Int,
    onMaxTokensChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("最大 Tokens：$maxTokens", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "控制单次回答的长度，过高可能导致调用耗时增加。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.Slider(
            value = maxTokens.toFloat(),
            onValueChange = { onMaxTokensChange(it.toInt()) },
            valueRange = 128f..4096f,
            steps = 30
        )
    }
}

@Composable
private fun ConnectionTestBlock(
    apiTestState: ApiTestState,
    onTestConnection: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "API 连接测试", style = MaterialTheme.typography.titleSmall)
            }

            Text(
                text = "测试当前配置是否可用，会发起一次轻量的诊断请求。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onTestConnection) {
                Text("开始测试")
            }

            AnimatedVisibility(visible = apiTestState !is ApiTestState.Idle) {
                when (apiTestState) {
                    ApiTestState.Idle -> {}
                    ApiTestState.Loading -> Text("测试进行中…", style = MaterialTheme.typography.bodySmall)
                    is ApiTestState.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("延迟：${apiTestState.latencyMs} ms", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "响应预览：${apiTestState.responsePreview}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is ApiTestState.Failure -> {
                        Text(
                            text = "测试失败：${apiTestState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun openShizukuApp(context: Context) {
    val packageName = "moe.shizuku.privileged.api"
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "无法打开 Shizuku 应用或应用商店")
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val vlmClient: VLMClient,
    private val operationExecutor: OperationExecutor,
    private val shizukuManager: ShizukuManager,
    private val accessibilityBridge: AccessibilityBridge
) : ViewModel() {

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _baseUrl = MutableStateFlow(DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _modelName = MutableStateFlow(DEFAULT_MODEL)
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _temperature = MutableStateFlow(0.4f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _maxTokens = MutableStateFlow(1024)
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _apiTestState = MutableStateFlow<ApiTestState>(ApiTestState.Idle)
    val apiTestState: StateFlow<ApiTestState> = _apiTestState.asStateFlow()

    private val _controlMode = MutableStateFlow(ControlMode.ACCESSIBILITY)
    val controlMode: StateFlow<ControlMode> = _controlMode.asStateFlow()

    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuManager.shizukuStatus
    val accessibilityStatus: StateFlow<AccessibilityStatus> = accessibilityBridge.status

    init {
        viewModelScope.launch {
            operationExecutor.controlModeFlow.collectLatest { mode ->
                _controlMode.value = mode
            }
        }

        viewModelScope.launch {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .collect { preferences ->
                    _apiKey.value = preferences[API_KEY] ?: ""
                    _baseUrl.value = preferences[BASE_URL] ?: DEFAULT_BASE_URL
                    _modelName.value = preferences[MODEL_NAME] ?: DEFAULT_MODEL
                }
        }
    }

    fun updateApiKey(value: String) {
        _apiKey.value = value
        markDirty()
    }

    fun updateBaseUrl(value: String) {
        _baseUrl.value = value
        markDirty()
    }

    fun updateModelName(value: String) {
        _modelName.value = value
        markDirty()
    }

    fun updateTemperature(value: Float) {
        _temperature.value = value
        markDirty(resetTest = true)
    }

    fun updateMaxTokens(value: Int) {
        _maxTokens.value = value.coerceIn(128, 4096)
        markDirty(resetTest = true)
    }

    fun updateDarkTheme(value: Boolean) {
        _isDarkTheme.value = value
        markDirty()
    }

    fun switchControlMode(mode: ControlMode) {
        viewModelScope.launch {
            operationExecutor.switchMode(mode)
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            try {
                if (_apiKey.value.isBlank()) {
                    _errorMessage.value = "API Key 不能为空"
                    return@launch
                }
                if (_baseUrl.value.isBlank()) {
                    _errorMessage.value = "API 基础 URL 不能为空"
                    return@launch
                }
                if (!_baseUrl.value.startsWith("http://") && !_baseUrl.value.startsWith("https://")) {
                    _errorMessage.value = "API 基础 URL 必须以 http:// 或 https:// 开头"
                    return@launch
                }
                if (_modelName.value.isBlank()) {
                    _errorMessage.value = "模型名称不能为空"
                    return@launch
                }

                dataStore.edit { preferences ->
                    preferences[API_KEY] = _apiKey.value
                    preferences[BASE_URL] = _baseUrl.value
                    preferences[MODEL_NAME] = _modelName.value
                }

                vlmClient.configure(
                    apiKey = _apiKey.value,
                    baseUrl = _baseUrl.value,
                    modelName = _modelName.value
                )

                _isSaved.value = true
                _errorMessage.value = null
            } catch (e: Exception) {
                Timber.e(e, "保存配置失败")
                _errorMessage.value = "保存失败：${e.message ?: "未知错误"}"
            }
        }
    }

    fun testApiConnection() {
        if (_apiKey.value.isBlank()) {
            _apiTestState.value = ApiTestState.Failure("请先填写 API Key")
            return
        }
        if (_baseUrl.value.isBlank()) {
            _apiTestState.value = ApiTestState.Failure("API 基础 URL 不能为空")
            return
        }

        viewModelScope.launch {
            _apiTestState.value = ApiTestState.Loading
            try {
                vlmClient.configure(
                    apiKey = _apiKey.value,
                    baseUrl = _baseUrl.value,
                    modelName = _modelName.value
                )
                val result = vlmClient.testConnection(
                    temperature = _temperature.value,
                    maxTokens = _maxTokens.value
                )
                result.onSuccess { diagnostics ->
                    _apiTestState.value = ApiTestState.Success(
                        latencyMs = diagnostics.latencyMs,
                        responsePreview = diagnostics.responsePreview
                    )
                }.onFailure { error ->
                    _apiTestState.value = ApiTestState.Failure(error.message ?: "连接失败，请稍后重试")
                }
            } catch (e: Exception) {
                Timber.e(e, "API 连接测试异常")
                _apiTestState.value = ApiTestState.Failure(e.message ?: "连接测试异常")
            }
        }
    }

    fun openAccessibilitySettings() {
        accessibilityBridge.openAccessibilitySettings()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun markDirty(resetTest: Boolean = false) {
        _isSaved.value = false
        if (resetTest) {
            _apiTestState.value = ApiTestState.Idle
        }
    }

    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val MODEL_NAME = stringPreferencesKey("model_name")

        private const val DEFAULT_BASE_URL = "https://api.siliconflow.cn/"
        private const val DEFAULT_MODEL = "Qwen/Qwen2-VL-7B-Instruct"
    }
}

sealed class ApiTestState {
    object Idle : ApiTestState()
    object Loading : ApiTestState()
    data class Success(val latencyMs: Long, val responsePreview: String) : ApiTestState()
    data class Failure(val message: String) : ApiTestState()
}

private data class Quad(
    val label: String,
    val description: String,
    val actionLabel: String?,
    val action: () -> Unit
)
