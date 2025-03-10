package com.stoneCode.rain_alert.model

/**
 * Model class to represent the confidence level of an alert
 */
data class AlertConfidence(
    val level: ConfidenceLevel,
    val score: Float,
    val factors: List<String>
)

/**
 * Enumeration of confidence levels
 */
enum class ConfidenceLevel { LOW, MEDIUM, HIGH }

/**
 * Function to calculate confidence level for freezing conditions
 */
fun calculateFreezingAlertConfidence(
    stationCount: Int,
    weightedPercentage: Double,
    maxDistance: Double?,
    thresholdDifference: Double
): AlertConfidence {
    val factors = mutableListOf<String>()
    var score = 0f
    
    // Factor 1: Number of stations
    when {
        stationCount >= 5 -> {
            score += 0.4f
            factors.add("Multiple stations reporting (${stationCount})")
        }
        stationCount >= 3 -> {
            score += 0.2f
            factors.add("Several stations reporting (${stationCount})")
        }
        else -> {
            factors.add("Limited station data (${stationCount})")
        }
    }
    
    // Factor 2: Agreement percentage
    when {
        weightedPercentage >= 80 -> {
            score += 0.3f
            factors.add("Strong agreement between stations")
        }
        weightedPercentage >= 60 -> {
            score += 0.2f
            factors.add("Moderate agreement between stations")
        }
        else -> {
            factors.add("Mixed signals from stations")
        }
    }
    
    // Factor 3: Station distance
    maxDistance?.let {
        when {
            it < 10 -> {
                score += 0.2f
                factors.add("Stations within 10km")
            }
            it < 30 -> {
                score += 0.1f
                factors.add("Stations within 30km")
            }
            else -> {
                factors.add("Some distant stations used")
            }
        }
    }
    
    // Factor 4: Threshold difference
    when {
        thresholdDifference > 5 -> {
            score += 0.2f
            factors.add("Temperature well below freezing")
        }
        thresholdDifference > 2 -> {
            score += 0.1f
            factors.add("Temperature below freezing")
        }
    }
    
    val level = when {
        score >= 0.7f -> ConfidenceLevel.HIGH
        score >= 0.4f -> ConfidenceLevel.MEDIUM
        else -> ConfidenceLevel.LOW
    }
    
    return AlertConfidence(level, score, factors)
}

/**
 * Function to calculate confidence level for rain conditions
 */
fun calculateRainAlertConfidence(
    stationCount: Int,
    weightedPercentage: Double,
    maxDistance: Double?,
    precipitation: Double?,
    textBasedDetection: Boolean
): AlertConfidence {
    val factors = mutableListOf<String>()
    var score = 0f
    
    // Factor 1: Number of stations
    when {
        stationCount >= 5 -> {
            score += 0.3f
            factors.add("Multiple stations reporting (${stationCount})")
        }
        stationCount >= 3 -> {
            score += 0.2f
            factors.add("Several stations reporting (${stationCount})")
        }
        else -> {
            factors.add("Limited station data (${stationCount})")
        }
    }
    
    // Factor 2: Agreement percentage
    when {
        weightedPercentage >= 80 -> {
            score += 0.3f
            factors.add("Strong agreement between stations")
        }
        weightedPercentage >= 60 -> {
            score += 0.2f
            factors.add("Moderate agreement between stations")
        }
        else -> {
            factors.add("Mixed signals from stations")
        }
    }
    
    // Factor 3: Station distance
    maxDistance?.let {
        when {
            it < 10 -> {
                score += 0.2f
                factors.add("Stations within 10km")
            }
            it < 30 -> {
                score += 0.1f
                factors.add("Stations within 30km")
            }
            else -> {
                factors.add("Some distant stations used")
            }
        }
    }
    
    // Factor 4: Precipitation amount
    precipitation?.let {
        when {
            it > 0.1 -> {
                score += 0.2f
                factors.add("Significant precipitation detected")
            }
            it > 0.01 -> {
                score += 0.1f
                factors.add("Light precipitation detected")
            }
        }
    }
    
    // Factor 5: Text-based detection
    if (textBasedDetection) {
        score += 0.2f
        factors.add("Weather description indicates rain")
    }
    
    val level = when {
        score >= 0.7f -> ConfidenceLevel.HIGH
        score >= 0.4f -> ConfidenceLevel.MEDIUM
        else -> ConfidenceLevel.LOW
    }
    
    return AlertConfidence(level, score, factors)
}
