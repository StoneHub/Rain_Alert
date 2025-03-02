package com.stoneCode.rain_alert.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.StoneCode.rain_alert.R
import com.stoneCode.rain_alert.viewmodel.WeatherViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    onStartServiceClick: () -> Unit,
    onStopServiceClick: () -> Unit,
    onSimulateFreezeClick: () -> Unit,
    onSimulateRainClick: () -> Unit,
    onOpenWeatherWebsiteClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewHistoryClick: () -> Unit,
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val isServiceRunning by weatherViewModel.isServiceRunning.observeAsState(false)
    val lastUpdateTime by weatherViewModel.lastUpdateTime.observeAsState("")
    val isDataReady by weatherViewModel.isDataReady.observeAsState(false)
    val apiStatus by weatherViewModel.apiStatus.observeAsState()
    var weatherData by remember { mutableStateOf("Loading...") }
    var isRefreshing by remember { mutableStateOf(false) }
    var longPressDetected by remember { mutableStateOf(false) }
    var initialContainerSize by remember { mutableStateOf(0.dp) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                weatherViewModel.updateWeatherStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            weatherViewModel.stopServiceChecker()
        }
    }

    LaunchedEffect(weatherViewModel.weatherData, isRefreshing, isDataReady) {
        weatherViewModel.weatherData.value?.let { data ->
            if (!isRefreshing) {
                weatherData = data
            } else {
                if (!longPressDetected) {
                    delay(500)
                }

                if (isDataReady) {
                    weatherData = data
                    isRefreshing = false
                    longPressDetected = false
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Column {
                // History button
                FloatingActionButton(
                    onClick = onViewHistoryClick,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "View Alert History"
                    )
                }
                
                // Settings button
                FloatingActionButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        },
        content = { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Image
                Image(
                    painter = painterResource(id = R.drawable.background_nature),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // App Title
                    AppTitle()

                    // Weather Banner Section
                    WeatherBanner(
                        weatherData = weatherData,
                        lastUpdateTime = lastUpdateTime,
                        isRefreshing = isRefreshing,
                        longPressDetected = longPressDetected,
                        onLongPress = {
                            longPressDetected = true
                            isRefreshing = true
                            weatherViewModel.updateWeatherStatus()
                        },
                        weatherViewModel = weatherViewModel,
                        onSizeCalculated = { size ->
                            if (initialContainerSize < size) {
                                initialContainerSize = size
                            }
                        },
                        containerSize = initialContainerSize
                    )

                    // Control Buttons Section
                    ControlButtons(
                        isServiceRunning = isServiceRunning,
                        onStartServiceClick = onStartServiceClick,
                        onStopServiceClick = onStopServiceClick,
                        onSimulateRainClick = onSimulateRainClick,
                        onSimulateFreezeClick = onSimulateFreezeClick,
                        onOpenWeatherWebsiteClick = onOpenWeatherWebsiteClick
                    )
                    
                    // API Status Section with refresh button - Removed external header as it's inside the component now
                    apiStatus?.let { status ->
                        ApiStatusWidget(
                            apiStatus = status.copy(
                                serviceProvider = "National Weather Service",
                                locationInfo = weatherViewModel.currentLocation.value,
                                rawApiData = weatherViewModel.rawApiData.value
                            ),
                            onRefreshClick = { 
                                isRefreshing = true
                                weatherViewModel.refreshWeatherData() 
                            },
                            isRefreshing = isRefreshing
                        )
                    }
                    
                    // Current Weather Details Card
                    if (!isRefreshing && weatherData.isNotEmpty() && weatherData != "Loading...") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Current Weather",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    // Location indicator if available
                                    weatherViewModel.currentLocation.value?.let { location ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Location",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = location,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = weatherData,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    // Add space at the bottom for better scrolling
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    )
}