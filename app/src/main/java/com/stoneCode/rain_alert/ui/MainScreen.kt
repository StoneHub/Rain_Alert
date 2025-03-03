package com.stoneCode.rain_alert.ui

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
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
    val stationData by weatherViewModel.stationData.observeAsState(emptyList())
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
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // This will add necessary padding for status bar
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Title
                AppTitle(compact = true)
                
                // Navigation icons
                Row {
                    IconButton(onClick = onViewHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View Alert History",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Weather Banner Section - Quick summary of current conditions
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
                    
                    // Weather Radar Map Placeholder
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            }
                        }
                    }

                    // Control Buttons Section - Simplified for service control only
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Service Control",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = if (isServiceRunning) onStopServiceClick else onStartServiceClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(if (isServiceRunning) "Stop Weather Alerts" else "Start Weather Alerts")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onOpenWeatherWebsiteClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Weather Website")
                            }
                        }
                    }
                    
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
                    
                    // Station Data Component
                    if (stationData.isNotEmpty()) {
                        StationDataComponent(stations = stationData)
                    }
                    
                    // Add space at the bottom for better scrolling
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    )
}