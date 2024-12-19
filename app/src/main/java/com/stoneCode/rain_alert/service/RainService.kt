package com.stoneCode.rain_alert.service

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
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
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.StoneCode.rain_alert.R
import com.stoneCode.rain_alert.MainActivity
import com.stoneCode.rain_alert.repository.WeatherRepository
import com.stoneCode.rain_alert.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

interface ServiceStatusListener {
    fun onServiceStatusChanged(isRunning: Boolean)
}

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
        private var statusListener: ServiceStatusListener? = null

        fun setServiceStatusListener(listener: ServiceStatusListener) {
            statusListener = listener
        }

        fun clearServiceStatusListener() {
            statusListener = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        weatherRepository = WeatherRepository(this)
        notificationHelper = NotificationHelper(this)
        isRunning = true
        statusListener?.onServiceStatusChanged(true)
        Log.d("RainService", "RainService created")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RainService", "onStartCommand called with action: ${intent?.action}")

        startForeground(FOREGROUND_SERVICE_ID, createForegroundNotification())

        when (intent?.action) {
            "START_RAIN_CHECK" -> {
                startRainCheck()
                startFreezeCheck()
            }
            "SIMULATE_RAIN" -> simulateRain()
            "SIMULATE_FREEZE" -> {
                Log.d("RainService", "SIMULATE_FREEZE intent received")
                simulateFreeze()
            }
            "STOP_SERVICE" -> {
                Log.d("RainService", "STOP_SERVICE intent received")
                stopSelf() // Stop the service
            }
            else -> {
                startRainCheck()
                startFreezeCheck()
            }
        }
        scheduleAlarm()
        return START_STICKY
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, notificationHelper.FOREGROUND_SERVICE_CHANNEL_ID) // Use the new channel ID
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rain Alert Service")
            .setContentText("Monitoring weather for rain...")
            .setPriority(NotificationCompat.PRIORITY_LOW) // Also set to low priority
            .setContentIntent(pendingIntent)
            .build()

        Log.d("RainService", "Foreground notification created with intent to open MainActivity")
        return notification
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

    private suspend fun isRaining(): Boolean {
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

    private suspend fun isFreezing(): Boolean {
        val isFreezing = weatherRepository.checkForFreezeWarning()
        Log.d("RainService", "isFreezing check: $isFreezing")
        return isFreezing
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun simulateRain() {
        serviceScope.launch {
            sendRainNotification()
            Log.d("RainService", "Rain simulation triggered")
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun simulateFreeze() {
        sendFreezeWarningNotification()
        Log.d("RainService", "Freeze simulation triggered")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun sendRainNotification() {
        val location = weatherRepository.getLastKnownLocation()
        if (location != null) {
            val notification = notificationHelper.createRainNotification(location.latitude, location.longitude)
            notificationHelper.sendNotification(NOTIFICATION_ID, notification)
            Log.d("RainService", "Rain notification sent for ${location.latitude}, ${location.longitude}")
        } else {
            Log.w("RainService", "Could not get location for rain notification")
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun sendFreezeWarningNotification() {
        val location = weatherRepository.getLastKnownLocation()
        if (location != null) {
            val notification = notificationHelper.createFreezeWarningNotification(location.latitude, location.longitude)
            notificationHelper.sendNotification(FREEZE_WARNING_NOTIFICATION_ID, notification)
            Log.d("RainService", "Freeze warning notification sent for ${location.latitude}, ${location.longitude}")
        } else {
            Log.w("RainService", "Could not get location for freeze warning notification")
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        isRunning = false
        statusListener?.onServiceStatusChanged(false)
        Log.d("RainService", "RainService destroyed")

        // Get the NotificationManager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cancel all notifications
        notificationManager.cancelAll()
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