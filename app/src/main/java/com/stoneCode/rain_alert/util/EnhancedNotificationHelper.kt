package com.stoneCode.rain_alert.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.StoneCode.rain_alert.R
import com.stoneCode.rain_alert.MainActivity
import com.stoneCode.rain_alert.data.AlertType
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.data.UserPreferences
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.repository.AlertHistoryRepository
import com.stoneCode.rain_alert.repository.WeatherRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class EnhancedNotificationHelper(private val context: Context) {

    val FOREGROUND_SERVICE_CHANNEL_ID = "foreground_service_channel"
    private val RAIN_NOTIFICATION_CHANNEL_ID = "rain_notification_channel"
    private val FREEZE_WARNING_CHANNEL_ID = "freeze_warning_channel"
    private val userPreferences = UserPreferences(context)
    private val firebaseLogger = FirebaseLogger.getInstance()
    private val alertHistoryRepository = AlertHistoryRepository(context)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Foreground service channel
            createNotificationChannel(
                FOREGROUND_SERVICE_CHANNEL_ID,
                "Foreground Service",
                NotificationManager.IMPORTANCE_LOW,
                "Enables background weather monitoring",
                null
            )
            
            // Rain notification channel with custom sound
            val rainSoundUri = Uri.parse("android.resource://${context.packageName}/raw/rain_alert_short")
            createNotificationChannel(
                RAIN_NOTIFICATION_CHANNEL_ID,
                "Rain Alerts",
                NotificationManager.IMPORTANCE_HIGH,
                "Notifies you before it starts raining",
                rainSoundUri
            )
            
            // Freeze warning channel with custom sound
            val freezeSoundUri = Uri.parse("android.resource://${context.packageName}/raw/freeze_warning_short")
            createNotificationChannel(
                FREEZE_WARNING_CHANNEL_ID,
                "Freeze Warnings",
                NotificationManager.IMPORTANCE_HIGH,
                "Alerts you of potential freezing conditions",
                freezeSoundUri
            )
        }
    }

    private fun createNotificationChannel(
        channelId: String,
        channelName: String,
        importance: Int,
        description: String,
        soundUri: Uri?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                this.description = description
                enableVibration(true)
                enableLights(true)
                
                // Set custom sound if provided
                if (soundUri != null) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                }
            }
            
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createForegroundServiceNotification(): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, FOREGROUND_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rain Alert Active")
            .setContentText("Monitoring weather conditions")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    fun createPermissionRequiredNotification(): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("checkPermissions", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, RAIN_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Location Permission Required")
            .setContentText("Rain Alert needs location permission to monitor weather in your area")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Rain Alert needs location permission to check weather conditions in your area. Tap here to open settings and grant the required permissions."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    fun createRainNotification(latitude: Double, longitude: Double, weatherInfo: String = ""): Notification {
        val weatherIntent = Intent(Intent.ACTION_VIEW, Uri.parse(
            "https://forecast.weather.gov/MapClick.php?lat=$latitude&lon=$longitude"))
        val weatherPendingIntent = PendingIntent.getActivity(
            context, 0, weatherIntent, PendingIntent.FLAG_IMMUTABLE)

        // Check if custom sounds are enabled
        val useCustomSounds = runBlocking { userPreferences.useCustomSounds.first() }
        
        // Extract key weather data to show in the title or short content
        var shortWeatherInfo = "It's about to rain!"
        if (weatherInfo.isNotEmpty()) {
            val lines = weatherInfo.split("\n")
            if (lines.isNotEmpty() && lines[0].contains("Now:")) {
                shortWeatherInfo = "Rain soon - ${lines[0].replace("Now: ", "")}"
            }
        }

        val builder = NotificationCompat.Builder(context, RAIN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Rain Alert!")
            .setContentText(shortWeatherInfo)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(
                context.resources, R.drawable.ic_launcher_foreground))
            .setContentIntent(weatherPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        // Always set the sound for all API levels
        if (useCustomSounds) {
            val soundUri = Uri.parse("android.resource://${context.packageName}/raw/rain_alert_short")
            builder.setSound(soundUri)
            // Only enable vibration and lights but not default sound
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
        }

        if (weatherInfo.isNotEmpty()) {
            builder.setStyle(NotificationCompat.BigTextStyle()
                .bigText("Rain is expected in your area soon.\n\n$weatherInfo"))
        }

        return builder.build()
    }

    fun createFreezeWarningNotification(latitude: Double, longitude: Double): Notification {
        val weatherIntent = Intent(Intent.ACTION_VIEW, Uri.parse(
            "https://forecast.weather.gov/MapClick.php?lat=$latitude&lon=$longitude"))
        val pendingIntent = PendingIntent.getActivity(
            context, 3, weatherIntent, PendingIntent.FLAG_IMMUTABLE)

        // Check if custom sounds are enabled
        val useCustomSounds = runBlocking { userPreferences.useCustomSounds.first() }
        
        // Get the freeze threshold from user preferences for dynamic message
        val freezeThreshold = runBlocking { userPreferences.freezeThreshold.first() }
        
        // Get current temperature if available
        val weatherRepository = WeatherRepository(context)
        val currentTemp = weatherRepository.getCurrentTemperature()
        
        // Create a detailed notification text
        val shortInfo = if (currentTemp != null) {
            "Current: ${currentTemp.toInt()}°F, expected to drop below ${freezeThreshold.toInt()}°F"
        } else {
            "Freezing conditions expected in your area!"
        }

        val builder = NotificationCompat.Builder(context, FREEZE_WARNING_CHANNEL_ID)
            .setContentTitle("Freeze Warning!")
            .setContentText(shortInfo)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Freezing conditions expected! Temperatures will be below ${freezeThreshold.toInt()}°F " +
                        "for at least 4 hours.\n\n" + 
                        (if (currentTemp != null) "Current temperature: ${currentTemp.toInt()}°F\n\n" else "") +
                        "Protect sensitive plants, outdoor pipes, and pets.\n\n" +
                        "Tap to see detailed forecast."))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)

        // Always set the sound for all API levels
        if (useCustomSounds) {
            val soundUri = Uri.parse("android.resource://${context.packageName}/raw/freeze_warning_short")
            builder.setSound(soundUri)
            // Only enable vibration and lights but not default sound
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
        }

        return builder.build()
    }

    suspend fun sendNotification(notificationId: Int, notification: Notification, weatherInfo: String = "", temperature: Double? = null, precipitation: Int? = null, windSpeed: Double? = null) {
        // Check notification preferences before sending
        val shouldSendRainNotification = userPreferences.enableRainNotifications.first()
        val shouldSendFreezeNotification = userPreferences.enableFreezeNotifications.first()
        
        // Skip notification if disabled in settings
        if ((notificationId == AppConfig.RAIN_NOTIFICATION_ID && !shouldSendRainNotification) ||
            (notificationId == AppConfig.FREEZE_WARNING_NOTIFICATION_ID && !shouldSendFreezeNotification)) {
            Log.d("EnhancedNotificationHelper", "Notification skipped due to user preferences")
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        ) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return
        }

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("EnhancedNotificationHelper", "POST_NOTIFICATIONS permission not granted")
                return
            }
            notify(notificationId, notification)
            
            // Determine notification type and log to history
            val notificationType = when(notificationId) {
                AppConfig.RAIN_NOTIFICATION_ID -> {
                    // Log to alert history
                    alertHistoryRepository.addAlertToHistory(
                        type = AlertType.RAIN,
                        weatherConditions = weatherInfo.ifEmpty { "Rain expected in your area" },
                        temperature = temperature,
                        precipitation = precipitation,
                        windSpeed = windSpeed
                    )
                    "rain"
                }
                AppConfig.FREEZE_WARNING_NOTIFICATION_ID -> {
                    // Log to alert history
                    alertHistoryRepository.addAlertToHistory(
                        type = AlertType.FREEZE,
                        weatherConditions = weatherInfo.ifEmpty { "Freezing conditions expected" },
                        temperature = temperature,
                        precipitation = precipitation,
                        windSpeed = windSpeed
                    )
                    "freeze"
                }
                else -> "other"
            }
            
            // Log to Firebase
            firebaseLogger.logNotificationSent(notificationType)
        }
    }
}