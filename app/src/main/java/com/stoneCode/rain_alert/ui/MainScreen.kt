package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.StoneCode.rain_alert.R
import com.stoneCode.rain_alert.ui.dialogs.LocationDialog
import com.stoneCode.rain_alert.ui.dialogs.StationSelectDialog
import com.stoneCode.rain_alert.viewmodel.WeatherViewModel
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.ui.map.MapDisplayMode
import com.stoneCode.rain_alert.ui.map.SharedMapComponent
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    onStartServiceClick: () -> Unit,
    onStopServiceClick: () -> Unit,
    onOpenWeatherWebsiteClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewHistoryClick: () -> Unit,
    onOpenStationWebsiteClick: (String) -> Unit,  // New function to open station website
    onMapClick: () -> Unit, // Function to open the weather map
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val isServiceRunning by weatherViewModel.isServiceRunning.observeAsState(false)
    val lastUpdateTime by weatherViewModel.lastUpdateTime.observeAsState("")
    val isDataReady by weatherViewModel.isDataReady.observeAsState(false)
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Title with improved visibility
                Box(modifier = Modifier.weight(1f)) {
                    AppTitle(compact = true)
                }
                
                // Weather Alert Service Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { isChecked ->
                            if (isChecked) onStartServiceClick() else onStopServiceClick()
                        },
                        thumbContent = {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isServiceRunning) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                
                // Navigation icons
                Row {
                    IconButton(onClick = onViewHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View Alert History",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        content = { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Image - Choose based on dark mode
                val backgroundResId = if (isSystemInDarkTheme()) {
                    R.drawable.background_nature_dark
                } else {
                    R.drawable.background_nature
                }
                
                Image(
                    painter = painterResource(id = backgroundResId),
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
                    
                    // Dialog state
                    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
                    var showStationSelectDialog by rememberSaveable { mutableStateOf(false) }
                    val availableStations by weatherViewModel.availableStations.observeAsState(emptyList())
                    val selectedStationIds by weatherViewModel.selectedStationIds.observeAsState(emptyList())
                    
                    // Launch effect to fetch fresh stations when dialog is about to show
                    LaunchedEffect(showStationSelectDialog) {
                        if (showStationSelectDialog) {
                            // Fetch fresh stations when dialog opens
                            weatherViewModel.fetchAvailableStations()
                        }
                    }
                    
                    // Filter stationData to only include selected stations
                    val displayedStations = stationData.filter { stationObs ->
                        selectedStationIds.contains(stationObs.station.id)
                    }
                    
                    // Weather Map Component
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            // Map container with fixed height
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            ) {
                                // Get center from stations or use default
                                val center = if (displayedStations.isNotEmpty()) {
                                    val lat = displayedStations.map { it.station.latitude }.average()
                                    val lng = displayedStations.map { it.station.longitude }.average()
                                    com.google.android.gms.maps.model.LatLng(lat, lng)
                                } else {
                                    // Default US center
                                    com.google.android.gms.maps.model.LatLng(40.0, -98.0)
                                }
                                
                                // Render map component
                                SharedMapComponent(
                                    modifier = Modifier.fillMaxSize(),
                                    displayMode = MapDisplayMode.CAROUSEL,
                                    centerLatLng = center,
                                    selectedStations = displayedStations.map { it.station },
                                    isLoading = isRefreshing,
                                    onRefresh = {
                                        isRefreshing = true
                                        weatherViewModel.updateWeatherStatus()
                                    },
                                    onMyLocationClick = {
                                        // Center map on user's location without refreshing data
                                        weatherViewModel.getLastKnownLocation()?.let { location ->
                                            // Let the SharedMapComponent handle the camera movement
                                        }
                                    },
                                    onToggleFullScreen = onMapClick,
                                    onChangeLocationClick = { showLocationDialog = true }
                                )
                            }
                            
                            Text(
                                text = "Weather Radar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    // Station Data Component
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        if (displayedStations.isNotEmpty()) {
                            StationDataComponent(
                                stations = displayedStations,
                                onSelectStationsClick = { showStationSelectDialog = true },
                                onStationLongClick = onOpenStationWebsiteClick,
                                isLoading = isRefreshing
                            )
                        } else {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = "No Stations",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No weather stations selected", style = MaterialTheme.typography.bodyLarge)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { showStationSelectDialog = true }) {
                                        Text("Select Stations")
                                    }
                                }
                            }
                        }
                    }
                    
                    // Location Dialog
                    if (showLocationDialog) {
                        LocationDialog(
                            onDismiss = { showLocationDialog = false },
                            onConfirm = { zipCode ->
                                weatherViewModel.updateCustomLocation(zipCode, true)
                                showLocationDialog = false
                            },
                            onUseDeviceLocation = {
                                weatherViewModel.updateCustomLocation(null, false)
                                showLocationDialog = false
                            }
                        )
                    }
                    
                    // Station Select Dialog
                    if (showStationSelectDialog) {
                        StationSelectDialog(
                            stations = availableStations,
                            selectedStationIds = selectedStationIds,
                            onDismiss = { showStationSelectDialog = false },
                            onConfirm = { stationIds ->
                                weatherViewModel.updateSelectedStations(stationIds)
                                showStationSelectDialog = false
                            }
                        )
                    }
                    
                    // Add space at the bottom for better scrolling
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    )
}