package com.StoneCode.rain_alert.service

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.StoneCode.rain_alert.repository.WeatherRepository
import com.StoneCode.rain_alert.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class RainService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var notificationHelper: NotificationHelper
    private val RAIN_CHECK_INTERVAL = 15 * 60 * 1000L // 15 minutes
    private val FREEZE_CHECK_INTERVAL = 60 * 60 * 1000L // 1 hour
    private val NOTIFICATION_ID = 1
    private val FOREGROUND_SERVICE_ID = 2
    private val FREEZE_WARNING_NOTIFICATION_ID = 3

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        weatherRepository = WeatherRepository(this)
        notificationHelper = NotificationHelper(this)
        isRunning = true
        Log.d("RainService", "RainService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RainService", "onStartCommand called with action: ${intent?.action}")

        startForeground(FOREGROUND_SERVICE_ID, createForegroundNotification())

        when (intent?.action) {
            "START_RAIN_CHECK" -> {
                startRainCheck()
                startFreezeCheck()
            }
            "SIMULATE_RAIN" -> simulateRain()
            "STOP_SERVICE" -> stopSelf()
            else -> {
                startRainCheck()
                startFreezeCheck()
            }
        }
        scheduleAlarm()
        return START_STICKY
    }

    private fun createForegroundNotification(): Notification {
        val notification = notificationHelper.createNotification(
            "Rain Alert Service",
            "Monitoring weather for rain..."
        )
        Log.d("RainService", "Foreground notification created")
        return notification
    }

    private fun startRainCheck() {
        serviceScope.launch {
            while (true) {
                if (isRaining()) {
                    sendRainNotification()
                }
                delay(RAIN_CHECK_INTERVAL)
                Log.d("RainService", "Rain check performed")
            }
        }
    }

    private fun startFreezeCheck() {
        serviceScope.launch {
            while (true) {
                if (isFreezing()) {
                    sendFreezeWarningNotification()
                }
                delay(FREEZE_CHECK_INTERVAL)
                Log.d("RainService", "Freeze check performed")
            }
        }
    }

    private fun isRaining(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val isRaining = weatherRepository.checkForRain()
            Log.d("RainService", "isRaining check: $isRaining")
            return isRaining
        } else {
            Log.w("RainService", "Location permission not granted")
            return false
        }
    }

    private fun isFreezing(): Boolean {
        val isFreezing = weatherRepository.checkForFreezeWarning()
        Log.d("RainService", "isFreezing check: $isFreezing")
        return isFreezing
    }

    private fun simulateRain() {
        serviceScope.launch {
            sendRainNotification()
            Log.d("RainService", "Rain simulation triggered")
        }
    }

    private fun sendRainNotification() {
        val notification =
            notificationHelper.createNotification("Rain Alert!", "It's starting to rain!")
        notificationHelper.sendNotification(NOTIFICATION_ID, notification)
        Log.d("RainService", "Rain notification sent")
    }

    private fun sendFreezeWarningNotification() {
        val notification = notificationHelper.createFreezeWarningNotification()
        notificationHelper.sendNotification(FREEZE_WARNING_NOTIFICATION_ID, notification)
        Log.d("RainService", "Freeze warning notification sent")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        isRunning = false
        Log.d("RainService", "RainService destroyed")
    }

    private fun scheduleAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = "com.stonecode.rain_alert.RAIN_ALARM"
        }
        val pendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w("RainService", "Exact alarm permission not granted")

            val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(settingsIntent)

            return
        }

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d("RainService", "Alarm scheduled for: ${calendar.time}")
    }
}