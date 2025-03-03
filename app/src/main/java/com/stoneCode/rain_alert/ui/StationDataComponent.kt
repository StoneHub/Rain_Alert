package com.stoneCode.rain_alert.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.data.StationObservation

// Simple adapter for preview purposes only
object PreviewAdapters {
    fun createPreviewStation(name: String, distance: Double): WeatherStation {
        return WeatherStation(
            id = "PREVIEW_${name.replace(" ", "_")}",
            name = name,
            latitude = 0.0,
            longitude = 0.0,
            distance = distance,
            stationUrl = ""
        )
    }

    fun createPreviewObservation(
        stationName: String,
        distance: Double,
        temperature: Double?,
        windSpeed: Double?,
        windDirection: String?,
        precipitationLastHour: Double?,
        textDescription: String?
    ): StationObservation {
        return StationObservation(
            station = createPreviewStation(stationName, distance),
            temperature = temperature,
            windSpeed = windSpeed,
            windDirection = windDirection,
            precipitationLastHour = precipitationLastHour,
            textDescription = textDescription,
            relativeHumidity = null,
            rawData = null,
            timestamp = null
        )
    }
}

@Composable
fun StationDataComponent(
    stations: List<StationObservation>,
    modifier: Modifier = Modifier,
    onSelectStationsClick: () -> Unit = {},
    onStationLongClick: (String) -> Unit = {}
) {
    if (stations.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
    ) {
        // Header row with title and button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearby Weather Stations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onSelectStationsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Select Stations",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

        // Just show stations in a Column with NO verticalScroll
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            stations.forEachIndexed { index, station ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                StationItem(station = station, onLongClick = onStationLongClick)
            }
        }
    }
}


@SuppressLint("DefaultLocale")
@Composable
fun StationItem(station: StationObservation, onLongClick: (String) -> Unit = {}) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { 
                        // Create a forecast URL from the station's coordinates instead of using the API URL
                        val forecastUrl = "https://forecast.weather.gov/MapClick.php?lat=${station.station.latitude}&lon=${station.station.longitude}"
                        onLongClick(forecastUrl) 
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = station.station.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "(${String.format("%.1f", station.station.distance)} km)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (station.isRaining()) {
                    StatusIndicator(
                        icon = Icons.Default.WaterDrop,
                        contentDescription = "Raining",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (station.isFreezing(35.0)) {
                    StatusIndicator(
                        icon = Icons.Default.Warning,
                        contentDescription = "Freezing",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                station.temperature?.let { tempF ->
                    val tempText = "${tempF.toInt()}°F"
                    Text(
                        text = tempText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (station.temperature != null && station.windSpeed != null) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (station.windSpeed != null && station.windDirection != null) {
                    val windText = "${station.windSpeed.toInt()} mph ${station.windDirection}"
                    Text(
                        text = windText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                station.precipitationLastHour?.let { precip ->
                    if (precip > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.2f", precip)} in",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            station.textDescription?.takeIf { it.isNotEmpty() }?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(
                color = color.copy(alpha = 0.2f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun WeatherDataChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isHighlighted)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isHighlighted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isHighlighted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Previews

@Preview(showBackground = true, widthDp = 360)
@Composable
fun PreviewStationItem() {
    MaterialTheme {
        val dummyObservation = PreviewAdapters.createPreviewObservation(
            stationName = "Test Station",
            distance = 2.5,
            temperature = 42.0,
            windSpeed = 8.0,
            windDirection = "NE",
            precipitationLastHour = 0.1,
            textDescription = "Partly cloudy"
        )
        StationItem(
            station = dummyObservation,
            onLongClick = { url -> Log.d("Preview", "Long click on station with forecast URL: $url") }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 520)
@Composable
fun PreviewStationDataComponent() {
    MaterialTheme {
        val stationObservations = listOf(
            PreviewAdapters.createPreviewObservation(
                stationName = "Station One",
                distance = 1.2,
                temperature = 50.0,
                windSpeed = 10.0,
                windDirection = "N",
                precipitationLastHour = 0.0,
                textDescription = "Sunny"
            ),
            PreviewAdapters.createPreviewObservation(
                stationName = "Station Two",
                distance = 3.4,
                temperature = 32.0,
                windSpeed = 12.0,
                windDirection = "NW",
                precipitationLastHour = 0.5,
                textDescription = "Overcast"
            ),
            PreviewAdapters.createPreviewObservation(
                stationName = "Station Three",
                distance = 5.6,
                temperature = 28.0,
                windSpeed = 7.0,
                windDirection = "E",
                precipitationLastHour = 1.0,
                textDescription = "Raining"
            )
        )
        StationDataComponent(
            stations = stationObservations
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 520)
@Composable
fun PreviewEmptyStationDataComponent() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            StationDataComponent(
                stations = emptyList(),
                onSelectStationsClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStatusIndicator() {
    MaterialTheme {
        StatusIndicator(
            icon = Icons.Default.Warning,
            contentDescription = "Warning",
            color = Color.Red
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWeatherDataChip() {
    MaterialTheme {
        WeatherDataChip(
            label = "Humidity",
            value = "80%",
            isHighlighted = true
        )
    }
}