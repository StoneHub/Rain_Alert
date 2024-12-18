// file: app/src/main/java/com/stoneCode/rain_alert/repository/WeatherRepository.kt
package com.stoneCode.rain_alert.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.stoneCode.rain_alert.service.WeatherApiService
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WeatherRepository(private val context: Context) {

    private val weatherApiService = WeatherApiService()
    private var lastFreezeCheckTime: Long = 0
    private var isFreezing: Boolean = false
    private val freezeThreshold = 35.0 // 35°F in Fahrenheit

    suspend fun checkForRain(): Boolean {
        val lastKnownLocation = getLastKnownLocation() ?: return false
        val latitude = lastKnownLocation.latitude
        val longitude = lastKnownLocation.longitude

        val forecastJson = weatherApiService.getForecast(latitude, longitude) ?: return false
        return parseForecastForRain(forecastJson)
    }

    suspend fun checkForFreezeWarning(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFreezeCheckTime < 4 * 60 * 60 * 1000) {
            return isFreezing // Return cached value if checked within the last 4 hours
        }

        val lastKnownLocation = getLastKnownLocation() ?: return false
        val latitude = lastKnownLocation.latitude
        val longitude = lastKnownLocation.longitude

        val forecastHourlyJson = weatherApiService.getForecastGridData(latitude, longitude) ?: return false
        isFreezing = parseForecastForFreeze(forecastHourlyJson)
        lastFreezeCheckTime = currentTime

        return isFreezing
    }

    private fun parseForecastForRain(forecastJson: String): Boolean {
        try {
            val forecast = JSONObject(forecastJson)
            val periods = forecast.getJSONObject("properties").getJSONArray("periods")

            for (i in 0 until periods.length()) {
                val period = periods.getJSONObject(i)
                val shortForecast = period.getString("shortForecast").lowercase(Locale.getDefault())
                if (shortForecast.contains("rain")) {
                    Log.d("WeatherRepository", "Rain detected in forecast: $shortForecast")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error parsing forecast for rain", e)
        }
        return false
    }

    private fun parseForecastForFreeze(forecastHourlyJson: String): Boolean {
        try {
            val forecast = JSONObject(forecastHourlyJson)
            val periods = forecast.getJSONObject("properties").getJSONArray("periods")

            var freezeStartTime: Long = 0
            var isCurrentlyFreezing = false
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            for (i in 0 until periods.length()) {
                val period = periods.getJSONObject(i)
                val temperature = period.getDouble("temperature")
                val startTimeString = period.getString("startTime")
                val startTime = dateFormat.parse(startTimeString)?.time ?: continue

                if (temperature <= freezeThreshold) {
                    if (!isCurrentlyFreezing) {
                        freezeStartTime = startTime
                        isCurrentlyFreezing = true
                    }
                    val duration = startTime - freezeStartTime
                    if (duration >= 4 * 60 * 60 * 1000) { // 4 hours
                        Log.d("WeatherRepository", "Freezing conditions detected for 4 hours or more")
                        return true
                    }
                } else {
                    isCurrentlyFreezing = false
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error parsing forecast for freeze", e)
        }
        return false
    }

    private fun getLastKnownLocation(): Location? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("WeatherRepository", "Location permission not granted")
            return null
        }

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var bestLocation: Location? = null
        val providers = locationManager.getProviders(true)

        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                bestLocation = location
            }
        }
        return bestLocation
    }
}