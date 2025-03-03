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
     * Finds the closest weather stations to the provided location.
     *
     * By default, we:
     *   1) Use NWS's recommended "/points/lat,lon" endpoint to discover
     *      the official list of nearby stations.
     *   2) Calculate distances ourselves (optional).
     *
     * @param latitude  User’s latitude
     * @param longitude User’s longitude
     * @param limit     Number of closest stations to return (default 3)
     * @return List of closest weather stations
     */
    suspend fun findNearestStations(
        latitude: Double,
        longitude: Double,
        limit: Int = 3
    ): Result<List<WeatherStation>> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Fetch stations via the official "points" approach
            val stations = fetchStationsNearLocation(latitude, longitude)
            if (stations.isEmpty()) {
                return@withContext Result.failure(Exception("No weather stations found from NWS for ($latitude,$longitude)"))
            }

            // Step 2 (optional): Compute distance to each station in case you want
            // to confirm the ordering or do custom filtering.
            val stationsWithDistance = stations.map { station ->
                val dist = calculateDistance(latitude, longitude, station.latitude, station.longitude)
                station.copy(distance = dist)
            }.sortedBy { it.distance }

            // Step 3 (optional): If you have certain stations you want to
            // force-include (e.g., KGSP), you can do so:
            val stationsToInclude = mutableListOf<WeatherStation>()

            // Look for GSP or any other must-include station in the downloaded list
            val gspStation = stationsWithDistance.find {
                it.id.equals("KGSP", ignoreCase = true) ||
                        it.name.contains("Greenville-Spartanburg", ignoreCase = true)
            }
            if (gspStation != null) {
                Log.d(TAG, "Found GSP station: ${gspStation.name} (${gspStation.id}), distance=" +
                        "${String.format("%.2f", gspStation.distance)} km")
                stationsToInclude.add(gspStation)
            } else {
                Log.d(TAG, "GSP station not in NWS results; it may not be reporting or is out of range.")
            }

            // Step 4: Build final list
            // - Take your top [limit] from the sorted list
            // - Add any must-include station that was missing
            val closestStations = stationsWithDistance.take(limit).toMutableList()
            stationsToInclude.forEach { forcedStation ->
                if (!closestStations.any { it.id == forcedStation.id }) {
                    closestStations.add(forcedStation)
                }
            }

            Log.d(TAG, "Found ${closestStations.size} stations near ($latitude,$longitude)")
            closestStations.forEachIndexed { i, s ->
                Log.d(TAG, "Station ${i + 1}: ${s.name} (${s.id}), dist=${String.format("%.2f", s.distance)} km")
            }

            Result.success(closestStations)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding nearest stations", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches the list of nearby stations using the NWS-recommended
     * "/points/{lat},{lon}" approach, then reading "observationStations".
     */
    private suspend fun fetchStationsNearLocation(
        latitude: Double,
        longitude: Double
    ): List<WeatherStation> = withContext(Dispatchers.IO) {
        val stations = mutableListOf<WeatherStation>()
        try {
            // 1) Query /points/lat,lon to retrieve the stations URL
            val pointsUrl = "${AppConfig.WEATHER_API_BASE_URL}points/$latitude,$longitude"
            val requestPoints = Request.Builder()
                .url(pointsUrl)
                .header("User-Agent", "(${AppConfig.USER_AGENT}, ${AppConfig.CONTACT_EMAIL})")
                .build()

            val responsePoints = client.newCall(requestPoints).execute()
            if (!responsePoints.isSuccessful) {
                Log.e(TAG, "NWS /points request failed: ${responsePoints.code}")
                return@withContext emptyList<WeatherStation>()
            }

            val pointsBody = responsePoints.body?.string().orEmpty()
            val pointsJson = JSONObject(pointsBody)
            val properties = pointsJson.optJSONObject("properties")
            if (properties == null) {
                Log.e(TAG, "NWS /points response missing properties object")
                return@withContext emptyList<WeatherStation>()
            }

            val stationsUrl = properties.optString("observationStations")
            if (stationsUrl.isNullOrEmpty()) {
                Log.e(TAG, "NWS /points response has no observationStations link")
                return@withContext emptyList<WeatherStation>()
            }

            // 2) Query the "observationStations" URL to get the actual station list
            val requestStations = Request.Builder()
                .url(stationsUrl)
                .header("User-Agent", "(${AppConfig.USER_AGENT}, ${AppConfig.CONTACT_EMAIL})")
                .build()

            val responseStations = client.newCall(requestStations).execute()
            if (!responseStations.isSuccessful) {
                Log.e(TAG, "NWS stations request failed: ${responseStations.code}")
                return@withContext emptyList<WeatherStation>()
            }

            val stationsBody = responseStations.body?.string().orEmpty()
            val stationsJson = JSONObject(stationsBody)
            val featuresArray = stationsJson.optJSONArray("features")
            if (featuresArray == null) {
                Log.e(TAG, "NWS stations response missing features array")
                return@withContext emptyList<WeatherStation>()
            }

            // 3) Parse each station feature
            for (i in 0 until featuresArray.length()) {
                try {
                    val feature = featuresArray.getJSONObject(i)
                    val props = feature.getJSONObject("properties")
                    val geometry = feature.getJSONObject("geometry")
                    val coords = geometry.getJSONArray("coordinates")

                    // Station ID, name
                    val stationId = props.getString("stationIdentifier")
                    val name = props.getString("name")

                    // Coordinates come in [longitude, latitude]
                    val stationLon = coords.getDouble(0)
                    val stationLat = coords.getDouble(1)

                    // Build station URL
                    val stationUrl = "${AppConfig.WEATHER_API_BASE_URL}stations/$stationId/observations/latest"

                    stations.add(
                        WeatherStation(
                            id = stationId,
                            name = name,
                            latitude = stationLat,
                            longitude = stationLon,
                            stationUrl = stationUrl
                        )
                    )
                } catch (parseErr: Exception) {
                    Log.w(TAG, "Skipping station entry due to parse error", parseErr)
                }
            }

            Log.d(TAG, "Fetched ${stations.size} nearby stations from NWS for ($latitude,$longitude)")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching stations near location", e)
        }
        stations
    }

    /**
     * (Optional) Calculate the distance between two coordinates (Haversine formula)
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
