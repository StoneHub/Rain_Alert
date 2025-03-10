package com.stoneCode.rain_alert.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoneCode.rain_alert.model.StationContribution

/**
 * A composable for visualizing each weather station's contribution to the
 * algorithm decision with a horizontal bar chart
 */
@Composable
fun StationContributionChart(
    stationContributions: List<StationContribution>,
    modifier: Modifier = Modifier,
    title: String = "Station Contributions to Alert Decision"
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        stationContributions.forEach { station ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Station name
                Text(
                    text = station.stationName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(120.dp)
                )
                
                // Weight visualization
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(
                            color = if (station.isPositive)
                                MaterialTheme.colorScheme.primary.copy(alpha = station.weight.toFloat())
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = station.weight.toFloat()),
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    Text(
                        text = "${station.temperature?.let { String.format("%.1f", it) } ?: "--"}°F",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    )
                }
                
                // Distance indicator
                Text(
                    text = "${String.format("%.1f", station.distance)} km",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(60.dp).padding(start = 8.dp)
                )
            }
        }
    }
}
