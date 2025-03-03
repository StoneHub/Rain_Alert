package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stoneCode.rain_alert.viewmodel.WeatherViewModel
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeatherBanner(
    weatherData: String,
    lastUpdateTime: String,
    isRefreshing: Boolean,
    longPressDetected: Boolean,
    onLongPress: () -> Unit,
    weatherViewModel: WeatherViewModel,
    onSizeCalculated: (Dp) -> Unit,
    containerSize: Dp,
    showLocationButton: Boolean = false,
    onLocationClick: () -> Unit = {}
) {
    // Get the density outside of the modifier callback
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(
                MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    // Regular tap action (if needed)
                },
                onLongClick = onLongPress,
                indication = null, // Remove default ripple
                interactionSource = remember { MutableInteractionSource() }
            )
            .semantics { role = Role.Button }
            .onGloballyPositioned { coordinates ->
                // Use density that was captured in the composable, not in the callback
                val heightDp = with(density) { coordinates.size.height.toDp() }
                onSizeCalculated(heightDp)
            }
            // Set a minimum height to ensure visibility
            .height(if (containerSize < 150.dp) 150.dp else containerSize),
        contentAlignment = Alignment.Center
    ) {
        // Manual Ripple Effect with Overlay
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (longPressDetected) Color.DarkGray.copy(alpha = 0.2f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }

        // Content of the weather box
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Latest Weather Data",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                if (showLocationButton) {
                    IconButton(
                        onClick = onLocationClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Change Location",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (isRefreshing) {
                ScrambleText(
                    text = weatherData,
                    isStatic = !isRefreshing || !weatherViewModel.isDataReady.value!!,
                    onAnimationFinished = {
                        if (weatherViewModel.isDataReady.value!!) {
                            weatherViewModel.weatherData.value?.let {
                                // Empty for now
                            }
                        }
                    }
                )
            } else {
                Text(
                    text = weatherData,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Last Updated: $lastUpdateTime",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}