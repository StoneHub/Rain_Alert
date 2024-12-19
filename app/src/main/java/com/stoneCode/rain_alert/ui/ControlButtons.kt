package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ControlButtons(
    isServiceRunning: Boolean,
    onStartServiceClick: () -> Unit,
    onStopServiceClick: () -> Unit,
    onSimulateRainClick: () -> Unit,
    onSimulateFreezeClick: () -> Unit,
    onOpenWeatherWebsiteClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 16.dp)
    ) {
        ActionButton(
            text = if (isServiceRunning) "Stop Service" else "Start Service",
            onClick = if (isServiceRunning) onStopServiceClick else onStartServiceClick,
            backgroundColor = if (isServiceRunning) Color.Red else Color.Green
        )
        ActionButton(text = "Simulate Rain", onClick = onSimulateRainClick)
        ActionButton(text = "Simulate Freeze", onClick = onSimulateFreezeClick)
        ActionButton(text = "Open Weather Website", onClick = onOpenWeatherWebsiteClick)
    }
}

@Composable
fun ActionButton(text: String, onClick: () -> Unit, backgroundColor: Color = MaterialTheme.colorScheme.primary) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier.padding(8.dp)
        )
    }
}