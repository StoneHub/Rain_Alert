package com.stoneCode.rain_alert.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * This class manages user preferences using DataStore.
 * Note: Current application implementation uses hardcoded values,
 * but this class is set up for future expansion when user settings UI is added.
 */
class UserPreferences(private val context: Context) {

    // Get freeze threshold with default value
    val freezeThreshold: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[AppConfig.FREEZE_THRESHOLD_KEY] ?: AppConfig.FREEZE_THRESHOLD_F
        }

    // Get rain probability threshold with default value
    val rainProbabilityThreshold: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[AppConfig.RAIN_PROBABILITY_THRESHOLD_KEY] ?: AppConfig.RAIN_PROBABILITY_THRESHOLD
        }

    // Get rain check interval with default value
    val rainCheckInterval: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[AppConfig.RAIN_CHECK_INTERVAL_KEY] ?: AppConfig.RAIN_CHECK_INTERVAL_MS
        }

    // Get freeze check interval with default value
    val freezeCheckInterval: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[AppConfig.FREEZE_CHECK_INTERVAL_KEY] ?: AppConfig.FREEZE_CHECK_INTERVAL_MS
        }

    // Get enable rain notifications preference
    val enableRainNotifications: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AppConfig.ENABLE_RAIN_NOTIFICATIONS_KEY] ?: true
        }
    
    // Get enable freeze notifications preference
    val enableFreezeNotifications: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AppConfig.ENABLE_FREEZE_NOTIFICATIONS_KEY] ?: true
        }
    
    // Get use custom sounds preference
    val useCustomSounds: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AppConfig.USE_CUSTOM_SOUNDS_KEY] ?: true
        }

    // Update freeze threshold
    suspend fun updateFreezeThreshold(threshold: Double) {
        context.dataStore.edit { preferences ->
            preferences[AppConfig.FREEZE_THRESHOLD_KEY] = threshold
        }
    }

    // Update rain probability threshold
    suspend fun updateRainProbabilityThreshold(threshold: Int) {
        context.dataStore.edit { preferences ->
            preferences[AppConfig.RAIN_PROBABILITY_THRESHOLD_KEY] = threshold
        }
    }

    // Update rain check interval
    suspend fun updateRainCheckInterval(intervalMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[AppConfig.RAIN_CHECK_INTERVAL_KEY] = intervalMs
        }
    }

    // Update freeze check interval
    suspend fun updateFreezeCheckInterval(intervalMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[AppConfig.FREEZE_CHECK_INTERVAL_KEY] = intervalMs
        }
    }
    
    // Update enable rain notifications
    suspend fun updateEnableRainNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AppConfig.ENABLE_RAIN_NOTIFICATIONS_KEY] = enabled
        }
    }
    
    // Update enable freeze notifications
    suspend fun updateEnableFreezeNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AppConfig.ENABLE_FREEZE_NOTIFICATIONS_KEY] = enabled
        }
    }
    
    // Update use custom sounds
    suspend fun updateUseCustomSounds(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AppConfig.USE_CUSTOM_SOUNDS_KEY] = enabled
        }
    }
}