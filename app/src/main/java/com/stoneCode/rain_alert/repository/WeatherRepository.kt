package com.stoneCode.rain_alert.repository

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.Geocoder
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.stoneCode.rain_alert.api.MultiStationWeatherService
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.data.StationObservation
import com.stoneCode.rain_alert.data.UserPreferences
import com.stoneCode.rain_alert.service.WeatherApiService
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WeatherRepository(private val context: Context) {
    private val TAG = "WeatherRepository"
    private val weatherApiService = WeatherApiService(context)
    private val multiStationWeatherService = MultiStationWeatherService(context)
    private val userPreferences = UserPreferences(context)
    
    // Store raw API response for debugging
    private var rawApiResponse: String? = null
    private var currentTemperature: Double? = null
    
    private var lastFreezeCheckTime: Long = 0
    private var isFreezing: Boolean = false
    private var precipitationChance: Int? = null
    
    // Store multi-station data
    private var currentStations: List<StationObservation> = emptyList()
    private var stationsLastUpdated: Long = 0
    
    /**
     * Filter stations based on user preferences
     */
    suspend fun filterStationsByPreference(stations: List<StationObservation>): List<StationObservation> {
        val selectedIds = userPreferences.selectedStationIds.first()
        
        return if (selectedIds.isNotEmpty()) {
            // Use only selected stations if available
            stations.filter { selectedIds.contains(it.station.id) }
                .sortedBy { selectedIds.indexOf(it.station.id) }
        } else {
            // Otherwise return all stations
            stations
        }
    }
    
    /**
     * Gets the last known location or uses custom location if enabled
     */
    suspend fun getCurrentLocation(): Location? {
        val useCustomLocation = userPreferences.useCustomLocation.first()
        val customZip = userPreferences.customLocationZip.first()
        
        if (useCustomLocation && !customZip.isNullOrEmpty()) {
            // Use custom location from ZIP code
            try {
                val geocoder = Geocoder(context)
                val addresses = geocoder.getFromLocationName(customZip, 1)
                
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val location = Location("ZipCodeProvider")
                    location.latitude = address.latitude
                    location.longitude = address.longitude
                    Log.d(TAG, "Using custom location from ZIP: $customZip")
                    return location
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location from ZIP code", e)
            }
        }
        
        // Fall back to device location
        return getLastKnownLocation()
    }

    suspend fun checkForRain(): Boolean {
        val lastKnownLocation = getCurrentLocation() ?: return false
        val latitude = lastKnownLocation.latitude
        val longitude = lastKnownLocation.longitude

        // First try multi-station approach
        val stationsResult = getUpdatedStationObservations(latitude, longitude)
        
        if (stationsResult.isSuccess) {
            val stations = stationsResult.getOrNull() ?: emptyList()
            if (stations.isNotEmpty()) {
                // Use threshold from user preferences
                val rainProbabilityThreshold = userPreferences.rainProbabilityThreshold.first()
                val isRaining = multiStationWeatherService.analyzeForRain(stations, rainProbabilityThreshold)
                if (isRaining) {
                    val rainingStations = stations.filter { it.isRaining() }
                    // Get precipitation chance from the closest station that is reporting rain
                    precipitationChance = rainingStations
                        .firstOrNull()
                        ?.precipitationLastHour
                        ?.let { (it * 100).toInt().coerceAtMost(100) }
                    return true
                }
                return false
            }
        }
        
        // Fall back to traditional forecast method if multi-station approach fails
        Log.d(TAG, "Falling back to traditional forecast method for rain check")
        val forecastJson = weatherApiService.getForecast(latitude, longitude) ?: return false
        return parseForecastForRain(forecastJson)
    }

    suspend fun checkForFreezeWarning(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFreezeCheckTime < 4 * 60 * 60 * 1000) {
            return isFreezing // Return cached value if checked within the last 4 hours
        }

        val lastKnownLocation = getCurrentLocation() ?: return false
        val latitude = lastKnownLocation.latitude
        val longitude = lastKnownLocation.longitude
        
        // Try multi-station approach first
        val stationsResult = getUpdatedStationObservations(latitude, longitude)
        
        if (stationsResult.isSuccess) {
            val stations = stationsResult.getOrNull() ?: emptyList()
            if (stations.isNotEmpty()) {
                // Use threshold from user preferences
                val freezeThreshold = userPreferences.freezeThreshold.first()
                val isCurrentlyFreezing = multiStationWeatherService.analyzeForFreeze(stations, freezeThreshold)
                
                if (isCurrentlyFreezing) {
                    // We still need to check the forecast to see if it will be freezing for the required duration
                    val forecastHourlyJson = weatherApiService.getForecastGridData(latitude, longitude) ?: return false
                    isFreezing = parseForecastForFreeze(forecastHourlyJson)
                    lastFreezeCheckTime = currentTime
                    return isFreezing
                }
            }
        }
        
        // Fall back to traditional method
        Log.d(TAG, "Falling back to traditional forecast method for freeze check")
        val forecastHourlyJson = weatherApiService.getForecastGridData(latitude, longitude) ?: return false
        isFreezing = parseForecastForFreeze(forecastHourlyJson)
        lastFreezeCheckTime = currentTime

        return isFreezing
    }

    private fun parseForecastForRain(forecastJson: String): Boolean {
        try {
            // Store raw API response for debugging
            rawApiResponse = forecastJson
            
            val forecast = JSONObject(forecastJson)
            val periods = forecast.getJSONObject("properties").getJSONArray("periods")

            for (i in 0 until periods.length()) {
                val period = periods.getJSONObject(i)
                val shortForecast = period.getString("shortForecast").lowercase(Locale.getDefault())
                val probabilityOfPrecipitation = period.optJSONObject("probabilityOfPrecipitation")?.optInt("value", 0) ?: 0
                val windSpeed = period.optString("windSpeed", "")
                val windInt = windSpeed.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

                Log.d(TAG, "Parsing period: ${period.getString("name")}")
                Log.d(TAG, "Parsing shortForecast: $shortForecast")
                Log.d(TAG, "Probability of Precipitation: $probabilityOfPrecipitation")
                Log.d(TAG, "Wind speed: $windSpeed (parsed as $windInt)")

                precipitationChance = probabilityOfPrecipitation

                // Check for rain or showers with probability over threshold
                if ((shortForecast.contains("rain") || shortForecast.contains("showers")) 
                    && probabilityOfPrecipitation >= AppConfig.RAIN_PROBABILITY_THRESHOLD) {
                    Log.d(TAG, "Rain detected (shortForecast contains 'rain'/'showers' and PoP >= ${AppConfig.RAIN_PROBABILITY_THRESHOLD})")
                    return true
                }
                
                // High wind without rain should not trigger rain alert
                if (windInt > 20 && !shortForecast.contains("rain") && !shortForecast.contains("showers")) {
                    Log.d(TAG, "High wind detected ($windInt) but no rain in forecast, not triggering rain alert")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing forecast for rain", e)
        }
        return false
    }

    private suspend fun parseForecastForFreeze(forecastHourlyJson: String): Boolean {
        try {
            val forecast = JSONObject(forecastHourlyJson)
            val periods = forecast.getJSONObject("properties").getJSONArray("periods")

            var freezeStartTime: Long = 0
            var isCurrentlyFreezing = false
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            
            // Get the user-configured freeze threshold and duration
            val freezeThreshold = userPreferences.freezeThreshold.first()
            val freezeDurationHours = userPreferences.freezeDurationHours.first()
            val freezeDurationMillis = freezeDurationHours * 60 * 60 * 1000L

            Log.d(TAG, "Checking for freeze with threshold: ${freezeThreshold}°F for $freezeDurationHours hours")
            
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
                    if (duration >= freezeDurationMillis) {
                        Log.d(TAG, "Freezing conditions detected for $freezeDurationHours hours or more")
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
        val lastKnownLocation = getCurrentLocation() ?: return "Location unavailable"
        val latitude = lastKnownLocation.latitude
        val longitude = lastKnownLocation.longitude

        // First try to get data from nearby stations
        val stationsResult = getUpdatedStationObservations(latitude, longitude)
        if (stationsResult.isSuccess) {
            val stations = stationsResult.getOrNull() ?: emptyList()
            if (stations.isNotEmpty()) {
                return formatStationWeatherData(stations)
            }
        }
        
        // Fall back to traditional method
        val forecastJson = weatherApiService.getForecast(latitude, longitude)
        if (forecastJson != null) {
            try {
                // Store raw API response for debugging
                rawApiResponse = forecastJson
                
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
                    val windSpeed = currentPeriod.optString("windSpeed", "N/A")
                    
                    // Store current temperature for debugging and display
                    currentTemperature = temperature.toDouble()
                    
                    var weatherInfo = "Now: $shortForecast, $temperature$temperatureUnit"
                    weatherInfo += "\nWind: $windSpeed"

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
    
    fun getRawApiResponse(): String? {
        return rawApiResponse
    }
    
    fun getCurrentTemperature(): Double? {
        return currentTemperature
    }
    
    /**
     * Gets the currently cached station observations
     */
    fun getCurrentStations(): List<StationObservation> {
        return currentStations
    }
    
    /**
     * Gets current station observations, refreshing if necessary
     */
    private suspend fun getUpdatedStationObservations(latitude: Double, longitude: Double): Result<List<StationObservation>> {
        val currentTime = System.currentTimeMillis()
        
        // Return cached data if available and recent
        if (currentStations.isNotEmpty() && 
            currentTime - stationsLastUpdated < 30 * 60 * 1000) { // 30 minutes
            return Result.success(currentStations)
        }
        
        // Otherwise fetch new data
        return try {
            val result = multiStationWeatherService.getNearbyStationsObservations(latitude, longitude)
            
            if (result.isSuccess) {
                val stations = result.getOrNull() ?: emptyList()
                if (stations.isNotEmpty()) {
                    currentStations = stations
                    stationsLastUpdated = currentTime
                    
                    // Update current temperature from the closest station
                    stations.firstOrNull()?.temperature?.let {
                        currentTemperature = it
                    }
                    
                    // Store the raw response for debugging
                    stations.firstOrNull()?.rawData?.let {
                        rawApiResponse = it
                    }
                }
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error getting station observations", e)
            Result.failure(e)
        }
    }
    
    /**
     * Formats weather data from multiple stations into a readable string
     */
    private fun formatStationWeatherData(stations: List<StationObservation>): String {
        if (stations.isEmpty()) return "No station data available"
        
        val closestStation = stations.first()
        val sb = StringBuilder()
        
        // Main current weather from closest station
        closestStation.temperature?.let {
            currentTemperature = it // Store for access by other components
            sb.append("Now: ${it.toInt()}°F")
            closestStation.textDescription?.let { desc ->
                sb.append(", $desc")
            }
            sb.append("\n")
        }
        
        // Wind information
        if (closestStation.windSpeed != null && closestStation.windDirection != null) {
            sb.append("Wind: ${closestStation.windSpeed.toInt()}mph ${closestStation.windDirection}\n")
        }
        
        sb.append("\nStation data from ${stations.size} nearby stations:\n")
        
        // Add data from each station
        stations.forEachIndexed { index, station ->
            sb.append("${index + 1}. ${station.station.name} (${String.format("%.1f", station.station.distance)} km)")
            station.temperature?.let { sb.append(", ${it.toInt()}°F") }
            
            if (station.isRaining()) {
                sb.append(" - RAINING")
            }
            
            if (station.precipitationLastHour != null && station.precipitationLastHour > 0) {
                sb.append(" - ${String.format("%.2f", station.precipitationLastHour)}in precip")
            }
            
            sb.append("\n")
        }
        
        return sb.toString()
    }
}