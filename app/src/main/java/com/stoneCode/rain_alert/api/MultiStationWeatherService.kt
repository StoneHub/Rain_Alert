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
     * 
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @param forceRefresh If true, forces a refresh of stations ignoring cache
     * @return Result containing list of station observations or an error
     */
    suspend fun getNearbyStationsObservations(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false
    ): Result<List<StationObservation>> = withContext(Dispatchers.IO) {
        try {
            // Get the nearest stations (using cache if available, unless forceRefresh is true)
            val stations = getNearbyStations(latitude, longitude, forceRefresh)
            
            if (stations.isEmpty()) {
                return@withContext Result.failure(Exception("No nearby weather stations found"))
            }
            
            Log.d(TAG, "Found ${stations.size} nearby stations to fetch observations from")
            
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
     * Get nearby weather stations, using cached results if available and not forced to refresh
     * 
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @param forceRefresh If true, ignore cache and fetch fresh data
     * @return List of nearby weather stations
     */
    private suspend fun getNearbyStations(
        latitude: Double, 
        longitude: Double, 
        forceRefresh: Boolean = false
    ): List<WeatherStation> {
        val currentTime = System.currentTimeMillis()
        
        // Check if cache is valid and we're not forced to refresh
        if (!forceRefresh && 
            cachedNearbyStations.isNotEmpty() && 
            currentTime - lastStationUpdateTime < AppConfig.STATION_REFRESH_INTERVAL_MS) {
            Log.d(TAG, "Using cached nearby stations (${cachedNearbyStations.size})")
            return cachedNearbyStations
        }
        
        // If forcing refresh or cache is invalid, log the reason
        if (forceRefresh) {
            Log.d(TAG, "Force refreshing nearby stations")
        } else {
            Log.d(TAG, "Cache expired, fetching fresh nearby stations")
        }
        
        // Fetch new stations
        return try {
            val stationsResult = stationFinder.findNearestStations(
                latitude = latitude, 
                longitude = longitude,
                limit = 10 // Increased limit to give more options for selection
            )
            
            if (stationsResult.isSuccess) {
                cachedNearbyStations = stationsResult.getOrNull() ?: emptyList()
                lastStationUpdateTime = currentTime
                Log.d(TAG, "Updated station cache with ${cachedNearbyStations.size} stations")
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
     * @return True if rain is likely based on multiple station data, weighted by distance
     */
    fun analyzeForRain(
        observations: List<StationObservation>,
        rainProbabilityThreshold: Int
    ): Boolean {
        return analyzeForRainWithDetails(observations, rainProbabilityThreshold).first
    }
    
    /**
     * Analyzes multiple station observations to determine if rain is likely,
     * and returns both the result and weighted percentage
     * 
     * @param observations List of station observations
     * @param rainProbabilityThreshold Minimum percentage of stations reporting rain to trigger alert
     * @return Pair<Boolean, Double> of rain likelihood result and weighted percentage
     */
    fun analyzeForRainWithDetails(
        observations: List<StationObservation>,
        rainProbabilityThreshold: Int
    ): Pair<Boolean, Double> {
        if (observations.isEmpty()) {
            return Pair(false, 0.0)
        }
        
        // Get stations reporting rain
        val rainingStations = observations.filter { it.isRaining() }
        
        if (rainingStations.isEmpty()) {
            return Pair(false, 0.0)
        }
        
        // Calculate weighted probability based on distance
        // Closer stations have more influence than distant ones
        val totalWeight = calculateDistanceWeights(observations)
        val rainingWeight = calculateDistanceWeights(rainingStations)
        
        // Calculate weighted percentage
        val rainingPercentage = (rainingWeight / totalWeight) * 100
        
        Log.d(TAG, "${rainingStations.size}/${observations.size} stations report rain, weighted percentage: ${rainingPercentage.toInt()}%")
        
        // Log detailed station information for debugging
        observations.forEach { station ->
            val isRaining = if (station.isRaining()) "RAIN" else "NO RAIN"
            val weight = 1.0 / (station.station.distance ?: 1.0)
            Log.d(TAG, "Station: ${station.station.name}, Distance: ${String.format("%.1f", station.station.distance)} km, Status: $isRaining, Weight: ${String.format("%.4f", weight)}")
        }
        
        // Return pair of result and weighted percentage
        return Pair(rainingPercentage >= rainProbabilityThreshold, rainingPercentage)
    }
    
    /**
     * Calculate the sum of weights for a list of stations, where weight is inversely proportional to distance
     */
    private fun calculateDistanceWeights(stations: List<StationObservation>): Double {
        return stations.sumOf { station -> 
            // Default to distance 1.0 if distance is null or zero to avoid division by zero
            val distance = station.station.distance ?: 1.0
            1.0 / (if (distance < 1.0) 1.0 else distance)
        }
    }
    
    /**
     * Analyzes station observations to check for freeze conditions
     * @param observations List of station observations
     * @param freezeThresholdF Temperature threshold for freeze warning in °F
     * @return True if stations are reporting freezing conditions, weighted by distance
     */
    fun analyzeForFreeze(
        observations: List<StationObservation>,
        freezeThresholdF: Double
    ): Boolean {
        return analyzeForFreezeWithDetails(observations, freezeThresholdF).first
    }
    
    /**
     * Analyzes station observations to check for freeze conditions,
     * and returns both the result and weighted percentage
     * 
     * @param observations List of station observations
     * @param freezeThresholdF Temperature threshold for freeze warning in °F
     * @return Pair<Boolean, Double> of freeze likelihood result and weighted percentage
     */
    fun analyzeForFreezeWithDetails(
        observations: List<StationObservation>,
        freezeThresholdF: Double
    ): Pair<Boolean, Double> {
        if (observations.isEmpty()) {
            return Pair(false, 0.0)
        }
        
        // Get stations reporting freezing conditions
        val freezingStations = observations.filter { it.isFreezing(freezeThresholdF) }
        
        if (freezingStations.isEmpty()) {
            return Pair(false, 0.0)
        }
        
        // Calculate weighted probability based on distance
        val totalWeight = calculateDistanceWeights(observations)
        val freezingWeight = calculateDistanceWeights(freezingStations)
        
        // Calculate weighted percentage
        val freezingPercentage = (freezingWeight / totalWeight) * 100
        
        Log.d(TAG, "${freezingStations.size}/${observations.size} stations report freezing conditions, weighted percentage: ${freezingPercentage.toInt()}%")
        
        // Log detailed station information for debugging
        observations.forEach { station ->
            val temp = station.temperature?.let { "${String.format("%.1f", it)}°F" } ?: "N/A"
            val isFreezing = if (station.isFreezing(freezeThresholdF)) "FREEZING" else "ABOVE FREEZE"
            val weight = 1.0 / (station.station.distance ?: 1.0)
            Log.d(TAG, "Station: ${station.station.name}, Distance: ${String.format("%.1f", station.station.distance)} km, Temp: $temp, Status: $isFreezing, Weight: ${String.format("%.4f", weight)}")
        }
        
        // Return pair of result and weighted percentage
        return Pair(freezingPercentage >= 50, freezingPercentage)
    }
}