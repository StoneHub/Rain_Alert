package com.StoneCode.rain_alert.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.StoneCode.rain_alert.R
import android.Manifest

class NotificationHelper(private val context: Context) {

    private val CHANNEL_ID = "rain_alert_channel"
    private val FREEZE_WARNING_CHANNEL_ID = "freeze_warning_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val rainChannel = NotificationChannel(
                CHANNEL_ID,
                "Rain Alert Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for rain alerts"
            }

            val freezeChannel = NotificationChannel(
                FREEZE_WARNING_CHANNEL_ID,
                "Freeze Warning Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for freeze warnings"
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(rainChannel)
            notificationManager.createNotificationChannel(freezeChannel)
            Log.d("NotificationHelper", "Notification channels created")
        }
    }

    fun createNotification(title: String, content: String): Notification {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        Log.d("NotificationHelper", "Notification created: $title")
        return notification
    }

    fun createFreezeWarningNotification(): Notification {
        val notification = NotificationCompat.Builder(context, FREEZE_WARNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Freeze Warning!")
            .setContentText("Temperature is below 35°F for 4 hours or more!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        Log.d("NotificationHelper", "Freeze warning notification created")
        return notification
    }

    fun sendNotification(notificationId: Int, notification: Notification) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("NotificationHelper", "Missing permission: ${Manifest.permission.POST_NOTIFICATIONS}")
            return
        }
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, notification)
            Log.d("NotificationHelper", "Notification sent: id=$notificationId")
        }
    }
}