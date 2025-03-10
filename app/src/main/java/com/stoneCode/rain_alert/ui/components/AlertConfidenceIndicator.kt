package com.stoneCode.rain_alert.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stoneCode.rain_alert.model.AlertConfidence
import com.stoneCode.rain_alert.model.ConfidenceLevel

/**
 * A component that displays the confidence level of a weather alert with factors
 * that contributed to that confidence rating
 */
@Composable
fun AlertConfidenceIndicator(
    confidence: AlertConfidence,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Alert Confidence: ",
                style = MaterialTheme.typography.bodyMedium
            )
            
            val color = when(confidence.level) {
                ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.primary
                ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                ConfidenceLevel.LOW -> MaterialTheme.colorScheme.error
            }
            
            Text(
                text = confidence.level.name,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
        
        // Confidence bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(confidence.score)
                    .background(
                        when(confidence.level) {
                            ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.primary
                            ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
                            ConfidenceLevel.LOW -> MaterialTheme.colorScheme.error
                        }
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Confidence factors
        Column {
            confidence.factors.forEach { factor ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = factor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
