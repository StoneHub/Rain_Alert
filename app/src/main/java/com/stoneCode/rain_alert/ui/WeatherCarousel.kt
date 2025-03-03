package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stoneCode.rain_alert.data.StationObservation
import com.stoneCode.rain_alert.viewmodel.WeatherViewModel

@Composable
fun WeatherCarousel(
    weatherData: String,
    lastUpdateTime: String,
    isRefreshing: Boolean,
    longPressDetected: Boolean,
    onLongPress: () -> Unit,
    weatherViewModel: WeatherViewModel,
    onSizeCalculated: (androidx.compose.ui.unit.Dp) -> Unit,
    containerSize: androidx.compose.ui.unit.Dp,
    stationData: List<StationObservation>,
    onChangeLocationClick: () -> Unit,
    onSelectStationsClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    rememberCoroutineScope()
    
    Column(modifier = Modifier.fillMaxWidth()) {

        // The pager with three cards - all with fixed height
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (containerSize < 250.dp) 250.dp else containerSize) // Use larger minimum height
        ) { page ->
            when (page) {
                0 -> {
                    // Weather Banner
                    WeatherBanner(
                        weatherData = weatherData,
                        lastUpdateTime = lastUpdateTime,
                        isRefreshing = isRefreshing,
                        longPressDetected = longPressDetected,
                        onLongPress = onLongPress,
                        weatherViewModel = weatherViewModel,
                        onSizeCalculated = onSizeCalculated,
                        containerSize = containerSize,
                        showLocationButton = true,
                        onLocationClick = onChangeLocationClick
                    )
                }
                1 -> {
                    // Weather Radar Map
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Weather Radar Map",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Weather Radar Map", style = MaterialTheme.typography.bodyLarge)
                                Text("Coming Soon", style = MaterialTheme.typography.bodyMedium)

                                Spacer(modifier = Modifier.height(8.dp))

                                IconButton(onClick = onChangeLocationClick) {
                                    Icon(
                                        imageVector = Icons.Default.LocationSearching,
                                        contentDescription = "Change Location",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Station Data with selection button
                    // Station Data card - show either data or empty state with same size
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        if (stationData.isNotEmpty()) {
                            StationDataComponent(
                                stations = stationData,
                                onSelectStationsClick = onSelectStationsClick
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Weather Stations",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No weather stations available", style = MaterialTheme.typography.bodyLarge)
                                    Text("Pull to refresh or change location", style = MaterialTheme.typography.bodyMedium)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    IconButton(onClick = onSelectStationsClick) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Select Stations",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }

        // Card titles
        Text(
            text = when (pagerState.currentPage) {
                0 -> "Current Weather"
                1 -> "Weather Radar"
                2 -> "Nearby Stations"
                else -> ""
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
        )

        // The page indicator
        Row(
            modifier = Modifier
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { page ->
                val selected = page == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .size(if (selected) 10.dp else 8.dp)
                )
            }
        }
    }
