package com.stoneCode.rain_alert.repository

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.Geocoder
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.StoneCode.rain_alert.R
import com.stoneCode.rain_alert.api.MultiStationWeatherService
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.data.StationObservation
import com.stoneCode.rain_alert.data.UserPreferences
import com.stoneCode.rain_alert.model.StationDetail
import com.stoneCode.rain_alert.model.WeatherCheckResult
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
    
    // Store additional metrics
    private var lastStationCount: Int = 0
    private var lastWeightedPercentage: Double = 0.0
    private var lastMultiStationApproach: Boolean = false

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

    /**
     * Enhanced version of checkForRain that returns detailed results for analytics
     */
    suspend fun checkForRainWithDetails(): WeatherCheckResult {
        val lastKnownLocation = getCurrentLocation() ?: return WeatherCheckResult(isRaining = false)
        val latitude = lastKnownLocation.latitude
        val longitude = lastKnownLocation.longitude
        var usedMultiStationApproach = false
        var weightedPercentage: Double? = null
        var thresholdUsed: Double? = null
        var stationCount: Int? = null
        var maxDistance: Double? = null
        var stationDetails: List<StationDetail>? = null

        // First try multi-station approach
        val stationsResult = getUpdatedStationObservations(latitude, longitude)
        
        if (stationsResult.isSuccess) {
            val stations = stationsResult.getOrNull() ?: emptyList()
            if (stations.isNotEmpty()) {
                usedMultiStationApproach = true
                stationCount = stations.size
                maxDistance = stations.map { it.station.distance ?: 0.0 }.maxOrNull() ?: 0.0

                // Get threshold from user preferences
                val rainProbabilityThreshold = userPreferences.rainProbabilityThreshold.first()
                thresholdUsed = rainProbabilityThreshold.toDouble()

                // Get detailed analysis
                val analysisResult = multiStationWeatherService.analyzeForRainWithDetails(stations, rainProbabilityThreshold)
                val isRaining = analysisResult.first
                weightedPercentage = analysisResult.second

                // Store metrics for later queries
                lastStationCount = stationCount
                lastWeightedPercentage = weightedPercentage
                lastMultiStationApproach = true

                // Create station details
                stationDetails = stations.map { station ->
                    StationDetail(
                        id = station.station.id,
                        name = station.station.name,
                        distance = station.station.distance ?: 0.0,
                        weight = 1.0 / Math.max(station.station.distance ?: 1.0, 1.0),
                        isReportingRain = station.isRaining(),
                        temperature = station.temperature,
                        precipitation = station.precipitationLastHour,
                        textDescription = station.textDescription,
                        observationTime = station.timestamp?.let { 
                            SimpleDateFormat(context.getString(R.string.yyyy_mm_dd_t_hh_mm_ssxxx), Locale.getDefault())
                                .parse(it)?.time 
                        }
                    )
                }

                if (isRaining) {
                    val rainingStations = stations.filter { it.isRaining() }
                    // Get precipitation chance from the closest station that is reporting rain
                    precipitationChance = rainingStations
                        .firstOrNull()
                        ?.precipitationLastHour
                        ?.let { (it * 100).toInt().coerceAtMost(100) }

                    return WeatherCheckResult(
                        isRaining = true,
                        usedMultiStationApproach = true,
                        thresholdUsed = thresholdUsed,
                        weightedPercentage = weightedPercentage,
                        stationsUsed = stationCount,
                        maxDistance = maxDistance,
                        stationDetails = stationDetails
                    )
                }

                return WeatherCheckResult(
                    isRaining = false,
                    usedMultiStationApproach = true,
                    thresholdUsed = thresholdUsed,
                    weightedPercentage = weightedPercentage,
                    stationsUsed = stationCount,
                    maxDistance = maxDistance,
                    stationDetails = stationDetails
                )
            }
        }
        
        // Fall back to traditional forecast method if multi-station approach fails
        Log.d(TAG, "Falling back to traditional forecast method for rain check")
        val forecastJson = weatherApiService.getForecast(latitude, longitude) ?: 
            return WeatherCheckResult(isRaining = false)
        
        // Store metrics for later queries
        lastStationCount = 0
        lastWeightedPercentage = 0.0
        lastMultiStationApproach = false
        
        val isRaining = parseForecastForRain(forecastJson)
        thresholdUsed = AppConfig.RAIN_PROBABILITY_THRESHOLD.toDouble()
        
        return WeatherCheckResult(
            isRaining = isRaining,
            usedMultiStationApproach = false,
            thresholdUsed = thresholdUsed,
            weightedPercentage = precipitationChance?.toDouble(),
            stationsUsed = 0,
            maxDistance = null,
            stationDetails = null
        )
    }

    /**
     * Original checkForRain method (calls enhanced version)
     */
    suspend fun checkForRain(): Boolean {
        return checkForRainWithDetails().isRaining
    }

    /**
     * Enhanced version of checkForFreezeWarning that returns detailed results for analytics
     */
    suspend fun checkForFreezeWarningWithDetails(): WeatherCheckResult {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFreezeCheckTime < 4 * 60 * 60 * 1000) {
            // Return cached result with limited details if checked recently
            return WeatherCheckResult(
                isFreezing = isFreezing,
                usedMultiStationApproach = lastMultiStationApproach,
                stationsUsed = lastStationCount,
                maxDistance = null,
                weightedPercentage = null,
                thresholdUsed = userPreferences.freezeThreshold.first()
            )
        }

        val lastKnownLocation = getCurrentLocation() ?: 
            return WeatherCheckResult(isFreezing = false)
            
        val latitude = lastKnownLocation.latitude
        val longitude = lastKnownLocation.longitude
        var usedMultiStationApproach = false
        var weightedPercentage: Double? = null
        var stationCount: Int? = null
        var maxDistance: Double? = null
        var stationDetails: List<StationDetail>? = null
        var forecastTemperatures: List<Double>? = null
        
        // Get threshold from user preferences
        val freezeThreshold = userPreferences.freezeThreshold.first()
        
        // Try multi-station approach first
        val stationsResult = getUpdatedStationObservations(latitude, longitude)
        
        if (stationsResult.isSuccess) {
            val stations = stationsResult.getOrNull() ?: emptyList()
            if (stations.isNotEmpty()) {
                usedMultiStationApproach = true
                stationCount = stations.size
                maxDistance = stations.map { it.station.distance ?: 0.0 }.maxOrNull() ?: 0.0
                
                // Get detailed analysis
                val analysisResult = multiStationWeatherService.analyzeForFreezeWithDetails(stations, freezeThreshold)
                val isCurrentlyFreezing = analysisResult.first
                weightedPercentage = analysisResult.second
                
                // Store metrics for later queries
                lastStationCount = stationCount
                lastWeightedPercentage = weightedPercentage
                lastMultiStationApproach = true
                
                // Create station details
                stationDetails = stations.map { station ->
                    StationDetail(
                        id = station.station.id,
                        name = station.station.name,
                        distance = station.station.distance ?: 0.0,
                        weight = 1.0 / Math.max(station.station.distance ?: 1.0, 1.0),
                        isReportingFreeze = station.temperature != null && station.temperature <= freezeThreshold,
                        temperature = station.temperature,
                        precipitation = station.precipitationLastHour,
                        textDescription = station.textDescription,
                        observationTime = station.timestamp?.let { 
                            SimpleDateFormat(context.getString(R.string.yyyy_mm_dd_t_hh_mm_ssxxx), Locale.getDefault())
                                .parse(it)?.time 
                        }
                    )
                }
                
                if (isCurrentlyFreezing) {
                    // We still need to check the forecast to see if it will be freezing for the required duration
                    val forecastHourlyJson = weatherApiService.getForecastGridData(latitude, longitude) 
                    
                    if (forecastHourlyJson != null) {
                        // Parse for freeze durations
                        val freezeCheckResult = parseForecastForFreezeWithDetails(forecastHourlyJson)
                        
                        isFreezing = freezeCheckResult.isFreezing
                        forecastTemperatures = freezeCheckResult.forecastTemperatures
                        lastFreezeCheckTime = currentTime
                        
                        return WeatherCheckResult(
                            isFreezing = isFreezing,
                            usedMultiStationApproach = true,
                            thresholdUsed = freezeThreshold,
                            weightedPercentage = weightedPercentage,
                            stationsUsed = stationCount,
                            maxDistance = maxDistance,
                            stationDetails = stationDetails,
                            currentTemperature = stations.firstOrNull()?.temperature,
                            forecastTemperatures = forecastTemperatures
                        )
                    }
                }
                
                return WeatherCheckResult(
                    isFreezing = false,
                    usedMultiStationApproach = true,
                    thresholdUsed = freezeThreshold,
                    weightedPercentage = weightedPercentage,
                    stationsUsed = stationCount,
                    maxDistance = maxDistance,
                    stationDetails = stationDetails,
                    currentTemperature = stations.firstOrNull()?.temperature
                )
            }
        }
        
        // Fall back to traditional method
        Log.d(TAG, "Falling back to traditional forecast method for freeze check")
        val forecastHourlyJson = weatherApiService.getForecastGridData(latitude, longitude) ?: 
            return WeatherCheckResult(isFreezing = false)
        
        // Store metrics for later queries
        lastStationCount = 0
        lastWeightedPercentage = 0.0
        lastMultiStationApproach = false
        
        // Parse detailed freeze forecast
        val freezeCheckResult = parseForecastForFreezeWithDetails(forecastHourlyJson)
        
        isFreezing = freezeCheckResult.isFreezing
        forecastTemperatures = freezeCheckResult.forecastTemperatures
        lastFreezeCheckTime = currentTime
        
        return WeatherCheckResult(
            isFreezing = isFreezing,
            usedMultiStationApproach = false,
            thresholdUsed = freezeThreshold,
            stationsUsed = 0,
            maxDistance = null,
            currentTemperature = freezeCheckResult.currentTemperature,
            forecastTemperatures = forecastTemperatures
        )
    }

    /**
     * Original checkForFreezeWarning method (calls enhanced version)
     */
    suspend fun checkForFreezeWarning(): Boolean {
        return checkForFreezeWarningWithDetails().isFreezing
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

    /**
     * Enhanced freeze forecast parser that returns detailed information
     */
    private suspend fun parseForecastForFreezeWithDetails(forecastHourlyJson: String): WeatherCheckResult {
        try {
            val forecast = JSONObject(forecastHourlyJson)
            val periods = forecast.getJSONObject("properties").getJSONArray("periods")

            var freezeStartTime: Long = 0
            var isCurrentlyFreezing = false
            val dateFormat = SimpleDateFormat(context.getString(R.string.yyyy_mm_dd_t_hh_mm_ssxxx), Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            
            // Get the user-configured freeze threshold and duration
            val freezeThreshold = userPreferences.freezeThreshold.first()
            val freezeDurationHours = userPreferences.freezeDurationHours.first()
            val freezeDurationMillis = freezeDurationHours * 60 * 60 * 1000L

            Log.d(TAG, "Checking for freeze with threshold: ${freezeThreshold}°F for $freezeDurationHours hours")
            
            // Track temperatures for reporting
            val temperatures = mutableListOf<Double>()
            var currentTemperature: Double? = null
            
            for (i in 0 until periods.length()) {
                val period = periods.getJSONObject(i)
                val temperature = period.getDouble("temperature")
                val startTimeString = period.getString("startTime")
                val startTime = dateFormat.parse(startTimeString)?.time ?: continue
                
                if (i == 0) {
                    currentTemperature = temperature
                }
                
                temperatures.add(temperature)
                
                if (temperature <= freezeThreshold) {
                    if (!isCurrentlyFreezing) {
                        freezeStartTime = startTime
                        isCurrentlyFreezing = true
                    }
                    val duration = startTime - freezeStartTime
                    if (duration >= freezeDurationMillis) {
                        Log.d(TAG, "Freezing conditions detected for $freezeDurationHours hours or more")
                        return WeatherCheckResult(
                            isFreezing = true,
                            currentTemperature = currentTemperature,
                            forecastTemperatures = temperatures.take(freezeDurationHours + 1)
                        )
                    }
                } else {
                    isCurrentlyFreezing = false
                }
            }
            
            return WeatherCheckResult(
                isFreezing = false,
                currentTemperature = currentTemperature,
                forecastTemperatures = temperatures.take(freezeDurationHours + 1)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing forecast for freeze", e)
            return WeatherCheckResult(isFreezing = false)
        }
    }

    private suspend fun parseForecastForFreeze(forecastHourlyJson: String): Boolean {
        return parseForecastForFreezeWithDetails(forecastHourlyJson).isFreezing
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
                val dateFormatter = SimpleDateFormat(context.getString(R.string.yyyy_mm_dd_t_hh_mm_ssxxx), Locale.US)
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
     * Returns the number of stations used in the last weather check
     */
    fun getLastStationCount(): Int? {
        return lastStationCount
    }
    
    /**
     * Returns the weighted percentage from the last weather check
     */
    fun getLastWeightedPercentage(): Double? {
        return lastWeightedPercentage
    }
    
    /**
     * Returns whether the last weather check used multi-station approach
     */
    fun lastUsedMultiStationApproach(): Boolean? {
        return lastMultiStationApproach
    }
    
    /**
     * Refreshes station data specifically for the given station IDs
     */
    suspend fun refreshStationData(stationIds: List<String>): List<StationObservation> {
        val location = getCurrentLocation() ?: return emptyList()
        
        try {
            Log.d(TAG, "Refreshing station data for IDs: $stationIds")
            
            // Always get fresh station data regardless of cache
            val result = multiStationWeatherService.getNearbyStationsObservations(
                latitude = location.latitude,
                longitude = location.longitude,
                forceRefresh = true // Force fresh data
            )
            
            if (result.isSuccess) {
                val allStations = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Retrieved ${allStations.size} stations from API")
                
                // Filter to only include selected stations
                val filteredStations = if (stationIds.isNotEmpty()) {
                    // Check if all requested stations were found
                    val foundIds = allStations.map { it.station.id }
                    val missingIds = stationIds.filter { !foundIds.contains(it) }
                    
                    if (missingIds.isNotEmpty()) {
                        Log.w(TAG, "Some selected stations were not found: $missingIds")
                    }
                    
                    // Filter and sort by the order in stationIds
                    allStations.filter { stationIds.contains(it.station.id) }
                        .sortedBy { stationIds.indexOf(it.station.id) }
                } else {
                    // If no specific stations requested, use all stations
                    allStations
                }
                
                Log.d(TAG, "Filtered to ${filteredStations.size} selected stations")
                
                // Update the cached stations
                currentStations = filteredStations
                stationsLastUpdated = System.currentTimeMillis()
                
                return filteredStations
            } else {
                Log.e(TAG, "Failed to get stations: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing station data", e)
        }
        
        return emptyList()
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
            sb.append("${index + 1}. ${station.station.name} (${String.format(Locale.US, "%.1f", station.station.distance)} km)")
            station.temperature?.let { sb.append(", ${it.toInt()}°F") }
            
            if (station.isRaining()) {
                sb.append(" - RAINING")
            }
            
            if (station.precipitationLastHour != null && station.precipitationLastHour > 0) {
                sb.append(" - ${String.format(Locale.US, "%.2f", station.precipitationLastHour)}in precip")
            }
            
            sb.append("\n")
        }
        
        return sb.toString()
    }
}