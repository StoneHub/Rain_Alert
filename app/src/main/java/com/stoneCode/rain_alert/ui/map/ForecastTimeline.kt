package com.stoneCode.rain_alert.ui.map

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Represents a forecast time slot with weather information
 */
data class ForecastTimeSlot(
    val dateTime: LocalDateTime,
    val weatherType: WeatherType,
    val temperature: Float,
    val precipitation: Float,
    val windSpeed: Float
)

/**
 * Enum representing different weather types
 */
enum class WeatherType {
    SUNNY, CLOUDY, RAINY, PARTLY_CLOUDY
}

/**
 * Companion object to provide utility methods for weather type icons
 */
object WeatherTypeIcons {
    fun getIcon(weatherType: WeatherType): ImageVector = when (weatherType) {
        WeatherType.SUNNY -> Icons.Default.WbSunny
        WeatherType.CLOUDY -> Icons.Default.Cloud
        WeatherType.RAINY -> Icons.Default.WaterDrop
        WeatherType.PARTLY_CLOUDY -> Icons.Default.Cloud // TODO: Consider a more specific icon
    }

    fun getColor(weatherType: WeatherType): Color = when (weatherType) {
        WeatherType.SUNNY -> Color(0xFFFFD700) // Gold
        WeatherType.CLOUDY -> Color.Gray
        WeatherType.RAINY -> Color.Blue
        WeatherType.PARTLY_CLOUDY -> Color(0xFFA9A9A9) // Dark Gray
    }
}

/**
 * Composable for the forecast timeline
 *
 * @param forecastSlots List of forecast time slots to display
 * @param selectedSlot Currently selected forecast slot
 * @param onSlotSelected Callback when a new slot is selected
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ForecastTimeline(
    modifier: Modifier = Modifier,
    forecastSlots: List<ForecastTimeSlot> = generateMockForecastData(),
    selectedSlot: ForecastTimeSlot? = null,
    onSlotSelected: (ForecastTimeSlot) -> Unit = {}
) {
    Box(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(forecastSlots) { slot ->
                ForecastTimelineItem(
                    slot = slot,
                    isSelected = slot == selectedSlot,
                    onClick = { onSlotSelected(slot) }
                )
            }
        }
    }
}

/**
 * Individual item in the forecast timeline
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ForecastTimelineItem(
    slot: ForecastTimeSlot,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Date
            Text(
                text = slot.dateTime.format(dateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            // Time
            Text(
                text = slot.dateTime.format(timeFormatter),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            // Weather Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(WeatherTypeIcons.getColor(slot.weatherType).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = WeatherTypeIcons.getIcon(slot.weatherType),
                    contentDescription = slot.weatherType.name,
                    tint = WeatherTypeIcons.getColor(slot.weatherType),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Temperature
            Text(
                text = "${slot.temperature.toInt()}°",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Precipitation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Precipitation",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Blue.copy(alpha = 0.7f)
                )
                Text(
                    text = "${(slot.precipitation * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Generate mock forecast data for preview and testing
 */
@RequiresApi(Build.VERSION_CODES.O)
fun generateMockForecastData(): List<ForecastTimeSlot> {
    val now = LocalDateTime.now()
    return listOf(
        ForecastTimeSlot(
            dateTime = now,
            weatherType = WeatherType.SUNNY,
            temperature = 25f,
            precipitation = 0.1f,
            windSpeed = 5f
        ),
        ForecastTimeSlot(
            dateTime = now.plusHours(3),
            weatherType = WeatherType.CLOUDY,
            temperature = 22f,
            precipitation = 0.3f,
            windSpeed = 10f
        ),
        ForecastTimeSlot(
            dateTime = now.plusHours(6),
            weatherType = WeatherType.RAINY,
            temperature = 18f,
            precipitation = 0.7f,
            windSpeed = 15f
        ),
        ForecastTimeSlot(
            dateTime = now.plusHours(9),
            weatherType = WeatherType.PARTLY_CLOUDY,
            temperature = 20f,
            precipitation = 0.2f,
            windSpeed = 8f
        ),
        ForecastTimeSlot(
            dateTime = now.plusHours(12),
            weatherType = WeatherType.SUNNY,
            temperature = 26f,
            precipitation = 0.05f,
            windSpeed = 6f
        )
    )
}

// Preview function for the ForecastTimeline
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun PreviewForecastTimeline() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ForecastTimeline(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
        }
    }
}