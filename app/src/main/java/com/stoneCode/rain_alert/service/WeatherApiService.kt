package com.stoneCode.rain_alert.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class WeatherApiService {

    private val client = OkHttpClient()

    suspend fun getForecast(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        val pointsUrl = "https://api.weather.gov/points/$latitude,$longitude"
        val pointsRequest = Request.Builder()
            .url(pointsUrl)
            .header("User-Agent", "(RainAlertApp, contact@yourdomain.com)")
            .build()

        try {
            val pointsResponse = client.newCall(pointsRequest).execute()
            if (!pointsResponse.isSuccessful) {
                Log.e("WeatherApiService", "Points API request failed: ${pointsResponse.code}")
                return@withContext null
            }
            val pointsResponseBody = pointsResponse.body?.string() ?: return@withContext null
            val forecastUrl = JSONObject(pointsResponseBody).getJSONObject("properties").getString("forecast")

            val forecastRequest = Request.Builder()
                .url(forecastUrl)
                .header("User-Agent", "(RainAlertApp, contact@yourdomain.com)")
                .build()

            val forecastResponse = client.newCall(forecastRequest).execute()
            if (!forecastResponse.isSuccessful) {
                Log.e("WeatherApiService", "Forecast API request failed: ${forecastResponse.code}")
                return@withContext null
            }

            forecastResponse.body?.string()
        } catch (e: IOException) {
            Log.e("WeatherApiService", "Error during API call", e)
            null
        }
    }

    suspend fun getForecastGridData(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        val pointsUrl = "https://api.weather.gov/points/$latitude,$longitude"
        val pointsRequest = Request.Builder()
            .url(pointsUrl)
            .header("User-Agent", "(RainAlertApp, contact@yourdomain.com)")
            .build()

        try {
            val pointsResponse = client.newCall(pointsRequest).execute()
            if (!pointsResponse.isSuccessful) {
                Log.e("WeatherApiService", "Points API request failed: ${pointsResponse.code}")
                return@withContext null
            }
            val pointsResponseBody = pointsResponse.body?.string() ?: return@withContext null
            val forecastHourlyUrl = JSONObject(pointsResponseBody).getJSONObject("properties").getString("forecastHourly")

            val forecastHourlyRequest = Request.Builder()
                .url(forecastHourlyUrl)
                .header("User-Agent", "(RainAlertApp, contact@yourdomain.com)")
                .build()

            val forecastHourlyResponse = client.newCall(forecastHourlyRequest).execute()
            if (!forecastHourlyResponse.isSuccessful) {
                Log.e("WeatherApiService", "Forecast Hourly API request failed: ${forecastHourlyResponse.code}")
                return@withContext null
            }

            forecastHourlyResponse.body?.string()
        } catch (e: IOException) {
            Log.e("WeatherApiService", "Error during API call", e)
            null
        }
    }
}