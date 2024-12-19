package com.stoneCode.rain_alert.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.StoneCode.rain_alert.R

class NotificationHelper(private val context: Context) {

    val FOREGROUND_SERVICE_CHANNEL_ID = "foreground_service_channel"
    private val RAIN_NOTIFICATION_CHANNEL_ID = "rain_notification_channel"
    private val FREEZE_WARNING_CHANNEL_ID = "freeze_warning_channel"

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(FOREGROUND_SERVICE_CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_LOW)
            createNotificationChannel(RAIN_NOTIFICATION_CHANNEL_ID, "Rain Notifications", NotificationManager.IMPORTANCE_HIGH)
            createNotificationChannel(FREEZE_WARNING_CHANNEL_ID, "Freeze Warning Notifications", NotificationManager.IMPORTANCE_HIGH)
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "$channelName for Rain Alert"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createRainNotification(latitude: Double, longitude: Double): Notification {
        val notificationIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://forecast.weather.gov/MapClick.php?lat=$latitude&lon=$longitude"))
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, RAIN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Rain Alert!")
            .setContentText("It's about to rain! Tap to view details.")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    fun createFreezeWarningNotification(latitude: Double, longitude: Double): Notification {
        val notificationIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://forecast.weather.gov/MapClick.php?lat=$latitude&lon=$longitude"))
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, FREEZE_WARNING_CHANNEL_ID)
            .setContentTitle("Freeze Warning!")
            .setContentText("Freezing conditions expected! Tap to view details.")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    fun sendNotification(notificationId: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        ) {
            // take user to settings to enable notifications for this app.
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
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
                Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted")
                return
            }
            notify(notificationId, notification)
        }
    }
}