package uk.appyapp.stepsafe.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Internal for testing to provide a mock DataStore
    internal var testDataStore: DataStore<Preferences>? = null

    private val dataStore: DataStore<Preferences>
        get() = testDataStore ?: context.settingsDataStore

    private object PreferencesKeys {
        val SAFE_ZONE_RADIUS = doublePreferencesKey("safe_zone_radius")
        val VOICE_PROMPT_ENABLED = booleanPreferencesKey("voice_prompt_enabled")
        val MONITORING_ACTIVE = booleanPreferencesKey("monitoring_active")
    }

    val safeZoneRadius: Flow<Double>
        get() = dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.SAFE_ZONE_RADIUS] ?: 100.0 // Default 100 meters
            }

    val voicePromptEnabled: Flow<Boolean>
        get() = dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.VOICE_PROMPT_ENABLED] ?: true
            }

    val monitoringActive: Flow<Boolean>
        get() = dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.MONITORING_ACTIVE] ?: false
            }

    suspend fun updateSafeZoneRadius(radius: Double) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAFE_ZONE_RADIUS] = radius
        }
    }

    suspend fun updateVoicePromptEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_PROMPT_ENABLED] = enabled
        }
    }

    suspend fun updateMonitoringActive(active: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONITORING_ACTIVE] = active
        }
    }
}
