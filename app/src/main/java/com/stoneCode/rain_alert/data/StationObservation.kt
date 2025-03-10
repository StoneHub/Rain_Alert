package com.stoneCode.rain_alert.data

import com.stoneCode.rain_alert.api.WeatherStation

/**
 * Data class representing a weather observation from a station
 */
data class StationObservation(
    val station: WeatherStation,
    val temperature: Double?, // Temperature in Fahrenheit
    val precipitationLastHour: Double?, // Precipitation in inches for the last hour
    val relativeHumidity: Double?, // Relative humidity percentage
    val windSpeed: Double?, // Wind speed in mph
    val windDirection: String?, // Wind direction as string (e.g. "NE", "SW")
    val textDescription: String?, // Text description of weather
    val rawData: String?, // Raw JSON response for debugging
    val timestamp: String? // Timestamp of the observation
) {
    /**
     * Determine if this observation indicates rain
     * Enhanced version with improved detection capabilities
     */
    fun isRaining(): Boolean {
        // Rain is indicated by precipitation or by textual description
        if (precipitationLastHour != null && precipitationLastHour > 0.01) {
            return true
        }
        
        // Enhanced detection using relative humidity and text
        if (relativeHumidity != null && relativeHumidity > 95 && 
            (textDescription?.lowercase()?.contains("overcast") == true || 
             textDescription?.lowercase()?.contains("fog") == true)) {
            return true
        }
        
        // Check text description for rain indicators
        val rainIndicators = listOf(
            "rain", "shower", "drizzle", "thunderstorm", 
            "precipitation", "precip", "wet", "mist"
        )
        
        textDescription?.lowercase()?.let { desc ->
            for (indicator in rainIndicators) {
                if (desc.contains(indicator)) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Check if the observation indicates freezing conditions
     */
    fun isFreezing(thresholdF: Double): Boolean {
        return temperature != null && temperature <= thresholdF
    }
}