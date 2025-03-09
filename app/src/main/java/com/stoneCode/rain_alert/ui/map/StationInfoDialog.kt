package com.stoneCode.rain_alert.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.stoneCode.rain_alert.api.WeatherStation
import java.util.Locale

/**
 * Dialog to display information about a selected weather station.
 * Different styles based on display mode.
 */
@Composable
fun StationInfoDialog(
    station: WeatherStation,
    onDismiss: () -> Unit,
    isFullscreen: Boolean
) {
    if (isFullscreen) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = station.name) },
            text = {
                Column {
                    Text("Distance: ${String.format("%.1f", station.distance)} km")
                    Spacer(modifier = Modifier.height(4.dp))
                    station.stationUrl?.let { url ->
                        if (url.isNotEmpty()) {
                            Text("Station URL: $url")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Station: ${station.name}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Distance: ${String.format(Locale.US, "%.1f", station.distance)} km",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    station.stationUrl?.let { url ->
                        if (url.isNotEmpty()) {
                            Text("Station URL: $url", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}