package com.stoneCode.rain_alert.ui.map

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.viewmodel.RadarMapViewModel
import kotlinx.coroutines.launch

/**
 * A weather map component that shows current weather data overlays and station information.
 * Simplified to focus on current weather conditions without forecast features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherMapScreen(
    modifier: Modifier = Modifier,
    centerLatLng: LatLng = LatLng(40.0, -98.0),
    myLocation: LatLng? = null,
    selectedStations: List<WeatherStation> = emptyList(),
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onMyLocationClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    radarMapViewModel: RadarMapViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    
    // State
    var showPrecipitationLayer by remember { mutableStateOf(true) }
    var showWindLayer by remember { mutableStateOf(false) }
    var showTemperatureLayer by remember { mutableStateOf(false) }
    var selectedStation by remember { mutableStateOf<WeatherStation?>(null) }
    
    // Observe data from ViewModel
    val precipitationRadarUrl by radarMapViewModel.precipitationRadarUrl.observeAsState()
    val windRadarUrl by radarMapViewModel.windRadarUrl.observeAsState()
    val temperatureRadarUrl by radarMapViewModel.temperatureRadarUrl.observeAsState()
    val isRadarLoading by radarMapViewModel.isLoading.observeAsState(false)
    
    // Map camera state (with default position)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(myLocation ?: centerLatLng, 9f)
    }
    
    // Initialize map at user location if available
    val initialized = remember { mutableStateOf(false) }
    LaunchedEffect(myLocation) {
        if (!initialized.value && myLocation != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(myLocation, 12f)
            radarMapViewModel.updateMapCenter(myLocation)
            initialized.value = true
        }
    }
    
    // Track camera position changes
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && initialized.value) {
            radarMapViewModel.updateMapCenter(cameraPositionState.position.target)
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Top Bar with title and back button
        TopAppBar(
            title = { Text("Current Weather") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
        
        // Weather layer controls
        WeatherControls(
            onPrecipitationToggle = { showPrecipitationLayer = !showPrecipitationLayer },
            onWindLayerToggle = { showWindLayer = !showWindLayer },
            onTemperatureToggle = { showTemperatureLayer = !showTemperatureLayer },
            showPrecipitationLayer = showPrecipitationLayer,
            showWindLayer = showWindLayer,
            showTemperatureLayer = showTemperatureLayer,
            modifier = Modifier.padding(8.dp)
        )
        
        // Loading indicator
        if (isLoading || isRadarLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        
        // Map with weather overlays
        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = MapType.TERRAIN,
                    isMyLocationEnabled = myLocation != null
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                ),
                onMapLoaded = {
                    Log.d("WeatherMapScreen", "Map loaded")
                    if (myLocation != null && !initialized.value) {
                        radarMapViewModel.updateMapCenter(myLocation)
                        initialized.value = true
                    }
                }
            ) {
                // Temperature overlay (lowest z-index so it appears below other layers)
                if (showTemperatureLayer && temperatureRadarUrl != null) {
                    WeatherOverlay(
                        imageUrl = temperatureRadarUrl,
                        visible = true,
                        transparency = 0.4f,
                        zIndex = 0f
                    )
                }
                
                // Precipitation overlay (middle z-index)
                if (showPrecipitationLayer && precipitationRadarUrl != null) {
                    WeatherOverlay(
                        imageUrl = precipitationRadarUrl,
                        visible = true,
                        transparency = 0.3f,
                        zIndex = 1f
                    )
                }
                
                // Wind overlay (highest z-index so it appears on top)
                if (showWindLayer && windRadarUrl != null) {
                    WeatherOverlay(
                        imageUrl = windRadarUrl,
                        visible = true,
                        transparency = 0.4f,
                        zIndex = 2f
                    )
                }
                
                // Display weather station markers
                selectedStations.forEach { station ->
                    Marker(
                        state = MarkerState(position = LatLng(station.latitude, station.longitude)),
                        title = station.name,
                        snippet = "Distance: ${String.format("%.1f", station.distance)} km",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        onClick = {
                            selectedStation = station
                            true
                        }
                    )
                }
                
                // User location marker
                myLocation?.let { location ->
                    Circle(
                        center = location,
                        radius = 500.0, // 500 meters
                        fillColor = Color.Blue.copy(alpha = 0.15f),
                        strokeColor = Color.Blue.copy(alpha = 0.8f),
                        strokeWidth = 2f
                    )
                }
            }
            
            // Map control buttons
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // My location button
                    FloatingActionButton(
                        onClick = {
                            onMyLocationClick()
                            myLocation?.let { loc ->
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition(loc, 12f, 0f, 0f)
                                        ),
                                        1000
                                    )
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "My Location",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    // Refresh button
                    FloatingActionButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        
        // Station info dialog
        if (selectedStation != null) {
            AlertDialog(
                onDismissRequest = { selectedStation = null },
                title = { Text(text = selectedStation!!.name) },
                text = {
                    Column {
                        Text("Distance: ${String.format("%.1f", selectedStation!!.distance)} km")
                        Spacer(modifier = Modifier.height(4.dp))
                        selectedStation?.stationUrl?.let { url ->
                            if (url.isNotEmpty()) {
                                Text("Station URL: $url")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedStation = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

/**
 * Weather layer control chips for toggling different weather data layers.
 */
@Composable
fun WeatherControls(
    onPrecipitationToggle: () -> Unit,
    onWindLayerToggle: () -> Unit,
    onTemperatureToggle: () -> Unit,
    showPrecipitationLayer: Boolean,
    showWindLayer: Boolean,
    showTemperatureLayer: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Precipitation layer toggle
        FilterChip(
            selected = showPrecipitationLayer,
            onClick = onPrecipitationToggle,
            label = { Text("Precipitation") },
            leadingIcon = {
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = if (showPrecipitationLayer) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        )
        
        // Wind layer toggle
        FilterChip(
            selected = showWindLayer,
            onClick = onWindLayerToggle,
            label = { Text("Wind") },
            leadingIcon = {
                Icon(
                    Icons.Default.AirplanemodeActive,
                    contentDescription = null,
                    tint = if (showWindLayer) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        )
        
        // Temperature layer toggle
        FilterChip(
            selected = showTemperatureLayer,
            onClick = onTemperatureToggle,
            label = { Text("Temperature") },
            leadingIcon = {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = null,
                    tint = if (showTemperatureLayer) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        )
    }
}