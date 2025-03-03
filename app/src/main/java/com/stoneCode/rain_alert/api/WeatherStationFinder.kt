package com.stoneCode.rain_alert.api

import android.util.Log
import com.stoneCode.rain_alert.data.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Data class representing a weather station
 */
data class WeatherStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Double? = null,
    val stationUrl: String
)

/**
 * Class responsible for finding nearby NWS weather stations
 */
class WeatherStationFinder {
    private val TAG = "WeatherStationFinder"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Finds the closest weather stations to the provided location
     * @param latitude Latitude of the user's location
     * @param longitude Longitude of the user's location
     * @param limit Number of closest stations to return (default 3)
     * @return List of closest weather stations
     */
    suspend fun findNearestStations(
        latitude: Double, 
        longitude: Double, 
        limit: Int = 3
    ): Result<List<WeatherStation>> = withContext(Dispatchers.IO) {
        try {
            val stations = fetchStations()
            
            if (stations.isEmpty()) {
                return@withContext Result.failure(Exception("No weather stations found"))
            }
            
            // Calculate distance to each station and sort by distance
            val stationsWithDistance = stations.map { station ->
                val distance = calculateDistance(
                    latitude, longitude,
                    station.latitude, station.longitude
                )
                station.copy(distance = distance)
            }.sortedBy { it.distance }
            
            // Try to include specific important stations like GSP
            val stationsToInclude = mutableListOf<WeatherStation>()
            
            // Look for GSP airport station specifically
            val gspStation = stationsWithDistance.find { 
                it.id == "KGSP" || it.name.contains("Greenville-Spartanburg", ignoreCase = true) 
            }
            
            if (gspStation != null) {
                Log.d(TAG, "Found GSP airport station: ${gspStation.name} (${gspStation.id}), Distance: ${String.format("%.2f", gspStation.distance)} km")
                stationsToInclude.add(gspStation)
            } else {
                Log.d(TAG, "GSP airport station not found in available stations")
            }
            
            // Return the closest stations plus any important stations we want to include
            val closestStations = stationsWithDistance.take(limit).toMutableList()
            
            // Add any specific stations that aren't already in the list
            stationsToInclude.forEach { station ->
                if (!closestStations.any { it.id == station.id }) {
                    closestStations.add(station)
                }
            }
            
            Log.d(TAG, "Found ${closestStations.size} stations for location ($latitude,$longitude)")
            closestStations.forEachIndexed { index, station ->
                Log.d(TAG, "Station ${index + 1}: ${station.name} (${station.id}), Distance: ${String.format("%.2f", station.distance)} km")
            }
            
            Result.success(closestStations)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding nearest stations", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches all available weather stations from the NWS API
     */
    private suspend fun fetchStations(): List<WeatherStation> = withContext(Dispatchers.IO) {
        val stations = mutableListOf<WeatherStation>()
        
        try {
            val stationsUrl = "${AppConfig.WEATHER_API_BASE_URL}stations"
            val request = Request.Builder()
                .url(stationsUrl)
                .header("User-Agent", "(${AppConfig.USER_AGENT}, ${AppConfig.CONTACT_EMAIL})")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch stations: ${response.code}")
                return@withContext emptyList()
            }
            
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val responseJson = JSONObject(responseBody)
            
            // Parse stations from response
            val featuresArray = responseJson.getJSONArray("features")
            
            for (i in 0 until featuresArray.length()) {
                try {
                    val feature = featuresArray.getJSONObject(i)
                    val properties = feature.getJSONObject("properties")
                    val geometry = feature.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    
                    // Get station ID and name
                    val stationId = properties.getString("stationIdentifier")
                    val name = properties.getString("name")
                    
                    // Get coordinates (longitude, latitude order in GeoJSON)
                    val longitude = coordinates.getDouble(0)
                    val latitude = coordinates.getDouble(1)
                    
                    // Get station URL
                    val stationUrl = "${AppConfig.WEATHER_API_BASE_URL}stations/$stationId/observations/latest"
                    
                    stations.add(
                        WeatherStation(
                            id = stationId,
                            name = name,
                            latitude = latitude,
                            longitude = longitude,
                            stationUrl = stationUrl
                        )
                    )
                } catch (e: Exception) {
                    // Skip this station if it has invalid data
                    Log.w(TAG, "Error parsing station data", e)
                    continue
                }
            }
            
            Log.d(TAG, "Fetched ${stations.size} weather stations")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stations", e)
        }
        
        stations
    }

    /**
     * Calculates the distance between two coordinates using the Haversine formula
     * @return Distance in kilometers
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadiusKm * c
    }
}