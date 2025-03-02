package com.stoneCode.rain_alert.api

import android.content.Context
// import android.location.Location (unused)
import android.util.Log
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.data.StationObservation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service for fetching and analyzing weather data from multiple stations
 */
class MultiStationWeatherService(private val context: Context) {
    private val TAG = "MultiStationWeather"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val stationFinder = WeatherStationFinder()
    
    // Cache of nearby stations to reduce API calls
    private var cachedNearbyStations: List<WeatherStation> = emptyList()
    private var lastStationUpdateTime: Long = 0
    
    /**
     * Fetches weather observations from the nearest stations to the given location
     */
    suspend fun getNearbyStationsObservations(
        latitude: Double,
        longitude: Double
    ): Result<List<StationObservation>> = withContext(Dispatchers.IO) {
        try {
            // Get the nearest stations (using cache if available)
            val stations = getNearbyStations(latitude, longitude)
            
            if (stations.isEmpty()) {
                return@withContext Result.failure(Exception("No nearby weather stations found"))
            }
            
            // Fetch observations from each station in parallel
            val observations = stations.map { station ->
                async {
                    fetchStationObservation(station)
                }
            }.awaitAll().filterNotNull()
            
            if (observations.isEmpty()) {
                return@withContext Result.failure(Exception("Could not fetch observations from any station"))
            }
            
            Log.d(TAG, "Fetched observations from ${observations.size} stations")
            Result.success(observations)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting nearby station observations", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get nearby weather stations, using cached results if available
     */
    private suspend fun getNearbyStations(latitude: Double, longitude: Double): List<WeatherStation> {
        val currentTime = System.currentTimeMillis()
        
        // Check if cache is valid (less than STATION_REFRESH_INTERVAL_MS old)
        if (cachedNearbyStations.isNotEmpty() && 
            currentTime - lastStationUpdateTime < AppConfig.STATION_REFRESH_INTERVAL_MS) {
            Log.d(TAG, "Using cached nearby stations (${cachedNearbyStations.size})")
            return cachedNearbyStations
        }
        
        // Fetch new stations
        return try {
            val stationsResult = stationFinder.findNearestStations(latitude, longitude)
            
            if (stationsResult.isSuccess) {
                cachedNearbyStations = stationsResult.getOrNull() ?: emptyList()
                lastStationUpdateTime = currentTime
                cachedNearbyStations
            } else {
                Log.e(TAG, "Failed to find nearby stations: ${stationsResult.exceptionOrNull()?.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding nearby stations", e)
            emptyList()
        }
    }
    
    /**
     * Fetches a weather observation from a specific station
     */
    private suspend fun fetchStationObservation(station: WeatherStation): StationObservation? = 
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(station.stationUrl)
                    .header("User-Agent", "(${AppConfig.USER_AGENT}, ${AppConfig.CONTACT_EMAIL})")
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch observation from station ${station.id}: ${response.code}")
                    return@withContext null
                }
                
                val responseBody = response.body?.string()
                    ?: return@withContext null
                
                parseStationObservation(station, responseBody)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching observation from station ${station.id}", e)
                null
            }
        }
    
    /**
     * Parses a station observation from JSON response
     */
    private fun parseStationObservation(station: WeatherStation, jsonResponse: String): StationObservation? {
        return try {
            val json = JSONObject(jsonResponse)
            val properties = json.getJSONObject("properties")
            
            // Extract relevant fields
            val temperature = properties.optJSONObject("temperature")?.optDouble("value")?.let {
                // Convert from Celsius to Fahrenheit
                it * 9.0/5.0 + 32.0
            }
            
            val precipitation = properties.optJSONObject("precipitationLastHour")?.optDouble("value")?.let {
                // Convert from mm to inches
                it / 25.4
            }
            
            val relativeHumidity = properties.optJSONObject("relativeHumidity")?.optDouble("value")
            
            val windSpeed = properties.optJSONObject("windSpeed")?.optDouble("value")?.let {
                // Convert from m/s to mph
                it * 2.237
            }
            
            val windDirection = properties.optJSONObject("windDirection")?.optDouble("value")?.let {
                getWindDirectionString(it)
            }
            
            val textDescription = properties.optString("textDescription")
            val timestamp = properties.optString("timestamp")
            
            StationObservation(
                station = station,
                temperature = temperature,
                precipitationLastHour = precipitation,
                relativeHumidity = relativeHumidity,
                windSpeed = windSpeed,
                windDirection = windDirection,
                textDescription = textDescription,
                rawData = jsonResponse,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing station observation", e)
            null
        }
    }
    
    /**
     * Converts wind direction in degrees to cardinal direction string
     */
    private fun getWindDirectionString(degrees: Double): String {
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", 
                                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val index = ((degrees % 360) / 22.5).toInt()
        return directions[index]
    }
    
    /**
     * Clear the cached stations, forcing a refresh on next request
     */
    fun clearStationCache() {
        cachedNearbyStations = emptyList()
        lastStationUpdateTime = 0
    }
    
    /**
     * Analyzes multiple station observations to determine if rain is likely
     * @param observations List of station observations
     * @param rainProbabilityThreshold Minimum percentage of stations reporting rain to trigger alert
     * @return True if rain is likely based on multiple station data
     */
    fun analyzeForRain(
        observations: List<StationObservation>,
        rainProbabilityThreshold: Int
    ): Boolean {
        if (observations.isEmpty()) {
            return false
        }
        
        // Count stations reporting rain
        val rainingStations = observations.count { it.isRaining() }
        
        // Calculate percentage of stations reporting rain
        val rainingPercentage = (rainingStations.toDouble() / observations.size) * 100
        
        Log.d(TAG, "$rainingStations/${observations.size} stations report rain (${rainingPercentage.toInt()}%)")
        
        // Return true if percentage exceeds threshold
        return rainingPercentage >= rainProbabilityThreshold
    }
    
    /**
     * Analyzes station observations to check for freeze conditions
     * @param observations List of station observations
     * @param freezeThresholdF Temperature threshold for freeze warning in °F
     * @return True if any station is reporting freezing conditions
     */
    fun analyzeForFreeze(
        observations: List<StationObservation>,
        freezeThresholdF: Double
    ): Boolean {
        if (observations.isEmpty()) {
            return false
        }
        
        // Count stations reporting freezing conditions
        val freezingStations = observations.count { it.isFreezing(freezeThresholdF) }
        
        // Check if majority of stations report freezing
        val freezingPercentage = (freezingStations.toDouble() / observations.size) * 100
        
        Log.d(TAG, "$freezingStations/${observations.size} stations report freezing conditions (${freezingPercentage.toInt()}%)")
        
        // Consider it freezing if more than half of stations report freezing
        return freezingPercentage >= 50
    }
}