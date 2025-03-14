package com.stoneCode.rain_alert.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object AppConfig {
    // Default weather thresholds
    const val FREEZE_THRESHOLD_F = 35.0
    const val FREEZE_DURATION_HOURS = 4
    const val RAIN_PROBABILITY_THRESHOLD = 50

    // Default service intervals
    const val RAIN_CHECK_INTERVAL_MS = 15 * 60 * 1000L  // 15 minutes
    const val FREEZE_CHECK_INTERVAL_MS = 60 * 60 * 1000L  // 1 hour
    const val STATION_REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1000L  // 6 hours

    // API Constants
    const val USER_AGENT = "RainAlertApp"
    const val CONTACT_EMAIL = "threedimensionalstone@gmail.com"
    const val WEATHER_API_BASE_URL = "https://api.weather.gov/"

    // Notification IDs
    const val FOREGROUND_SERVICE_ID = 2
    const val RAIN_NOTIFICATION_ID = 1
    const val FREEZE_WARNING_NOTIFICATION_ID = 3
    const val PERMISSION_NOTIFICATION_ID = 4

    // DataStore preference keys
    val FREEZE_THRESHOLD_KEY = doublePreferencesKey("freeze_threshold")
    val FREEZE_DURATION_HOURS_KEY = intPreferencesKey("freeze_duration_hours")
    val RAIN_PROBABILITY_THRESHOLD_KEY = intPreferencesKey("rain_probability_threshold")
    val RAIN_CHECK_INTERVAL_KEY = longPreferencesKey("rain_check_interval")
    val FREEZE_CHECK_INTERVAL_KEY = longPreferencesKey("freeze_check_interval")
    val ENABLE_RAIN_NOTIFICATIONS_KEY = booleanPreferencesKey("enable_rain_notifications")
    val ENABLE_FREEZE_NOTIFICATIONS_KEY = booleanPreferencesKey("enable_freeze_notifications")
    val USE_CUSTOM_SOUNDS_KEY = booleanPreferencesKey("use_custom_sounds")
    val CUSTOM_LOCATION_ZIP_KEY = stringPreferencesKey("custom_location_zip")
    val USE_CUSTOM_LOCATION_KEY = booleanPreferencesKey("use_custom_location")
    val SELECTED_STATION_IDS_KEY = stringSetPreferencesKey("selected_station_ids")
    
    // Algorithm settings keys
    val PREFER_MULTI_STATION_KEY = booleanPreferencesKey("prefer_multi_station")
    val MIN_STATIONS_REQUIRED_KEY = intPreferencesKey("min_stations_required")
    val MAX_STATION_DISTANCE_KEY = doublePreferencesKey("max_station_distance")
}