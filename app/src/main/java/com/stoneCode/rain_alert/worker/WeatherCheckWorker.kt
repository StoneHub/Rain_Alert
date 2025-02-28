package com.stoneCode.rain_alert.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.repository.WeatherRepository
import com.stoneCode.rain_alert.util.EnhancedNotificationHelper
import java.util.concurrent.TimeUnit

/**
 * This worker provides an alternative way to check weather in the background
 * that is more battery efficient than keeping a service running continuously.
 * It's set up for future implementation but not currently used by the main app flow.
 */
class WeatherCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "WeatherCheckWorker"
    private val weatherRepository = WeatherRepository(appContext)
    private val notificationHelper = EnhancedNotificationHelper(appContext)

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting weather check work")

        try {
            // Check for rain
            val isRaining = weatherRepository.checkForRain()
            if (isRaining) {
                val location = weatherRepository.getLastKnownLocation()
                if (location != null) {
                    val weatherInfo = weatherRepository.getCurrentWeather()
                    val notification = notificationHelper.createRainNotification(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        weatherInfo = weatherInfo
                    )

                    notificationHelper.sendNotification(
                        AppConfig.RAIN_NOTIFICATION_ID,
                        notification
                    )

                    Log.d(TAG, "Rain notification sent")
                }
            }

            // Check for freeze warning
            val isFreezing = weatherRepository.checkForFreezeWarning()
            if (isFreezing) {
                val location = weatherRepository.getLastKnownLocation()
                if (location != null) {
                    val notification = notificationHelper.createFreezeWarningNotification(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )

                    notificationHelper.sendNotification(
                        AppConfig.FREEZE_WARNING_NOTIFICATION_ID,
                        notification
                    )

                    Log.d(TAG, "Freeze warning notification sent")
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during weather check", e)
            return Result.retry()
        }
    }

    companion object {
        private const val WEATHER_CHECK_WORK_NAME = "weather_check_work"

        /**
         * Schedule periodic weather checks using WorkManager.
         * This is a more battery-efficient alternative to the foreground service.
         */
        fun scheduleWeatherChecks(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Use the correct format for PeriodicWorkRequestBuilder
            val workRequest = PeriodicWorkRequestBuilder<WeatherCheckWorker>(
                AppConfig.RAIN_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WEATHER_CHECK_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            Log.d("WeatherCheckWorker", "Scheduled periodic weather checks")
        }

        /**
         * Cancel all scheduled weather checks.
         */
        fun cancelWeatherChecks(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WEATHER_CHECK_WORK_NAME)
            Log.d("WeatherCheckWorker", "Cancelled periodic weather checks")
        }
    }
}