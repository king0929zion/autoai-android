@file:OptIn(ExperimentalMaterial3Api::class)

package com.autoai.android.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoai.android.R
import com.autoai.android.accessibility.AccessibilityBridge
import com.autoai.android.accessibility.AccessibilityStatus
import com.autoai.android.decision.VLMClient
import com.autoai.android.permission.ControlMode
import com.autoai.android.permission.OperationExecutor
import com.autoai.android.permission.ShizukuManager
import com.autoai.android.permission.ShizukuStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val maxTokensInput by viewModel.maxTokensInput.collectAsState()
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
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_save_success))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ControlModeCard(
                controlMode = controlMode,
                shizukuStatus = shizukuStatus,
                accessibilityStatus = accessibilityStatus,
                onModeSelected = viewModel::switchControlMode,
                onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                onOpenShizuku = { openShizukuApp(context) }
            )

            ApiSettingsCard(
                apiKey = apiKey,
                onApiKeyChange = viewModel::updateApiKey,
                baseUrl = baseUrl,
                onBaseUrlChange = viewModel::updateBaseUrl,
                modelName = modelName,
                onModelNameChange = viewModel::updateModelName,
                temperature = temperature,
                onTemperatureChange = viewModel::updateTemperature,
                maxTokens = maxTokensInput,
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
private fun ControlModeCard(
    controlMode: ControlMode,
    shizukuStatus: ShizukuStatus,
    accessibilityStatus: AccessibilityStatus,
    onModeSelected: (ControlMode) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenShizuku: () -> Unit
) {
    val statusInfo = when (controlMode) {
        ControlMode.ACCESSIBILITY -> accessibilityStatus.toStatusInfo(onOpenAccessibilitySettings)
        ControlMode.SHIZUKU -> shizukuStatus.toStatusInfo(onOpenShizuku)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.settings_control_mode_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = stringResource(R.string.settings_control_mode_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = controlMode.isAccessibility(),
                    onClick = { onModeSelected(ControlMode.ACCESSIBILITY) },
                    label = { Text(stringResource(R.string.settings_control_mode_accessibility)) },
                    leadingIcon = { Icon(Icons.Default.AccessibilityNew, contentDescription = null) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = controlMode.isShizuku(),
                    onClick = { onModeSelected(ControlMode.SHIZUKU) },
                    label = { Text(stringResource(R.string.settings_control_mode_shizuku)) },
                    leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            StatusSummary(statusInfo = statusInfo)
        }
    }
}
@Composable
private fun StatusSummary(statusInfo: StatusInfo) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = statusInfo.headline,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = statusInfo.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = statusInfo.actionLabel != null && statusInfo.onAction != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val actionLabel = statusInfo.actionLabel
                val action = statusInfo.onAction
                if (actionLabel != null && action != null) {
                    AssistChip(onClick = action, label = { Text(actionLabel) })
                }
            }
        }
    }
}

