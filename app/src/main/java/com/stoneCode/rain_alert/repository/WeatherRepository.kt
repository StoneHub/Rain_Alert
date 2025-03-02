package com.stoneCode.rain_alert.repository

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.service.WeatherApiService
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WeatherRepository(private val context: Context) {
    private val TAG = "WeatherRepository"
    private val weatherApiService = WeatherApiService(context)
    private var lastFreezeCheckTime: Long = 0
    private var isFreezing: Boolean = false
    private var precipitationChance: Int? = null

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
                val probabilityOfPrecipitation = period.optJSONObject("probabilityOfPrecipitation")?.optInt("value", 0) ?: 0

                Log.d(TAG, "Parsing period: ${period.getString("name")}")
                Log.d(TAG, "Parsing shortForecast: $shortForecast")
                Log.d(TAG, "Probability of Precipitation: $probabilityOfPrecipitation")

                precipitationChance = probabilityOfPrecipitation

                if (shortForecast.contains("rain") && probabilityOfPrecipitation >= AppConfig.RAIN_PROBABILITY_THRESHOLD) {
                    Log.d(TAG, "Rain detected (shortForecast contains 'rain' and PoP >= ${AppConfig.RAIN_PROBABILITY_THRESHOLD})")
                    return true
                } else if (shortForecast.contains("showers") && probabilityOfPrecipitation >= AppConfig.RAIN_PROBABILITY_THRESHOLD) {
                    Log.d(TAG, "Rain detected (shortForecast contains 'showers' and PoP >= ${AppConfig.RAIN_PROBABILITY_THRESHOLD})")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing forecast for rain", e)
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

                if (temperature <= AppConfig.FREEZE_THRESHOLD_F) {
                    if (!isCurrentlyFreezing) {
                        freezeStartTime = startTime
                        isCurrentlyFreezing = true
                    }
                    val duration = startTime - freezeStartTime
                    if (duration >= 4 * 60 * 60 * 1000) { // 4 hours
                        Log.d(TAG, "Freezing conditions detected for 4 hours or more")
                        return true
                    }
                } else {
                    isCurrentlyFreezing = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing forecast for freeze", e)
        }
        return false
    }

    suspend fun getCurrentWeather(): String {
        val lastKnownLocation = getLastKnownLocation() ?: return "Location unavailable"
        val latitude = lastKnownLocation.latitude
        val longitude = lastKnownLocation.longitude

        val forecastJson = weatherApiService.getForecast(latitude, longitude)
        if (forecastJson != null) {
            try {
                val forecast = JSONObject(forecastJson)
                val periods = forecast.getJSONObject("properties").getJSONArray("periods")
                val now = Date()
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                dateFormatter.timeZone = TimeZone.getTimeZone("UTC") // NWS API uses UTC

                var currentPeriod: JSONObject? = null
                for (i in 0 until periods.length()) {
                    val period = periods.getJSONObject(i)
                    val startTime = dateFormatter.parse(period.getString("startTime"))
                    val endTime = dateFormatter.parse(period.getString("endTime"))

                    if (now.after(startTime) && now.before(endTime)) {
                        currentPeriod = period
                        break
                    }
                }

                if (currentPeriod != null) {
                    val shortForecast = currentPeriod.getString("shortForecast")
                    val temperature = currentPeriod.getInt("temperature")
                    val temperatureUnit = currentPeriod.getString("temperatureUnit")
                    var weatherInfo = "Now: $shortForecast, $temperature$temperatureUnit"

                    // Add forecast for next few hours
                    val nextPeriods = mutableListOf<String>()
                    for (i in 0 until periods.length()) {
                        val period = periods.getJSONObject(i)
                        val startTime = dateFormatter.parse(period.getString("startTime"))

                        if (startTime != null) {
                            if (startTime.after(now)) {
                                nextPeriods.add("${period.getString("name")}: ${period.getString("shortForecast")}")
                                if (nextPeriods.size == 3) break // Get next 3 periods
                            }
                        }
                    }

                    if (nextPeriods.isNotEmpty()) {
                        weatherInfo += "\n\nLater: ${nextPeriods.joinToString(", ")}"
                    }

                    return weatherInfo
                } else {
                    return "Current weather data not found in forecast"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing forecast", e)
                return "Error parsing weather data"
            }
        } else {
            return "Could not retrieve weather forecast"
        }
    }

    fun getLastKnownLocation(): Location? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
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

    fun getPrecipitationChance(): Int? {
        return precipitationChance
    }
}