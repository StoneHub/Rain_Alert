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
import android.media.AudioAttributes
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
    val FREEZE_WARNING_CHANNEL_ID = "freeze_warning_channel"
    val FOREGROUND_SERVICE_CHANNEL_ID = "foreground_service_channel" // New channel ID

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Rain Alert Channel
            val rainSoundUri =
                Uri.parse("android.resource://${context.packageName}/${R.raw.rain_alert}")
            Log.d("NotificationHelper", "Rain channel sound URI: $rainSoundUri")

            val rainChannel = NotificationChannel(
                CHANNEL_ID,
                "Rain Alert Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for rain alerts"
                setSound(
                    rainSoundUri, AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            Log.d(
                "NotificationHelper",
                "Rain channel created: ${rainChannel.id}, Sound: ${rainChannel.sound}"
            )

            // Freeze Warning Channel
            val freezeSoundUri =
                Uri.parse("android.resource://${context.packageName}/${R.raw.freeze_warning}")
            Log.d("NotificationHelper", "Freeze channel sound URI: $freezeSoundUri")

            val freezeChannel = NotificationChannel(
                FREEZE_WARNING_CHANNEL_ID,
                "Freeze Warning Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for freeze warnings"
                setSound(
                    freezeSoundUri, AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            Log.d(
                "NotificationHelper",
                "Freeze channel created: ${freezeChannel.id}, Sound: ${freezeChannel.sound}"
            )

            // Foreground Service Channel
            val foregroundServiceChannel = NotificationChannel(
                FOREGROUND_SERVICE_CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW // No sound or vibration
            ).apply {
                description = "Channel for the foreground service notification"
            }
            Log.d(
                "NotificationHelper",
                "Foreground service channel created: ${foregroundServiceChannel.id}"
            )

            // Register channels with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(rainChannel)
            notificationManager.createNotificationChannel(freezeChannel)
            notificationManager.createNotificationChannel(foregroundServiceChannel)
        }
    }

    fun createRainNotification(latitude: Double, longitude: Double): Notification {
        val websiteUrl = "https://forecast.weather.gov/MapClick.php?lat=$latitude&lon=$longitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // No need to set sound here, it's defined in the channel
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
        val websiteUrl = "https://forecast.weather.gov/MapClick.php?lat=$latitude&lon=$longitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // No need to set sound here, it's defined in the channel
        return NotificationCompat.Builder(context, FREEZE_WARNING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Freeze Warning!")
            .setContentText("Temperature is below 35°F for 4 hours or more!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
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
            Log.d("NotificationHelper", "Notification sent: id=$notificationId, channel=${notification.channelId}")
        }
    }
}