package com.autoai.android.permission

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore wrapper that persists the selected control mode (accessibility / Shizuku).
 */
@Singleton
class ControlPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val controlModeFlow: Flow<ControlMode> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            preferences[Keys.CONTROL_MODE]?.let { raw ->
                runCatching { ControlMode.valueOf(raw) }.getOrDefault(DEFAULT_MODE)
            } ?: DEFAULT_MODE
        }

    suspend fun getCurrentMode(): ControlMode =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[Keys.CONTROL_MODE] }
            .firstOrNull()
            ?.let { stored -> runCatching { ControlMode.valueOf(stored) }.getOrDefault(DEFAULT_MODE) }
            ?: DEFAULT_MODE

    suspend fun setControlMode(mode: ControlMode) {
        dataStore.edit { prefs ->
            prefs[Keys.CONTROL_MODE] = mode.name
        }
    }

    private object Keys {
        val CONTROL_MODE = stringPreferencesKey("control_mode")
    }

    companion object {
        val DEFAULT_MODE: ControlMode = ControlMode.ACCESSIBILITY
    }
}
