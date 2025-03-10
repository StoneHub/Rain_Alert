package com.stoneCode.rain_alert.model

/**
 * Model class to hold detailed results of a weather check
 */
data class WeatherCheckResult(
    // Basic result
    val isRaining: Boolean = false,
    val isFreezing: Boolean = false,
    
    // Algorithm metadata
    val usedMultiStationApproach: Boolean = false,
    val thresholdUsed: Double? = null,
    val weightedPercentage: Double? = null,
    
    // Station data
    val stationsUsed: Int? = null,
    val maxDistance: Double? = null,
    val stationDetails: List<StationDetail>? = null,
    
    // Freeze-specific data
    val currentTemperature: Double? = null,
    val forecastTemperatures: List<Double>? = null
)

/**
 * Model class to hold detailed information about a weather station's contribution
 */
data class StationDetail(
    val id: String,
    val name: String,
    val distance: Double,
    val weight: Double,
    val isReportingRain: Boolean = false,
    val isReportingFreeze: Boolean = false,
    val temperature: Double? = null,
    val precipitation: Double? = null,
    val textDescription: String? = null,
    val observationTime: Long? = null
)