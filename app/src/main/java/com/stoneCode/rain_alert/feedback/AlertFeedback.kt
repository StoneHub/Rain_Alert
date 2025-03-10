package com.stoneCode.rain_alert.feedback

import java.util.UUID

/**
 * Model class for user feedback on alert accuracy
 */
data class AlertFeedback(
    val alertId: String = UUID.randomUUID().toString(),
    val alertType: String,  // "rain" or "freeze"
    val wasAccurate: Boolean,
    val userComments: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Algorithm details for analysis
    val stationCount: Int? = null,
    val weightedPercentage: Double? = null,
    val maxDistance: Double? = null,
    val usedMultiStationApproach: Boolean = false,
    val thresholdUsed: Double? = null,
    val confidenceScore: Float? = null
)
