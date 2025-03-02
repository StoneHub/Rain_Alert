package com.stoneCode.rain_alert.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.UUID

/**
 * Singleton class to handle Firebase Analytics logging
 */
class FirebaseLogger private constructor() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var userId: String? = null
    private val TAG = "FirebaseLogger"

    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            
            // Generate a unique ID for this installation if not already set
            userId = context.getSharedPreferences("rain_alert_prefs", Context.MODE_PRIVATE)
                .getString("user_id", null)
            
            if (userId == null) {
                userId = UUID.randomUUID().toString()
                context.getSharedPreferences("rain_alert_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("user_id", userId)
                    .apply()
            }
            
            // Set user ID in Firebase
            firebaseAnalytics.setUserId(userId)
            
            Log.d(TAG, "Firebase Analytics initialized with user ID: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}")
        }
    }

    fun logServiceStatusChanged(isRunning: Boolean) {
        try {
            val bundle = Bundle().apply {
                putBoolean("service_running", isRunning)
            }
            firebaseAnalytics.logEvent("service_status_changed", bundle)
            Log.d(TAG, "Logged service status: $isRunning")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging service status: ${e.message}")
        }
    }

    fun logSettingsChanged(
        freezeThreshold: Double,
        rainProbabilityThreshold: Int,
        enableRainNotifications: Boolean,
        enableFreezeNotifications: Boolean,
        useCustomSounds: Boolean
    ) {
        try {
            val bundle = Bundle().apply {
                putDouble("freeze_threshold", freezeThreshold)
                putInt("rain_probability_threshold", rainProbabilityThreshold)
                putBoolean("enable_rain_notifications", enableRainNotifications)
                putBoolean("enable_freeze_notifications", enableFreezeNotifications)
                putBoolean("use_custom_sounds", useCustomSounds)
            }
            firebaseAnalytics.logEvent("settings_changed", bundle)
            Log.d(TAG, "Logged settings change")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging settings change: ${e.message}")
        }
    }

    fun logNotificationSent(notificationType: String) {
        try {
            val bundle = Bundle().apply {
                putString("notification_type", notificationType)
                putLong("time", System.currentTimeMillis())
            }
            firebaseAnalytics.logEvent("notification_sent", bundle)
            Log.d(TAG, "Logged notification sent: $notificationType")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging notification: ${e.message}")
        }
    }

    fun logWeatherCheck(
        isRaining: Boolean,
        isFreezing: Boolean,
        precipitationChance: Int?,
        temperature: Double?
    ) {
        try {
            val bundle = Bundle().apply {
                putBoolean("is_raining", isRaining)
                putBoolean("is_freezing", isFreezing)
                precipitationChance?.let { putInt("precipitation_chance", it) }
                temperature?.let { putDouble("temperature", it) }
            }
            firebaseAnalytics.logEvent("weather_check", bundle)
            Log.d(TAG, "Logged weather check: Rain=$isRaining, Freeze=$isFreezing")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging weather check: ${e.message}")
        }
    }
    
    fun logSimulation(simulationType: String, isSuccess: Boolean) {
        try {
            val bundle = Bundle().apply {
                putString("simulation_type", simulationType)
                putBoolean("success", isSuccess)
                putLong("timestamp", System.currentTimeMillis())
            }
            firebaseAnalytics.logEvent("simulation_triggered", bundle)
            Log.d(TAG, "Logged simulation: $simulationType, success=$isSuccess")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging simulation: ${e.message}")
        }
    }
    
    fun logApiStatus(isSuccess: Boolean, errorMessage: String? = null, endpoint: String? = null, responseTime: Long? = null) {
        try {
            val bundle = Bundle().apply {
                putBoolean("success", isSuccess)
                putLong("timestamp", System.currentTimeMillis())
                errorMessage?.let { putString("error_message", it) }
                endpoint?.let { putString("endpoint", it) }
                responseTime?.let { putLong("response_time_ms", it) }
            }
            firebaseAnalytics.logEvent("api_request", bundle)
            if (isSuccess) {
                Log.d(TAG, "Logged successful API request to $endpoint")
            } else {
                Log.d(TAG, "Logged failed API request to $endpoint: $errorMessage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging API status: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var instance: FirebaseLogger? = null

        fun getInstance(): FirebaseLogger {
            return instance ?: synchronized(this) {
                instance ?: FirebaseLogger().also { instance = it }
            }
        }
    }
}