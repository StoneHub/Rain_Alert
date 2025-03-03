package com.stoneCode.rain_alert.service

import android.Manifest
import android.app.AlarmManager
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
import androidx.core.content.ContextCompat
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.data.UserPreferences
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.repository.WeatherRepository
import com.stoneCode.rain_alert.util.EnhancedNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

interface ServiceStatusListener {
    fun onServiceStatusChanged(isRunning: Boolean)
}

class RainService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var notificationHelper: EnhancedNotificationHelper
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseLogger: FirebaseLogger

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
        notificationHelper = EnhancedNotificationHelper(this)
        userPreferences = UserPreferences(this)
        firebaseLogger = FirebaseLogger.getInstance()
        
        isRunning = true
        statusListener?.onServiceStatusChanged(true)
        
        // Log service started to Firebase
        firebaseLogger.logServiceStatusChanged(true)
        
        Log.d("RainService", "RainService created")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RainService", "onStartCommand called with action: ${intent?.action}")
        
        // Always start as foreground with a notification, even if we'll exit soon due to permissions
        startForeground(AppConfig.FOREGROUND_SERVICE_ID, notificationHelper.createForegroundServiceNotification())
        
        // Check if we have the required permissions for foreground service with location type
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("RainService", "Location permissions not granted, showing permissions required notification and stopping service")
            
            // Show a notification about missing permissions instead of just stopping
            val permissionNotification = notificationHelper.createPermissionRequiredNotification()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(AppConfig.PERMISSION_NOTIFICATION_ID, permissionNotification)
            
            // Stop the service after showing the notification
            stopSelf()
            return START_NOT_STICKY
        }
        
        // Normal service operation continues here

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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun startRainCheck() {
        serviceScope.launch {
            // Get current rain check interval from preferences
            val rainCheckInterval = userPreferences.rainCheckInterval.first()
            
            while (true) {
                if (isRaining()) {
                    sendRainNotification()
                }
                delay(rainCheckInterval)
                Log.d("RainService", "Rain check performed")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun startFreezeCheck() {
        serviceScope.launch {
            // Get current freeze check interval from preferences
            val freezeCheckInterval = userPreferences.freezeCheckInterval.first()
            
            while (true) {
                if (isFreezing()) {
                    sendFreezeWarningNotification()
                }
                delay(freezeCheckInterval)
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
            
            // Log weather check to Firebase
            val precipitationChance = weatherRepository.getPrecipitationChance()
            firebaseLogger.logWeatherCheck(
                isRaining = isRaining,
                isFreezing = false,
                precipitationChance = precipitationChance,
                temperature = null
            )
            
            return isRaining
        } else {
            Log.w("RainService", "Location permission not granted")
            return false
        }
    }

    private suspend fun isFreezing(): Boolean {
        val isFreezing = weatherRepository.checkForFreezeWarning()
        Log.d("RainService", "isFreezing check: $isFreezing")
        
        // Log weather check to Firebase
        firebaseLogger.logWeatherCheck(
            isRaining = false,
            isFreezing = isFreezing,
            precipitationChance = null,
            temperature = userPreferences.freezeThreshold.first()
        )
        
        return isFreezing
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun simulateRain() {
        serviceScope.launch {
            firebaseLogger.logSimulation("rain", true)
            sendRainNotification()
            Log.d("RainService", "Rain simulation triggered")
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun simulateFreeze() {
        serviceScope.launch {
            firebaseLogger.logSimulation("freeze", true)
            sendFreezeWarningNotification()
            Log.d("RainService", "Freeze simulation triggered")
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun sendRainNotification() {
        val location = weatherRepository.getLastKnownLocation()
        if (location != null) {
            val weatherInfo = weatherRepository.getCurrentWeather()
            val precipitationChance = weatherRepository.getPrecipitationChance()
            
            val notification = notificationHelper.createRainNotification(
                location.latitude,
                location.longitude,
                weatherInfo
            )
            
            notificationHelper.sendNotification(
                AppConfig.RAIN_NOTIFICATION_ID,
                notification,
                weatherInfo = weatherInfo,
                precipitation = precipitationChance
            )
            
            Log.d("RainService", "Enhanced rain notification sent")
        } else {
            Log.w("RainService", "Could not get location for rain notification")
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun sendFreezeWarningNotification() {
        val location = weatherRepository.getLastKnownLocation()
        if (location != null) {
            val weatherInfo = weatherRepository.getCurrentWeather()
            val freezeThreshold = userPreferences.freezeThreshold.first()

            val notification = notificationHelper.createFreezeWarningNotification(
                location.latitude,
                location.longitude
            )
            
            notificationHelper.sendNotification(
                AppConfig.FREEZE_WARNING_NOTIFICATION_ID,
                notification,
                weatherInfo = weatherInfo,
                temperature = freezeThreshold
            )
            
            Log.d("RainService", "Enhanced freeze warning notification sent")
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
        
        // Log service stopped to Firebase
        firebaseLogger.logServiceStatusChanged(false)
        
        Log.d("RainService", "RainService destroyed")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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