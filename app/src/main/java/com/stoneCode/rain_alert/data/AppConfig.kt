package com.stoneCode.rain_alert.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

object AppConfig {
    // Default weather thresholds
    const val FREEZE_THRESHOLD_F = 35.0
    const val RAIN_PROBABILITY_THRESHOLD = 50

    // Default service intervals
    const val RAIN_CHECK_INTERVAL_MS = 15 * 60 * 1000L  // 15 minutes
    const val FREEZE_CHECK_INTERVAL_MS = 60 * 60 * 1000L  // 1 hour

    // API Constants
    const val USER_AGENT = "RainAlertApp"
    const val CONTACT_EMAIL = "threedimensionalstone@gmail.com"
    const val WEATHER_API_BASE_URL = "https://api.weather.gov/"

    // Notification IDs
    const val FOREGROUND_SERVICE_ID = 2
    const val RAIN_NOTIFICATION_ID = 1
    const val FREEZE_WARNING_NOTIFICATION_ID = 3

    // DataStore preference keys
    val FREEZE_THRESHOLD_KEY = doublePreferencesKey("freeze_threshold")
    val RAIN_PROBABILITY_THRESHOLD_KEY = intPreferencesKey("rain_probability_threshold")
    val RAIN_CHECK_INTERVAL_KEY = longPreferencesKey("rain_check_interval")
    val FREEZE_CHECK_INTERVAL_KEY = longPreferencesKey("freeze_check_interval")
    val ENABLE_RAIN_NOTIFICATIONS_KEY = booleanPreferencesKey("enable_rain_notifications")
    val ENABLE_FREEZE_NOTIFICATIONS_KEY = booleanPreferencesKey("enable_freeze_notifications")
    val USE_CUSTOM_SOUNDS_KEY = booleanPreferencesKey("use_custom_sounds")
}