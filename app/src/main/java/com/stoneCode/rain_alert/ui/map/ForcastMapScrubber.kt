package com.stoneCode.rain_alert.ui.map

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Data class representing a forecast time step
 * This is used for the forecast animation timeline
 */
data class ForecastTimeStep(
    val timestamp: Long, // Unix timestamp in seconds
    val label: String,   // Formatted time label
    val dateLabel: String // Formatted date label
)

/**
 * A forecast timeline scrubber component that resembles a ruler with tick marks.
 * Used to navigate through different time points in the weather forecast.
 * Enhanced with animation capabilities for radar forecasts.
 *
 * @param modifier Modifier for the component
 * @param isFullScreen Whether the component is displayed in full screen mode
 * @param onTimelineChange Callback when the timeline position changes (0.0-1.0)
 * @param initialPosition Initial position of the timeline (0.0-1.0)
 * @param maxForecastHours Maximum number of hours to display in the forecast timeline
 * @param timeSteps List of specific time steps for the forecast (optional)
 * @param currentTimeIndex Current selected time index (optional, used when timeSteps is provided)
 * @param onTimeStepSelected Callback when a specific time step is selected
 * @param isPlaying Whether the animation is currently playing
 * @param onPlayPauseToggled Callback when play/pause is toggled
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ForecastMapScrubber(
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = true,
    onTimelineChange: (Float) -> Unit = {},
    initialPosition: Float = 0f,
    maxForecastHours: Int = 72, // 3 days forecast
    timeSteps: List<ForecastTimeStep> = emptyList(),
    currentTimeIndex: Int = 0,
    onTimeStepSelected: (Int) -> Unit = {},
    isPlaying: Boolean = false,
    onPlayPauseToggled: () -> Unit = {}
) {
    // Choose the mode based on whether we have specific time steps
    val useContinuousTimeline = timeSteps.isEmpty()
    
    // State for timeline position (0.0 to 1.0)
    var timelinePosition by remember { mutableFloatStateOf(initialPosition) }
    
    // Current time index for discrete time steps
    var currentIndex by remember(currentTimeIndex) { mutableStateOf(currentTimeIndex) }
    
    // Animation state for automatic playback
    val animatedPosition = remember { Animatable(initialPosition) }
    
    // Coroutine scope for launching animations
    val coroutineScope = rememberCoroutineScope()
    
    // Calculate the current time based on the position for continuous timeline
    val currentDateTime by remember(timelinePosition) {
        derivedStateOf {
            if (useContinuousTimeline) {
                val now = LocalDateTime.now()
                val hoursToAdd = (timelinePosition * maxForecastHours).toLong()
                now.plusHours(hoursToAdd)
            } else if (timeSteps.isNotEmpty()) {
                // Use the timestamp from time steps
                val timestamp = timeSteps[currentIndex.coerceIn(0, timeSteps.size - 1)].timestamp
                LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC)
            } else {
                LocalDateTime.now()
            }
        }
    }
    
    // Animation effect when isPlaying is true
    LaunchedEffect(isPlaying, timeSteps, currentIndex) {
        if (isPlaying && timeSteps.isNotEmpty()) {
            // Reset to beginning if at the end
            if (currentIndex >= timeSteps.size - 1) {
                onTimeStepSelected(0)
            } else {
                // Animate through the time steps
                coroutineScope.launch {
                    val animationDuration = 300 // milliseconds per step
                    val targetIndex = (currentIndex + 1) % timeSteps.size
                    
                    animatedPosition.animateTo(
                        targetValue = targetIndex.toFloat() / (timeSteps.size - 1),
                        animationSpec = tween(
                            durationMillis = animationDuration,
                            easing = LinearEasing
                        )
                    )
                    
                    // Update the selected index when animation completes
                    onTimeStepSelected(targetIndex)
                }
            }
        }
    }
    
    // Format for displaying time
    val hourFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd") }
    
    // Density for converting between dp and px
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    // Width of the ruler component
    val rulerWidth = if (isFullScreen) 24.dp else 18.dp
    
    // For continuous timeline: Number of major ticks (representing days)
    val majorTickCount = 4 // 3 days + current
    // Spacing between major ticks
    val majorTickSpacing = 1f / (majorTickCount - 1)
    
    // Component UI
    Box(
        modifier = modifier
            .width(rulerWidth)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
            .background(Color.Black.copy(alpha = 0.15f)) // Semi-transparent background
            .pointerInput(Unit) {
                if (useContinuousTimeline) {
                    // Continuous scrubbing for the continuous timeline
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Convert drag amount to position change (reversed for Y-axis)
                        val dragPercentage = -dragAmount.y / size.height
                        val newPosition = (timelinePosition + dragPercentage).coerceIn(0f, 1f)
                        timelinePosition = newPosition
                        onTimelineChange(newPosition)
                    }
                } else if (timeSteps.isNotEmpty()) {
                    // Tap detection for discrete time points
                    detectTapGestures { tapOffset ->
                        // Convert tap Y position to timeline position
                        val tapPosition = 1f - (tapOffset.y / size.height)
                        val stepCount = timeSteps.size - 1
                        if (stepCount > 0) {
                            // Find the closest time step
                            val targetIndex = (tapPosition * stepCount).toInt().coerceIn(0, stepCount)
                            currentIndex = targetIndex
                            onTimeStepSelected(targetIndex)
                        }
                    }
                }
            }
    ) {
        // Create ruler with tick marks using Canvas
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val tickLength = width * 0.7f // Length of tick marks
            
            // Draw ruler line
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(width * 0.8f, 0f),
                end = Offset(width * 0.8f, height),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
            
            if (useContinuousTimeline) {
                // Continuous timeline mode - draw days and hours
                // Draw major ticks and labels (days)
                for (i in 0 until majorTickCount) {
                    val y = height * (1 - i * majorTickSpacing)
                    val date = LocalDateTime.now().plusDays(i.toLong())
                    
                    // Major tick
                    drawLine(
                        color = Color.White,
                        start = Offset(width * 0.4f, y),
                        end = Offset(width * 0.8f, y),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                    
                    // Day label
                    drawDayLabel(
                        textMeasurer = textMeasurer,
                        day = date.format(dateFormatter),
                        position = Offset(width * 0.3f, y),
                        rotation = 90f
                    )
                }
                
                // Draw minor ticks (every 6 hours)
                val minorTicksPerDay = 4 // Every 6 hours
                for (day in 0 until majorTickCount - 1) {
                    for (tick in 1 until minorTicksPerDay) {
                        val tickPosition = day * majorTickSpacing + (tick / minorTicksPerDay.toFloat()) * majorTickSpacing
                        val y = height * (1 - tickPosition)
                        
                        // Minor tick
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = Offset(width * 0.6f, y),
                            end = Offset(width * 0.8f, y),
                            strokeWidth = 1f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                
                // Draw position indicator (current selection) for continuous timeline
                val indicatorY = height * (1 - timelinePosition)
                
                // Draw indicator line
                drawLine(
                    color = Color.Red,
                    start = Offset(0f, indicatorY),
                    end = Offset(width, indicatorY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                
                // Draw indicator circle
                drawCircle(
                    color = Color.Red,
                    radius = width * 0.25f,
                    center = Offset(width * 0.8f, indicatorY),
                    style = Stroke(width = 2f)
                )
            } else if (timeSteps.isNotEmpty()) {
                // Discrete time steps mode - draw forecast time points
                val stepCount = timeSteps.size
                val stepSpacing = if (stepCount > 1) 1f / (stepCount - 1) else 1f
                
                // Draw time ticks and labels
                timeSteps.forEachIndexed { index, timeStep ->
                    val position = index.toFloat() / max(1, stepCount - 1)
                    val y = height * (1 - position)
                    
                    // Time tick
                    val tickWidth = if (index == currentIndex) 3f else 1.5f
                    val tickAlpha = if (index == currentIndex) 1f else 0.7f
                    val tickColor = if (index == currentIndex) Color.Red else Color.White
                    
                    drawLine(
                        color = tickColor.copy(alpha = tickAlpha),
                        start = Offset(width * 0.5f, y),
                        end = Offset(width * 0.8f, y),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round
                    )
                    
                    // Draw time label for every other tick to avoid crowding
                    if (index % 2 == 0 || index == currentIndex) {
                        drawDayLabel(
                            textMeasurer = textMeasurer,
                            day = timeStep.label,
                            position = Offset(width * 0.3f, y),
                            rotation = 90f,
                            color = if (index == currentIndex) Color.Red else Color.White
                        )
                    }
                }
                
                // Draw the selection indicator for discrete timeline
                val currentPosition = currentIndex.toFloat() / max(1, stepCount - 1)
                val indicatorY = height * (1 - currentPosition)
                
                // Draw indicator circle at current selection
                drawCircle(
                    color = Color.Red,
                    radius = width * 0.25f,
                    center = Offset(width * 0.8f, indicatorY),
                    style = Stroke(width = 2f)
                )
            }
        }
        
        // Current time indicator (displayed at the current position)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = if (useContinuousTimeline) {
                    ((0.5f - timelinePosition) * with(density) { 300.dp.toPx() }).dp
                } else if (timeSteps.isNotEmpty()) {
                    val position = currentIndex.toFloat() / max(1, timeSteps.size - 1)
                    ((0.5f - position) * with(density) { 300.dp.toPx() }).dp
                } else {
                    0.dp
                })
                .padding(start = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time display
                val timeText = if (useContinuousTimeline) {
                    currentDateTime.format(hourFormatter)
                } else if (timeSteps.isNotEmpty() && currentIndex < timeSteps.size) {
                    timeSteps[currentIndex].label
                } else {
                    "--:--"
                }
                
                Text(
                    text = timeText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(0.9f)
                )
                
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Current time",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Play/Pause controls for animation (only visible with time steps)
        if (timeSteps.isNotEmpty()) {
            IconButton(
                onClick = onPlayPauseToggled,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .size(28.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause animation" else "Play animation",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * A compact timeline scrubber for forecast animation, designed for horizontal layout
 * This is an alternative UI for the forecast scrubber, better suited for bottom screen placement
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HorizontalForecastScrubber(
    modifier: Modifier = Modifier,
    timeSteps: List<ForecastTimeStep> = emptyList(),
    currentTimeIndex: Int = 0,
    onTimeStepSelected: (Int) -> Unit = {},
    isPlaying: Boolean = false,
    onPlayPauseToggled: () -> Unit = {}
) {
    if (timeSteps.isEmpty()) return
    
    val stepCount = timeSteps.size
    
    Column(modifier = modifier
        .fillMaxWidth()
        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Time display
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
        ) {
            // Current time label with larger font size for better readability
            Text(
                text = if (currentTimeIndex < timeSteps.size) timeSteps[currentTimeIndex].label else "--:--",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            
            // Current date with larger font and better contrast
            Text(
                text = if (currentTimeIndex < timeSteps.size) timeSteps[currentTimeIndex].dateLabel else "--",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        
        // Time indicators - this helps users see all available time steps
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .padding(horizontal = 40.dp) // Make room for play button
        ) {
            Row(Modifier.fillMaxWidth()) {
                // Create small time indicators
                for (i in 0 until timeSteps.size) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(width = 2.dp, height = if (i == currentTimeIndex) 12.dp else 8.dp)
                                .background(
                                    color = if (i == currentTimeIndex) Color.White else Color.White.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Scrubber controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Play/Pause button with larger size
            IconButton(
                onClick = onPlayPauseToggled,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp) // Larger icon
                )
            }
            
            // Timeline slider with thicker track for better visibility
            Slider(
                value = currentTimeIndex.toFloat(),
                onValueChange = { value ->
                    onTimeStepSelected(value.toInt().coerceIn(0, timeSteps.size - 1))
                },
                valueRange = 0f..(timeSteps.size - 1).toFloat(),
                steps = timeSteps.size - 2,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                )
            )
        }
    }
}

/**
 * Helper function to draw rotated day labels on the Canvas
 */
