package com.stoneCode.rain_alert.feedback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.StoneCode.rain_alert.R
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.firebase.FirestoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Manages feedback collection for weather alerts to improve algorithm accuracy
 */
class AlertFeedbackManager private constructor(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        FEEDBACK_PREFS, Context.MODE_PRIVATE
    )
    private val TAG = "AlertFeedbackManager"
    
    // Notification channel for feedback requests
    private val FEEDBACK_CHANNEL_ID = "feedback_channel"
    private val FEEDBACK_NOTIFICATION_ID = 1001

    init {
        createNotificationChannel()
    }

    /**
     * Records a new weather alert for potential feedback collection
     */
    fun recordAlert(alertId: String, alertType: String, timestamp: Long) {
        sharedPreferences.edit().apply {
            putString("$alertId:type", alertType)
            putLong("$alertId:timestamp", timestamp)
            putBoolean("$alertId:feedback_requested", false)
            putBoolean("$alertId:feedback_provided", false)
        }.apply()
        
        Log.d(TAG, "Recorded alert $alertId of type $alertType for potential feedback")
    }

    /**
     * Schedules a feedback request for an alert that was previously triggered
     * Will request feedback after a delay to allow the user to experience the actual weather
     */
    fun scheduleFeedbackRequest(alertId: String, delayMinutes: Int = 30) {
        val alertType = sharedPreferences.getString("$alertId:type", null) ?: return
        val timestamp = sharedPreferences.getLong("$alertId:timestamp", 0)
        if (timestamp == 0L) return
        
        // Mark that feedback has been requested for this alert
        sharedPreferences.edit().putBoolean("$alertId:feedback_requested", true).apply()
        
        // Schedule the feedback notification
        val notificationTime = timestamp + TimeUnit.MINUTES.toMillis(delayMinutes.toLong())
        val currentTime = System.currentTimeMillis()
        
        if (currentTime >= notificationTime) {
            // If the delay has already passed, send the notification immediately
            sendFeedbackNotification(alertId, alertType)
        } else {
            // TODO: For a complete implementation, use WorkManager to schedule this notification
            // This is a simple version that just logs the intent to request feedback later
            Log.d(TAG, "Would schedule feedback request for alert $alertId in $delayMinutes minutes")
            
            // For demonstration purposes, we'll just send it now
            sendFeedbackNotification(alertId, alertType)
        }
    }

    /**
     * Sends a notification asking the user for feedback on an alert's accuracy
     */
    private fun sendFeedbackNotification(alertId: String, alertType: String) {
        val intent = Intent(context, FeedbackActivity::class.java).apply {
            putExtra(EXTRA_ALERT_ID, alertId)
            putExtra(EXTRA_ALERT_TYPE, alertType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            alertId.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationTitle = when (alertType) {
            "rain" -> "Was our rain alert accurate?"
            "freeze" -> "Was our freeze warning accurate?"
            else -> "How was our weather alert?"
        }
        
        val notificationText = "Help us improve! Tap to provide quick feedback."
        
        val notification = NotificationCompat.Builder(context, FEEDBACK_CHANNEL_ID)
            .setSmallIcon(R.drawable.rain_alert_ico)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(FEEDBACK_NOTIFICATION_ID, notification)
            Log.d(TAG, "Sent feedback request notification for alert $alertId")
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to send feedback notification: ${e.message}")
        }
    }

    /**
     * Records user feedback for a specific alert
     */
    fun recordFeedback(
        alertId: String, 
        alertType: String,
        accuracyScore: Int, 
        feedback: String? = null,
        actualConditionsDescription: String? = null
    ) {
        // Mark that feedback has been provided
        sharedPreferences.edit().apply {
            putBoolean("$alertId:feedback_provided", true)
            putInt("$alertId:accuracy_score", accuracyScore)
            feedback?.let { putString("$alertId:feedback", it) }
            actualConditionsDescription?.let { putString("$alertId:actual_conditions", it) }
        }.apply()
        
        // Log to Firebase Analytics
        FirebaseLogger.getInstance().logAlertFeedback(
            alertId = alertId,
            alertType = alertType,
            accuracyScore = accuracyScore,
            hasFeedbackText = !feedback.isNullOrBlank()
        )
        
        // Store in Firestore for detailed analysis
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirestoreManager.getInstance().recordAlertFeedback(
                    alertId = alertId,
                    accuracyScore = accuracyScore,
                    feedback = feedback,
                    actualConditionsDescription = actualConditionsDescription
                )
                Log.d(TAG, "Recorded feedback for alert $alertId with score $accuracyScore")
            } catch (e: Exception) {
                Log.e(TAG, "Error storing feedback in Firestore: ${e.message}")
            }
        }
    }

    /**
     * Checks for alerts that need feedback requests
     */
    fun checkPendingFeedbackRequests() {
        // This would scan all stored alerts and check which ones need feedback
        // For now, we'll just log that the check was performed
        Log.d(TAG, "Checked for pending feedback requests")
    }

    /**
     * Creates the notification channel for feedback requests
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weather Alert Feedback"
            val descriptionText = "Notifications requesting feedback about weather alerts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(FEEDBACK_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val FEEDBACK_PREFS = "feedback_preferences"
        const val EXTRA_ALERT_ID = "alert_id"
        const val EXTRA_ALERT_TYPE = "alert_type"
        
        @Volatile
        private var instance: AlertFeedbackManager? = null
        
        fun getInstance(context: Context): AlertFeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: AlertFeedbackManager(context.applicationContext).also { instance = it }
            }
        }
    }
}