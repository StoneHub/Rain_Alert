// file: app/src/main/java/com/stoneCode/rain_alert/util/NotificationHelper.kt
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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.StoneCode.rain_alert.R

class NotificationHelper(private val context: Context) {

    val CHANNEL_ID = "rain_alert_channel"
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

    fun createRainNotification(latitude: Double, longitude: Double): Notification {
        // Create a dynamic NWS URL based on latitude and longitude
        val websiteUrl = "https://forecast.weather.gov/MapClick.php?lat=$latitude&lon=$longitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rain Alert!")
            .setContentText("It's starting to rain!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    fun createFreezeWarningNotification(latitude: Double, longitude: Double): Notification {
        // Create a dynamic NWS URL based on latitude and longitude
        val websiteUrl = "https://forecast.weather.gov/MapClick.php?lat=$latitude&lon=$longitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, FREEZE_WARNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Freeze Warning!")
            .setContentText("Temperature is below 35°F for 4 hours or more!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Set the PendingIntent
            .setAutoCancel(true) // Automatically dismiss the notification when clicked
            .build()
        Log.d("NotificationHelper", "Freeze warning notification created")
        return notification
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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