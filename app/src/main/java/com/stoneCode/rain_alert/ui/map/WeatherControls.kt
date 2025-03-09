package com.stoneCode.rain_alert.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Weather layer control chips for toggling different weather data layers.
 */
@Composable
fun WeatherControls(
    onPrecipitationToggle: () -> Unit,
    onWindLayerToggle: () -> Unit,
    onTemperatureToggle: () -> Unit,
    showPrecipitationLayer: Boolean,
    showWindLayer: Boolean,
    showTemperatureLayer: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Precipitation layer toggle
        FilterChip(
            selected = showPrecipitationLayer,
            onClick = onPrecipitationToggle,
            label = { Text("Precipitation") },
            leadingIcon = {
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = if (showPrecipitationLayer) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        )
        
        // Wind layer toggle
        FilterChip(
            selected = showWindLayer,
            onClick = onWindLayerToggle,
            label = { Text("Wind") },
            leadingIcon = {
                Icon(
                    Icons.Default.AirplanemodeActive,
                    contentDescription = null,
                    tint = if (showWindLayer) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        )
        
        // Temperature layer toggle
        FilterChip(
            selected = showTemperatureLayer,
            onClick = onTemperatureToggle,
            label = { Text("Temperature") },
            leadingIcon = {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = if (showTemperatureLayer) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        )
    }
}

/**
 * A toggle switch for map layers in carousel mode
 */
@Composable
fun LayerToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.5f
            ),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked && enabled,
            onCheckedChange = { if (enabled) onCheckedChange() },
            enabled = enabled
        )
    }
}