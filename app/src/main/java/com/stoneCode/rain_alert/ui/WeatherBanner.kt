package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onSizeCalculated: (Dp) -> Unit, // Remove the @Composable annotation
    containerSize: Dp
) {
    // Get the density outside of the modifier callback
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .background(
                MaterialTheme.colorScheme.secondary,
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
            .height(containerSize),
        contentAlignment = Alignment.Center
    ) {
        // Manual Ripple Effect with Overlay
        Surface(
            color = MaterialTheme.colorScheme.secondary,
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
            Text(
                text = "Latest Weather Data",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            )
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
                        color = MaterialTheme.colorScheme.onSecondary
                    ),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Last Updated: $lastUpdateTime",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSecondary
                )
            )
        }
    }
}