package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppTitle(compact: Boolean = false) {
    // For compact mode (app bar), use on-surface color to match other component titles
    // For full mode, use white text for better contrast on background image
    val titleColor = if (compact) {
        MaterialTheme.colorScheme.onSurface
    } else if (isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.95f) // Very slightly transparent white for dark mode
    } else {
        Color.White.copy(alpha = 0.9f) // Slightly transparent white for light mode
    }

    if (compact) {
        Text(
            text = "Rain Alert",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = titleColor
            ),
            textAlign = TextAlign.Start,
            softWrap = true,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 4.dp)
        )
    } else {
        Text(
            text = "Rain Alert",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = titleColor
            ),
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            textAlign = TextAlign.Center,
            softWrap = true,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "by StoneCode",
            style = MaterialTheme.typography.bodySmall.copy(
                color = titleColor.copy(alpha = 0.8f), // Slightly more transparent for subtitle
                fontWeight = FontWeight.Light
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Preview(name = "App Title Compact (Normal)", showBackground = true)
@Composable
fun AppTitleCompactPreview() {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AppTitle(compact = true)
                }

                // Simulate the switch
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Alerts On", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

@Preview(name = "App Title Compact (Narrow)", showBackground = true)
@Composable
fun AppTitleCompactNarrowPreview() {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(240.dp) // Simulate narrow screen
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AppTitle(compact = true)
                }

                // Simulate the switch
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Alerts On", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

@Preview(name = "App Title Full", showBackground = true)
@Composable
fun AppTitleFullPreview() {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AppTitle(compact = false)
            }
        }
    }
