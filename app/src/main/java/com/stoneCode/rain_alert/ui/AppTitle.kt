package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            textAlign = TextAlign.Start
        )
    } else {
        Text(
            text = "Rain Alert",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = titleColor
            ),
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            textAlign = TextAlign.Center
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