@Composable
private fun ApiSettingsCard(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    modelName: String,
    onModelNameChange: (String) -> Unit,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    maxTokens: String,
    onMaxTokensChange: (String) -> Unit,
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
                text = stringResource(R.string.settings_api_section_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text(stringResource(R.string.settings_api_key_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChange,
                label = { Text(stringResource(R.string.settings_api_endpoint_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = onModelNameChange,
                label = { Text(stringResource(R.string.settings_api_model_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_temperature_label, temperature),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = temperature,
                    onValueChange = onTemperatureChange,
                    valueRange = 0.0f..1.2f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            OutlinedTextField(
                value = maxTokens,
                onValueChange = onMaxTokensChange,
                label = { Text(stringResource(R.string.settings_max_tokens_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Switch(checked = isDarkTheme, onCheckedChange = onDarkThemeChange)
                Text(
                    text = stringResource(R.string.settings_dark_theme_toggle),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            ApiTestStatus(state = apiTestState)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onTestConnection,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_test_button))
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.settings_save_button))
                }
            }
        }
    }
}

@Composable
private fun ApiTestStatus(state: ApiTestState) {
    Crossfade(targetState = state, label = "api_test_state") { current ->
        when (current) {
            ApiTestState.Idle -> Unit
            ApiTestState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.settings_testing_status),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is ApiTestState.Success -> {
                val preview = current.responsePreview.ifBlank {
                    stringResource(R.string.settings_api_success_message)
                }
                StatusBanner(
                    icon = Icons.Default.CloudDone,
                    tint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_api_success_title, current.latencyMs),
                    message = preview
                )
            }

            is ApiTestState.Failure -> {
                StatusBanner(
                    icon = Icons.Default.ErrorOutline,
                    tint = MaterialTheme.colorScheme.error,
                    title = stringResource(R.string.settings_api_failure_title),
                    message = stringResource(R.string.settings_api_failure_message, current.message)
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    title: String,
    message: String
) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class StatusInfo(
    val headline: String,
    val description: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@Composable
@Composable
private fun AccessibilityStatus.toStatusInfo(onOpenSettings: () -> Unit): StatusInfo = when (this) {
    AccessibilityStatus.READY -> StatusInfo(
        headline = stringResource(R.string.settings_accessibility_ready),
        description = stringResource(R.string.settings_accessibility_ready_desc)
    )

    AccessibilityStatus.CONNECTING -> StatusInfo(
        headline = stringResource(R.string.settings_accessibility_connecting),
        description = stringResource(R.string.settings_accessibility_connecting_desc)
    )

    AccessibilityStatus.SERVICE_DISABLED -> StatusInfo(
        headline = stringResource(R.string.settings_accessibility_disabled),
        description = stringResource(R.string.settings_accessibility_disabled_desc),
        actionLabel = stringResource(R.string.settings_accessibility_open_button),
        onAction = onOpenSettings
    )

    AccessibilityStatus.ERROR -> StatusInfo(
        headline = stringResource(R.string.settings_accessibility_error),
        description = stringResource(R.string.settings_accessibility_error_desc),
        actionLabel = stringResource(R.string.settings_accessibility_open_button),
        onAction = onOpenSettings
    )
}

@Composable
@Composable
private fun ShizukuStatus.toStatusInfo(onOpenShizuku: () -> Unit): StatusInfo = when (this) {
    ShizukuStatus.AVAILABLE -> StatusInfo(
        headline = stringResource(R.string.settings_shizuku_ready),
        description = stringResource(R.string.settings_shizuku_ready_desc)
    )

    ShizukuStatus.PERMISSION_REQUIRED -> StatusInfo(
        headline = stringResource(R.string.settings_shizuku_permission),
        description = stringResource(R.string.settings_shizuku_permission_desc),
        actionLabel = stringResource(R.string.settings_shizuku_open_button),
        onAction = onOpenShizuku
    )

    ShizukuStatus.NOT_RUNNING -> StatusInfo(
        headline = stringResource(R.string.settings_shizuku_not_running),
        description = stringResource(R.string.settings_shizuku_not_running_desc),
        actionLabel = stringResource(R.string.settings_shizuku_open_button),
        onAction = onOpenShizuku
    )

    ShizukuStatus.NOT_INSTALLED -> StatusInfo(
        headline = stringResource(R.string.settings_shizuku_not_installed),
        description = stringResource(R.string.settings_shizuku_not_installed_desc),
        actionLabel = stringResource(R.string.settings_shizuku_website_button),
        onAction = onOpenShizuku
    )

    ShizukuStatus.UNKNOWN -> StatusInfo(
        headline = stringResource(R.string.settings_shizuku_unknown),
        description = stringResource(R.string.settings_shizuku_unknown_desc),
        actionLabel = stringResource(R.string.settings_shizuku_open_button),
        onAction = onOpenShizuku
    )

    ShizukuStatus.ERROR -> StatusInfo(
        headline = stringResource(R.string.settings_shizuku_error),
        description = stringResource(R.string.settings_shizuku_error_desc),
        actionLabel = stringResource(R.string.settings_shizuku_open_button),
        onAction = onOpenShizuku
    )
}
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val vlmClient: VLMClient,
    private val operationExecutor: OperationExecutor,
    private val shizukuManager: ShizukuManager,
    private val accessibilityBridge: AccessibilityBridge,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _baseUrl = MutableStateFlow(DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _modelName = MutableStateFlow(DEFAULT_MODEL)
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _temperature = MutableStateFlow(DEFAULT_TEMPERATURE)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _maxTokensInput = MutableStateFlow(DEFAULT_MAX_TOKENS.toString())
    val maxTokensInput: StateFlow<String> = _maxTokensInput.asStateFlow()

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
                    _temperature.value = preferences[TEMPERATURE] ?: DEFAULT_TEMPERATURE
                    _maxTokensInput.value = (preferences[MAX_TOKENS] ?: DEFAULT_MAX_TOKENS).toString()
                    _isDarkTheme.value = preferences[DARK_THEME] ?: false
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
        _temperature.value = value.coerceIn(0f, 1.5f)
        markDirty(resetTest = true)
    }

    fun updateMaxTokens(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(4)
        _maxTokensInput.value = sanitized
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
                val apiKey = _apiKey.value.trim()
                val baseUrl = _baseUrl.value.trim()
                val modelName = _modelName.value.trim()
                val maxTokens = resolveMaxTokens()

                when {
                    apiKey.isBlank() -> {
                        _errorMessage.value = appContext.getString(R.string.settings_error_api_key_empty)
                        return@launch
                    }

                    baseUrl.isBlank() -> {
                        _errorMessage.value = appContext.getString(R.string.settings_error_endpoint_empty)
                        return@launch
                    }

                    !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://") -> {
                        _errorMessage.value = appContext.getString(R.string.settings_error_endpoint_schema)
                        return@launch
                    }

                    modelName.isBlank() -> {
                        _errorMessage.value = appContext.getString(R.string.settings_error_model_empty)
                        return@launch
                    }
                }

                dataStore.edit { preferences ->
                    preferences[API_KEY] = apiKey
                    preferences[BASE_URL] = baseUrl
                    preferences[MODEL_NAME] = modelName
                    preferences[TEMPERATURE] = _temperature.value
                    preferences[MAX_TOKENS] = maxTokens
                    preferences[DARK_THEME] = _isDarkTheme.value
                }

                vlmClient.configure(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    modelName = modelName
                )

                _maxTokensInput.value = maxTokens.toString()
                _isSaved.value = true
                _errorMessage.value = null
            } catch (error: Exception) {
                Timber.e(error, "Failed to save settings")
                val message = error.message ?: appContext.getString(R.string.settings_error_save_unknown)
                _errorMessage.value = appContext.getString(R.string.settings_error_save_failed, message)
            }
        }
    }

    fun testApiConnection() {
        val apiKey = _apiKey.value.trim()
        val baseUrl = _baseUrl.value.trim()
        val modelName = _modelName.value.trim()
        val maxTokens = resolveMaxTokens()

        when {
            apiKey.isBlank() -> {
                _apiTestState.value = ApiTestState.Failure(appContext.getString(R.string.settings_test_missing_key))
                return
            }

            baseUrl.isBlank() -> {
                _apiTestState.value = ApiTestState.Failure(appContext.getString(R.string.settings_test_missing_endpoint))
                return
            }
        }

        viewModelScope.launch {
            _apiTestState.value = ApiTestState.Loading
            try {
                vlmClient.configure(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    modelName = modelName
                )
                val result = vlmClient.testConnection(
                    temperature = _temperature.value,
                    maxTokens = maxTokens
                )
                result.onSuccess { diagnostics ->
                    _apiTestState.value = ApiTestState.Success(
                        latencyMs = diagnostics.latencyMs,
                        responsePreview = diagnostics.responsePreview
                    )
                }.onFailure { error ->
                    _apiTestState.value = ApiTestState.Failure(
                        error.message ?: appContext.getString(R.string.error_unknown)
                    )
                }
            } catch (error: Exception) {
                Timber.e(error, "API test failed")
                _apiTestState.value = ApiTestState.Failure(
                    error.message ?: appContext.getString(R.string.error_unknown)
                )
            }
        }
    }

    fun openAccessibilitySettings() {
        accessibilityBridge.openAccessibilitySettings()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun resolveMaxTokens(): Int =
        _maxTokensInput.value.toIntOrNull()?.coerceIn(128, 4096) ?: DEFAULT_MAX_TOKENS

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
        private val TEMPERATURE = floatPreferencesKey("temperature")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val DARK_THEME = booleanPreferencesKey("dark_theme")

        private const val DEFAULT_BASE_URL = "https://api.siliconflow.cn/"
        private const val DEFAULT_MODEL = "Qwen/Qwen2-VL-7B-Instruct"
        private const val DEFAULT_TEMPERATURE = 0.4f
        private const val DEFAULT_MAX_TOKENS = 1024
    }
}

sealed class ApiTestState {
    object Idle : ApiTestState()
    object Loading : ApiTestState()
    data class Success(val latencyMs: Long, val responsePreview: String) : ApiTestState()
    data class Failure(val message: String) : ApiTestState()
}

private fun openShizukuApp(context: Context) {
    val packageName = "moe.shizuku.privileged.api"
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        val launched = runCatching {
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrElse {
            Timber.e(it, "Unable to start Shizuku app")
            false
        }
        if (launched) return
    }

    runCatching {
        val webpage = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
        context.startActivity(webpage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { Timber.e(it, "Unable to open Shizuku website") }
}
