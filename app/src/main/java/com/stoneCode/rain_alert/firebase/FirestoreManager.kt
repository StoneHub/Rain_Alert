package com.stoneCode.rain_alert.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Manages Firestore database operations for storing detailed alert data and user feedback
 * for later analysis to improve the weather algorithm.
 */
class FirestoreManager private constructor() {
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private val TAG = "FirestoreManager"

    fun initialize(context: Context) {
        try {
            db = FirebaseFirestore.getInstance()
            
            // Get the same user ID used by FirebaseLogger
            userId = context.getSharedPreferences("rain_alert_prefs", Context.MODE_PRIVATE)
                .getString("user_id", null)
            
            if (userId == null) {
                userId = UUID.randomUUID().toString()
                context.getSharedPreferences("rain_alert_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("user_id", userId)
                    .apply()
            }
            
            Log.d(TAG, "Firestore initialized with user ID: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore: ${e.message}")
        }
    }

    /**
     * Records detailed information about a rain alert for later analysis
     */
    suspend fun recordRainAlert(
        timestamp: Long,
        location: Map<String, Double>,
        stationData: List<StationContribution>,
        weightedPercentage: Double,
        thresholdUsed: Double,
        wasUsingMultiStationApproach: Boolean,
        precipitationChance: Int? = null,
        alertId: String = UUID.randomUUID().toString()
    ): String {
        try {
            val alertData = hashMapOf(
                "type" to "rain",
                "timestamp" to timestamp,
                "location" to location,
                "stationData" to stationData.map { it.toMap() },
                "weightedPercentage" to weightedPercentage,
                "thresholdUsed" to thresholdUsed,
                "usingMultiStationApproach" to wasUsingMultiStationApproach,
                "precipitationChance" to precipitationChance,
                "userId" to userId,
                "deviceTime" to System.currentTimeMillis(),
                "accuracyFeedback" to null, // To be filled in later by user feedback
                "accuracyScore" to null     // To be filled in later by user feedback
            )

            db.collection("weatherAlerts")
                .document(alertId)
                .set(alertData)
                .await()

            Log.d(TAG, "Recorded rain alert with ID: $alertId")
            return alertId
        } catch (e: Exception) {
            Log.e(TAG, "Error recording rain alert: ${e.message}")
            throw e
        }
    }

    /**
     * Records detailed information about a freeze warning for later analysis
     */
    suspend fun recordFreezeWarning(
        timestamp: Long,
        location: Map<String, Double>,
        stationData: List<StationContribution>? = null,
        currentTemperature: Double,
        forecastTemperatures: List<Double>? = null,
        thresholdUsed: Double,
        durationHours: Int,
        wasUsingMultiStationApproach: Boolean,
        alertId: String = UUID.randomUUID().toString()
    ): String {
        try {
            val alertData = hashMapOf(
                "type" to "freeze",
                "timestamp" to timestamp,
                "location" to location,
                "currentTemperature" to currentTemperature,
                "forecastTemperatures" to forecastTemperatures,
                "thresholdUsed" to thresholdUsed,
                "durationHours" to durationHours,
                "usingMultiStationApproach" to wasUsingMultiStationApproach,
                "userId" to userId,
                "deviceTime" to System.currentTimeMillis(),
                "accuracyFeedback" to null, // To be filled in later by user feedback
                "accuracyScore" to null     // To be filled in later by user feedback
            )

            // Only add station data if using multi-station approach
            stationData?.let {
                alertData["stationData"] = it.map { station -> station.toMap() }
            }

            db.collection("weatherAlerts")
                .document(alertId)
                .set(alertData)
                .await()

            Log.d(TAG, "Recorded freeze warning with ID: $alertId")
            return alertId
        } catch (e: Exception) {
            Log.e(TAG, "Error recording freeze warning: ${e.message}")
            throw e
        }
    }

    /**
     * Records user feedback about the accuracy of a weather alert
     */
    suspend fun recordAlertFeedback(
        alertId: String,
        accuracyScore: Int, // 1-5 scale where 5 is most accurate
        feedback: String? = null,
        actualConditionsDescription: String? = null
    ): Boolean {
        try {
            val feedbackData = hashMapOf<String, Any>(
                "accuracyScore" to accuracyScore,
                "feedbackTimestamp" to System.currentTimeMillis()
            )
            
            feedback?.let { feedbackData["accuracyFeedback"] = it }
            actualConditionsDescription?.let { feedbackData["actualConditions"] = it }

            db.collection("weatherAlerts")
                .document(alertId)
                .set(feedbackData, SetOptions.merge())
                .await()

            Log.d(TAG, "Recorded feedback for alert ID: $alertId")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error recording alert feedback: ${e.message}")
            return false
        }
    }

    /**
     * Records algorithm performance metrics to track over time
     */
    suspend fun recordAlgorithmMetrics(
        algorithmVersion: String,
        metricType: String, // "rain" or "freeze"
        calculationTimeMs: Long,
        numStationsUsed: Int,
        maxDistance: Double,
        weightingMethod: String = "inverse", // For future alternative weighting methods
        successful: Boolean,
        errorMessage: String? = null
    ) {
        try {
            val metricsData = hashMapOf(
                "algorithmVersion" to algorithmVersion,
                "metricType" to metricType,
                "timestamp" to System.currentTimeMillis(),
                "calculationTimeMs" to calculationTimeMs,
                "numStationsUsed" to numStationsUsed,
                "maxDistance" to maxDistance,
                "weightingMethod" to weightingMethod,
                "successful" to successful,
                "userId" to userId
            )
            
            errorMessage?.let { metricsData["errorMessage"] = it }

            db.collection("algorithmMetrics")
                .add(metricsData)
                .await()

            Log.d(TAG, "Recorded algorithm metrics for $metricType")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording algorithm metrics: ${e.message}")
        }
    }

    /**
     * Records station reliability metrics
     */
    suspend fun recordStationReliability(
        stationId: String,
        stationName: String,
        distance: Double,
        wasContributingToAlert: Boolean,
        observationMatchedActual: Boolean? = null,
        delayMinutes: Int? = null
    ) {
        try {
            val reliabilityData = hashMapOf(
                "stationId" to stationId,
                "stationName" to stationName,
                "timestamp" to System.currentTimeMillis(),
                "distance" to distance,
                "contributedToAlert" to wasContributingToAlert,
                "userId" to userId
            )
            
            observationMatchedActual?.let { reliabilityData["matchedActual"] = it }
            delayMinutes?.let { reliabilityData["delayMinutes"] = it }

            db.collection("stationReliability")
                .add(reliabilityData)
                .await()

            Log.d(TAG, "Recorded reliability data for station: $stationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording station reliability: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var instance: FirestoreManager? = null

        fun getInstance(): FirestoreManager {
            return instance ?: synchronized(this) {
                instance ?: FirestoreManager().also { instance = it }
            }
        }
    }
}

/**
 * Data class to store information about a weather station's contribution to an alert
 */
data class StationContribution(
    val stationId: String,
    val stationName: String,
    val distance: Double,
    val weight: Double,
    val isPositive: Boolean, // true if reporting rain/freeze
    val temperature: Double? = null,
    val precipitation: Double? = null,
    val textDescription: String? = null,
    val observationTime: Long? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "stationId" to stationId,
            "stationName" to stationName,
            "distance" to distance,
            "weight" to weight,
            "isPositive" to isPositive,
            "temperature" to temperature,
            "precipitation" to precipitation,
            "textDescription" to textDescription,
            "observationTime" to observationTime
        )
    }
}