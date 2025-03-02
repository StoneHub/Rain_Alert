package com.stoneCode.rain_alert.service

import android.content.Context
import android.util.Log
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class WeatherApiService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
        
    private val firebaseLogger = FirebaseLogger.getInstance()

    suspend fun getForecast(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        try {
            val pointsUrl = "https://api.weather.gov/points/$latitude,$longitude"
            val pointsRequest = Request.Builder()
                .url(pointsUrl)
                .header("User-Agent", "(${AppConfig.USER_AGENT}, ${AppConfig.CONTACT_EMAIL})")
                .build()

            val startTime = System.currentTimeMillis()
            val pointsResponse = client.newCall(pointsRequest).execute()
            val responseTime = System.currentTimeMillis() - startTime
            
            if (!pointsResponse.isSuccessful) {
                val errorMessage = "Points API request failed: ${pointsResponse.code}"
                Log.e("WeatherApiService", errorMessage)
                firebaseLogger.logApiStatus(false, errorMessage, "points")
                return@withContext null
            }
            
            // Log successful API call
            firebaseLogger.logApiStatus(true, endpoint = "points", responseTime = responseTime)

            val pointsResponseBody = pointsResponse.body?.string() ?: return@withContext null
            val forecastUrl = JSONObject(pointsResponseBody).getJSONObject("properties").getString("forecast")

            val forecastRequest = Request.Builder()
                .url(forecastUrl)
                .header("User-Agent", "(${AppConfig.USER_AGENT}, ${AppConfig.CONTACT_EMAIL})")
                .build()

            val forecastStartTime = System.currentTimeMillis()
            val forecastResponse = client.newCall(forecastRequest).execute()
            val forecastResponseTime = System.currentTimeMillis() - forecastStartTime
            
            if (!forecastResponse.isSuccessful) {
                val errorMessage = "Forecast API request failed: ${forecastResponse.code}"
                Log.e("WeatherApiService", errorMessage)
                firebaseLogger.logApiStatus(false, errorMessage, "forecast")
                return@withContext null
            }
            
            // Log successful API call
            firebaseLogger.logApiStatus(true, endpoint = "forecast", responseTime = forecastResponseTime)

            forecastResponse.body?.string()
        } catch (e: IOException) {
            Log.e("WeatherApiService", "Error during API call", e)
            firebaseLogger.logApiStatus(false, e.message, "getForecast")
            null
        }
    }

    suspend fun getForecastGridData(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        try {
            val pointsUrl = "https://api.weather.gov/points/$latitude,$longitude"
            val pointsRequest = Request.Builder()
                .url(pointsUrl)
                .header("User-Agent", "(${AppConfig.USER_AGENT}, ${AppConfig.CONTACT_EMAIL})")
                .build()

            val startTime = System.currentTimeMillis()
            val pointsResponse = client.newCall(pointsRequest).execute()
            val responseTime = System.currentTimeMillis() - startTime
            
            if (!pointsResponse.isSuccessful) {
                val errorMessage = "Points API request failed: ${pointsResponse.code}"
                Log.e("WeatherApiService", errorMessage)
                firebaseLogger.logApiStatus(false, errorMessage, "points")
                return@withContext null
            }
            
            // Log successful API call
            firebaseLogger.logApiStatus(true, endpoint = "points", responseTime = responseTime)

            val pointsResponseBody = pointsResponse.body?.string() ?: return@withContext null
            val forecastHourlyUrl = JSONObject(pointsResponseBody).getJSONObject("properties").getString("forecastHourly")

            val forecastHourlyRequest = Request.Builder()
                .url(forecastHourlyUrl)
                .header("User-Agent", "(${AppConfig.USER_AGENT}, ${AppConfig.CONTACT_EMAIL})")
                .build()

            val hourlyStartTime = System.currentTimeMillis()
            val forecastHourlyResponse = client.newCall(forecastHourlyRequest).execute()
            val hourlyResponseTime = System.currentTimeMillis() - hourlyStartTime
            
            if (!forecastHourlyResponse.isSuccessful) {
                val errorMessage = "Forecast Hourly API request failed: ${forecastHourlyResponse.code}"
                Log.e("WeatherApiService", errorMessage)
                firebaseLogger.logApiStatus(false, errorMessage, "forecastHourly")
                return@withContext null
            }
            
            // Log successful API call
            firebaseLogger.logApiStatus(true, endpoint = "forecastHourly", responseTime = hourlyResponseTime)

            forecastHourlyResponse.body?.string()
        } catch (e: IOException) {
            Log.e("WeatherApiService", "Error during API call", e)
            firebaseLogger.logApiStatus(false, e.message, "getForecastGridData")
            null
        }
    }
}