private fun DrawScope.drawDayLabel(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    day: String,
    position: Offset,
    rotation: Float = 0f,
    color: Color = Color.White
) {
    // Create text style for the label
    val textStyle = TextStyle(
        color = color,
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium
    )
    
    // Measure the text to calculate its dimensions
    val textLayoutResult = textMeasurer.measure(day, textStyle)
    
    // Draw the text with rotation
    rotate(rotation, pivot = position) {
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = position.copy(
                x = position.x - textLayoutResult.size.width / 2,
                y = position.y - textLayoutResult.size.height / 2
            )
        )
    }
}

/**
 * Helper function to prevent division by zero
 */
private fun max(a: Int, b: Int): Int {
    return if (a > b) a else b
}

/**
 * Preview function for the ForecastMapScrubber with continuous timeline
 */
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ForecastMapScrubberPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray)
        ) {
            // Background to simulate map
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF4CAF50).copy(alpha = 0.5f))
            )

            // Timeline at the left side
            ForecastMapScrubber(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .fillMaxHeight(0.8f),
                isFullScreen = true
            )
        }
    }
}

/**
 * Preview function for the ForecastMapScrubber with time steps
 */
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ForecastMapScrubberWithTimeStepsPreview() {
    val timeSteps = List(12) { index ->
        val time = LocalDateTime.now().plusHours(index.toLong())
        ForecastTimeStep(
            timestamp = time.toEpochSecond(ZoneOffset.UTC),
            label = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            dateLabel = time.format(DateTimeFormatter.ofPattern("MMM dd"))
        )
    }
    
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray)
        ) {
            // Background to simulate map
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF4CAF50).copy(alpha = 0.5f))
            )

            // Timeline at the left side with time steps
            ForecastMapScrubber(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .fillMaxHeight(0.8f),
                isFullScreen = true,
                timeSteps = timeSteps,
                currentTimeIndex = 3,
                isPlaying = true
            )
        }
    }
}

/**
 * Preview function for the horizontal scrubber
 */
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, widthDp = 360, heightDp = 100)
@Composable
fun HorizontalForecastScrubberPreview() {
    val timeSteps = List(12) { index ->
        val time = LocalDateTime.now().plusHours(index.toLong())
        ForecastTimeStep(
            timestamp = time.toEpochSecond(ZoneOffset.UTC),
            label = time.format(DateTimeFormatter.ofPattern("HH:mm")),
            dateLabel = time.format(DateTimeFormatter.ofPattern("MMM dd"))
        )
    }
    
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color.DarkGray)
        ) {
            HorizontalForecastScrubber(
                modifier = Modifier.align(Alignment.BottomCenter),
                timeSteps = timeSteps,
                currentTimeIndex = 2
            )
        }
    }
}
