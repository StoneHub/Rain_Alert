package com.stoneCode.rain_alert.data

import java.util.Date

/**
 * Data class representing a historical alert entry
 */
data class AlertHistoryEntry(
    val id: String, // Unique ID for the alert
    val timestamp: Date, // When the alert was triggered
    val type: AlertType, // Type of alert (RAIN, FREEZE)
    val weatherConditions: String, // Weather conditions that triggered the alert
    val temperature: Double? = null, // Temperature at time of alert (if available)
    val precipitation: Int? = null, // Precipitation chance at time of alert (if available)
    val windSpeed: Double? = null // Wind speed at time of alert (if available)
)

enum class AlertType {
    RAIN,
    FREEZE
}
