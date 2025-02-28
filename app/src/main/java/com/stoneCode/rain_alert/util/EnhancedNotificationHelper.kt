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
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.StoneCode.rain_alert.R
import com.stoneCode.rain_alert.MainActivity
import com.stoneCode.rain_alert.data.AppConfig

class EnhancedNotificationHelper(private val context: Context) {

    val FOREGROUND_SERVICE_CHANNEL_ID = "foreground_service_channel"
    private val RAIN_NOTIFICATION_CHANNEL_ID = "rain_notification_channel"
    private val FREEZE_WARNING_CHANNEL_ID = "freeze_warning_channel"

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                FOREGROUND_SERVICE_CHANNEL_ID,
                "Foreground Service",
                NotificationManager.IMPORTANCE_LOW,
                "Enables background weather monitoring"
            )
            createNotificationChannel(
                RAIN_NOTIFICATION_CHANNEL_ID,
                "Rain Alerts",
                NotificationManager.IMPORTANCE_HIGH,
                "Notifies you before it starts raining"
            )
            createNotificationChannel(
                FREEZE_WARNING_CHANNEL_ID,
                "Freeze Warnings",
                NotificationManager.IMPORTANCE_HIGH,
                "Alerts you of potential freezing conditions"
            )
        }
    }

    private fun createNotificationChannel(
        channelId: String,
        channelName: String,
        importance: Int,
        description: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                this.description = description
                enableVibration(true)
                enableLights(true)
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

    fun createRainNotification(latitude: Double, longitude: Double, weatherInfo: String = ""): Notification {
        val weatherIntent = Intent(Intent.ACTION_VIEW, Uri.parse(
            "https://forecast.weather.gov/MapClick.php?lat=$latitude&lon=$longitude"))
        val weatherPendingIntent = PendingIntent.getActivity(
            context, 0, weatherIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, RAIN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Rain Alert!")
            .setContentText("It's about to rain! Tap for details.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(
                context.resources, R.drawable.ic_launcher_foreground))
            .setContentIntent(weatherPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

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

        return NotificationCompat.Builder(context, FREEZE_WARNING_CHANNEL_ID)
            .setContentTitle("Freeze Warning!")
            .setContentText("Freezing conditions expected in your area!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Freezing conditions expected! Temperatures will be below 35°F " +
                        "for at least 4 hours.\n\nTap to see detailed forecast."))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
    }

    fun sendNotification(notificationId: Int, notification: Notification) {
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
        }
    }
}