package com.stoneCode.rain_alert.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.stoneCode.rain_alert.data.AppConfig
import java.util.UUID

/**
 * Singleton class to handle Firebase Analytics logging
 * Enhanced to capture detailed metrics about the weather algorithm performance
 */
class FirebaseLogger private constructor() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var userId: String? = null
    private val TAG = "FirebaseLogger"
    
    // Current version of the weather algorithm for tracking improvements over time
    val ALGORITHM_VERSION = "1.0.0"

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
        useCustomSounds: Boolean,
        freezeDurationHours: Int = AppConfig.FREEZE_DURATION_HOURS
    ) {
        try {
            val bundle = Bundle().apply {
                putDouble("freeze_threshold", freezeThreshold)
                putInt("freeze_duration_hours", freezeDurationHours)
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

    fun logNotificationSent(
        notificationType: String,
        alertId: String? = null,
        stationCount: Int? = null,
        weightedPercentage: Double? = null,
        thresholdUsed: Double? = null,
        useMultiStationApproach: Boolean? = null
    ) {
        try {
            val bundle = Bundle().apply {
                putString("notification_type", notificationType)
                putLong("time", System.currentTimeMillis())
                putString("algorithm_version", ALGORITHM_VERSION)
                
                // Add detailed information about the alert
                alertId?.let { putString("alert_id", it) }
                stationCount?.let { putInt("station_count", it) }
                weightedPercentage?.let { putDouble("weighted_percentage", it) }
                thresholdUsed?.let { putDouble("threshold_used", it) }
                useMultiStationApproach?.let { putBoolean("multi_station_approach", it) }
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
        temperature: Double?,
        stationCount: Int? = null,
        maxStationDistance: Double? = null,
        calculationTimeMs: Long? = null,
        algorithmType: String? = null
    ) {
        try {
            val bundle = Bundle().apply {
                putBoolean("is_raining", isRaining)
                putBoolean("is_freezing", isFreezing)
                putString("algorithm_version", ALGORITHM_VERSION)
                precipitationChance?.let { putInt("precipitation_chance", it) }
                temperature?.let { putDouble("temperature", it) }
                
                // Add detailed algorithm performance metrics
                stationCount?.let { putInt("station_count", it) }
                maxStationDistance?.let { putDouble("max_station_distance", it) }
                calculationTimeMs?.let { putLong("calculation_time_ms", it) }
                algorithmType?.let { putString("algorithm_type", it) }
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

    /**
     * Log user feedback about alert accuracy
     * Enhanced version with detailed algorithm data for analysis
     */
    fun logAlertFeedback(
        alertId: String, 
        alertType: String,
        wasAccurate: Boolean, 
        userComments: String? = null,
        stationCount: Int? = null,
        weightedPercentage: Double? = null,
        maxDistance: Double? = null,
        usedMultiStationApproach: Boolean = false,
        thresholdUsed: Double? = null,
        confidenceScore: Float? = null
    ) {
        try {
            val bundle = Bundle().apply {
                putString("alert_id", alertId)
                putString("alert_type", alertType)
                putBoolean("was_accurate", wasAccurate)
                putBoolean("has_feedback_text", userComments != null)
                putString("algorithm_version", ALGORITHM_VERSION)
                putLong("feedback_time", System.currentTimeMillis())
                
                // Add algorithm details for analysis
                stationCount?.let { putInt("station_count", it) }
                weightedPercentage?.let { putDouble("weighted_percentage", it) }
                maxDistance?.let { putDouble("max_distance", it) }
                putBoolean("used_multi_station", usedMultiStationApproach)
                thresholdUsed?.let { putDouble("threshold_used", it) }
                confidenceScore?.let { putFloat("confidence_score", it) }
            }
            firebaseAnalytics.logEvent("alert_feedback", bundle)
            Log.d(TAG, "Logged alert feedback: accurate=$wasAccurate for $alertType alert")
            
            // Also save to Firestore for detailed analysis
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val feedbackData = hashMapOf(
                "alertId" to alertId,
                "alertType" to alertType,
                "wasAccurate" to wasAccurate,
                "userComments" to userComments,
                "timestamp" to System.currentTimeMillis(),
                "algorithmVersion" to ALGORITHM_VERSION,
                "stationCount" to stationCount,
                "weightedPercentage" to weightedPercentage,
                "maxDistance" to maxDistance,
                "usedMultiStationApproach" to usedMultiStationApproach,
                "thresholdUsed" to thresholdUsed,
                "confidenceScore" to confidenceScore
            )
            
            db.collection("alert_feedback")
                .document(alertId)
                .set(feedbackData)
                .addOnSuccessListener { 
                    Log.d(TAG, "Feedback saved to Firestore successfully") 
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving feedback to Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging alert feedback: ${e.message}")
        }
    }
    
    /**
     * Log algorithm performance metrics
     */
    fun logAlgorithmPerformance(
        algorithmType: String, 
        calculationTimeMs: Long,
        numStationsUsed: Int,
        maxDistance: Double,
        weightingMethod: String,
        successful: Boolean
    ) {
        try {
            val bundle = Bundle().apply {
                putString("algorithm_type", algorithmType)
                putLong("calculation_time_ms", calculationTimeMs)
                putInt("stations_used", numStationsUsed)
                putDouble("max_distance", maxDistance)
                putString("weighting_method", weightingMethod)
                putBoolean("successful", successful)
                putString("algorithm_version", ALGORITHM_VERSION)
            }
            firebaseAnalytics.logEvent("algorithm_performance", bundle)
            Log.d(TAG, "Logged algorithm performance: type=$algorithmType, time=$calculationTimeMs ms")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging algorithm performance: ${e.message}")
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