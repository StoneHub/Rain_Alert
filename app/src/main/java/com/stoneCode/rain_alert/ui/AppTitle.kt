package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AppTitle() {
    Text(
        text = "Rain Alert",
        style = MaterialTheme.typography.displaySmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        textAlign = TextAlign.Center
    )
    Text(
        text = "by StoneCode",
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Light
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}