package com.stoneCode.rain_alert.model

/**
 * Model class to hold detailed information about a station's contribution to weather decision
 */
data class StationContribution(
    val stationId: String,
    val stationName: String,
    val distance: Double, // in km
    val weight: Double, // normalized weight (0-1)
    val temperature: Double?, // in Fahrenheit
    val precipitation: Double?, // in inches
    val isPositive: Boolean, // true if contributing to a freeze/rain prediction
    val textDescription: String? = null
)

/**
 * Model class to hold detailed information about freeze detection algorithm decision
 */
data class FreezingDecisionData(
    val stationContributions: List<StationContribution>,
    val weightedPercentage: Double,
    val thresholdUsed: Double,
    val currentTemperature: Double?,
    val wasUsingMultiStationApproach: Boolean,
    val confidence: AlertConfidence? = null,
    val decisionTimestamp: Long = System.currentTimeMillis()
)

/**
 * Model class to hold detailed information about rain detection algorithm decision
 */
data class RainDecisionData(
    val stationContributions: List<StationContribution>,
    val weightedPercentage: Double,
    val precipitationThreshold: Double,
    val currentPrecipitation: Double?,
    val wasUsingMultiStationApproach: Boolean,
    val textBasedDetection: Boolean,
    val confidence: AlertConfidence? = null,
    val decisionTimestamp: Long = System.currentTimeMillis()
)
