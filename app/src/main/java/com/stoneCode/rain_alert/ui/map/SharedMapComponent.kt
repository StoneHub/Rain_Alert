package com.stoneCode.rain_alert.ui.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.viewmodel.RadarMapViewModel
import kotlinx.coroutines.launch

/**
 * Display mode for the map component
 */
enum class MapDisplayMode {
    CAROUSEL,  // Small, static view shown in the carousel
    FULLSCREEN // Large, interactive view shown in full screen
}

/**
 * A shared map component that can be used in both carousel and fullscreen modes.
 * Centralizes the common functionality between different map views.
 */
@Composable
fun SharedMapComponent(
    modifier: Modifier = Modifier,
    displayMode: MapDisplayMode = MapDisplayMode.FULLSCREEN,
    centerLatLng: LatLng = LatLng(40.0, -98.0),
    myLocation: LatLng? = null,
    selectedStations: List<WeatherStation> = emptyList(),
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onMyLocationClick: () -> Unit = {},
    onToggleFullScreen: () -> Unit = {},
    onChangeLocationClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    radarMapViewModel: RadarMapViewModel = viewModel(),
) {
    // Coroutine scope for animation
    val coroutineScope = rememberCoroutineScope()
    
    // Track selected station for info dialog
    var selectedStation by remember { mutableStateOf<WeatherStation?>(null) }
    
    // Observe data from ViewModel
    val activeLayer by radarMapViewModel.activeLayer.observeAsState(RadarMapViewModel.WeatherLayer.PRECIPITATION)
    val precipitationRadarUrl by radarMapViewModel.precipitationRadarUrl.observeAsState()
    val windRadarUrl by radarMapViewModel.windRadarUrl.observeAsState()
    val temperatureRadarUrl by radarMapViewModel.temperatureRadarUrl.observeAsState()
    val isRadarLoading by radarMapViewModel.isLoading.observeAsState(false)
    val lastLocation by radarMapViewModel.lastKnownLocation.observeAsState()
    
    // Debug logging for layer state
    LaunchedEffect(activeLayer, precipitationRadarUrl, windRadarUrl, temperatureRadarUrl) {
        Log.d("SharedMapComponent", "Display Mode: $displayMode")
        Log.d("SharedMapComponent", "Active Layer: $activeLayer")
        Log.d("SharedMapComponent", "Precipitation URL: $precipitationRadarUrl")
        Log.d("SharedMapComponent", "Wind URL: $windRadarUrl")
        Log.d("SharedMapComponent", "Temperature URL: $temperatureRadarUrl")
    }
    
    // Determine if controls should be shown for carousel mode
    var showControls by remember { mutableStateOf(false) }
    
    // Show triangle only if we have at least 3 stations
    val showTriangleLayer = remember(selectedStations) { selectedStations.size >= 3 }
    
    // Map camera state
    val initialZoom = when(displayMode) {
        MapDisplayMode.CAROUSEL -> 9f
        MapDisplayMode.FULLSCREEN -> 12f
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            myLocation ?: centerLatLng, 
            initialZoom
        )
    }
    
    // Initialize map only once
    val initialized = remember { mutableStateOf(false) }
    
    // Center on user location if available
    LaunchedEffect(myLocation, displayMode) {
        if (!initialized.value) {
            val locationToUse = myLocation ?: lastLocation ?: centerLatLng
            val zoom = if (displayMode == MapDisplayMode.FULLSCREEN) 12f else 9f
            
            cameraPositionState.position = CameraPosition.fromLatLngZoom(locationToUse, zoom)
            radarMapViewModel.updateMapCenter(locationToUse)
            radarMapViewModel.updateMapZoom(zoom)
            
            if (myLocation != null) {
                radarMapViewModel.updateLastKnownLocation(myLocation)
            }
            
            initialized.value = true
        }
    }
    
    // Track camera position changes
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && initialized.value) {
            val currentPos = cameraPositionState.position
            val visibleBounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
            
            if (visibleBounds != null) {
                radarMapViewModel.updateMapCamera(
                    currentPos.target,
                    currentPos.zoom,
                    visibleBounds
                )
                
                // After updating the camera and bounds, refresh the radar data
                if (activeLayer == RadarMapViewModel.WeatherLayer.PRECIPITATION || 
                    activeLayer == RadarMapViewModel.WeatherLayer.WIND || 
                    activeLayer == RadarMapViewModel.WeatherLayer.TEMPERATURE) {
                    radarMapViewModel.fetchRadarData(currentPos.target)
                }
            }
        }
    }
    
    // UI based on display mode
    when (displayMode) {
        MapDisplayMode.FULLSCREEN -> {
            Column(modifier = modifier.fillMaxSize()) {
                // Top Bar with title and back button
                if (displayMode == MapDisplayMode.FULLSCREEN) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    TopAppBar(
                        title = { Text("Current Weather") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                    
                    // Weather layer controls
                    WeatherControls(
                        onPrecipitationToggle = { 
                            radarMapViewModel.toggleLayer(RadarMapViewModel.WeatherLayer.PRECIPITATION) 
                        },
                        onWindLayerToggle = { 
                            radarMapViewModel.toggleLayer(RadarMapViewModel.WeatherLayer.WIND) 
                        },
                        onTemperatureToggle = { 
                            radarMapViewModel.toggleLayer(RadarMapViewModel.WeatherLayer.TEMPERATURE) 
                        },
                        showPrecipitationLayer = activeLayer == RadarMapViewModel.WeatherLayer.PRECIPITATION,
                        showWindLayer = activeLayer == RadarMapViewModel.WeatherLayer.WIND,
                        showTemperatureLayer = activeLayer == RadarMapViewModel.WeatherLayer.TEMPERATURE,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                // Loading indicator
                if (isLoading || isRadarLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                
                // Map with weather overlays
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    MapContent(
                        cameraPositionState = cameraPositionState,
                        myLocation = myLocation,
                        selectedStations = selectedStations,
                        showTriangleLayer = showTriangleLayer,
                        onMarkerClick = { station -> selectedStation = station },
                        activeLayer = activeLayer,
                        precipitationRadarUrl = precipitationRadarUrl,
                        windRadarUrl = windRadarUrl,
                        temperatureRadarUrl = temperatureRadarUrl,
                        onMapLoaded = {
                            if (myLocation != null && !initialized.value) {
                                radarMapViewModel.updateMapCenter(myLocation)
                                initialized.value = true
                            }
                        },
                        fullScreen = true
                    )
                    
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
            }
        }
        
        MapDisplayMode.CAROUSEL -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                MapContent(
                    cameraPositionState = cameraPositionState,
                    myLocation = myLocation,
                    selectedStations = selectedStations,
                    showTriangleLayer = showTriangleLayer,
                    onMarkerClick = { station -> selectedStation = station },
                    activeLayer = activeLayer,
                    precipitationRadarUrl = precipitationRadarUrl,
                    windRadarUrl = windRadarUrl,
                    temperatureRadarUrl = temperatureRadarUrl,
                    onMapLoaded = {
                        Log.d("SharedMapComponent", "Map loaded in carousel mode")
                    },
                    fullScreen = false
                )
                
                // Loading overlay
                if (isLoading || isRadarLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                // Top-left: fullscreen toggle
                IconButton(
                    onClick = onToggleFullScreen,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Full Screen",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Top-right: location controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            onMyLocationClick()
                            myLocation?.let { loc ->
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition(loc, 9f, 0f, 0f)
                                        ),
                                        1000
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "My Location",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = onChangeLocationClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Set Manual Location",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Bottom-end: show/hide layer controls
                IconButton(
                    onClick = { showControls = !showControls },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (showControls) Icons.Default.VisibilityOff else Icons.Default.Layers,
                        contentDescription = if (showControls) "Hide Controls" else "Show Layer Controls",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // If controls are visible, show them
                if (showControls) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 56.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // Layer toggle controls
                            Text("Weather Layers", style = MaterialTheme.typography.titleSmall)
                            
                            // Precipitation toggle
                            LayerToggle(
                                label = "Precipitation",
                                checked = activeLayer == RadarMapViewModel.WeatherLayer.PRECIPITATION,
                                onCheckedChange = { 
                                    radarMapViewModel.toggleLayer(RadarMapViewModel.WeatherLayer.PRECIPITATION) 
                                }
                            )
                            
                            // Wind toggle
                            LayerToggle(
                                label = "Wind",
                                checked = activeLayer == RadarMapViewModel.WeatherLayer.WIND,
                                onCheckedChange = { 
                                    radarMapViewModel.toggleLayer(RadarMapViewModel.WeatherLayer.WIND) 
                                }
                            )
                            
                            // Temperature toggle
                            LayerToggle(
                                label = "Temperature",
                                checked = activeLayer == RadarMapViewModel.WeatherLayer.TEMPERATURE,
                                onCheckedChange = { 
                                    radarMapViewModel.toggleLayer(RadarMapViewModel.WeatherLayer.TEMPERATURE) 
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Station info dialog
    if (selectedStation != null) {
        if (displayMode == MapDisplayMode.FULLSCREEN) {
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
        } else {
            Dialog(onDismissRequest = { selectedStation = null }) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Station: ${selectedStation!!.name}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Distance: ${String.format(java.util.Locale.US, "%.1f", selectedStation!!.distance)} km",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        selectedStation?.stationUrl?.let { url ->
                            if (url.isNotEmpty()) {
                                Text("Station URL: $url", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { selectedStation = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

/**
 * The core map content with weather overlays
 */
@Composable
private fun MapContent(
    cameraPositionState: CameraPositionState,
    myLocation: LatLng?,
    selectedStations: List<WeatherStation>,
    showTriangleLayer: Boolean,
    onMarkerClick: (WeatherStation) -> Unit,
    activeLayer: RadarMapViewModel.WeatherLayer,
    precipitationRadarUrl: String?,
    windRadarUrl: String?,
    temperatureRadarUrl: String?,
    onMapLoaded: () -> Unit,
    fullScreen: Boolean
) {
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = myLocation != null,
            mapType = MapType.TERRAIN,
            isBuildingEnabled = false,
            isIndoorEnabled = false,
            isTrafficEnabled = false
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = fullScreen,
            rotationGesturesEnabled = fullScreen,
            scrollGesturesEnabled = fullScreen,
            tiltGesturesEnabled = fullScreen,
            zoomGesturesEnabled = fullScreen
        ),
        onMapLoaded = onMapLoaded
    ) {
        // Temperature overlay (lowest z-index)
        if (activeLayer == RadarMapViewModel.WeatherLayer.TEMPERATURE && temperatureRadarUrl != null) {
            WeatherOverlay(
                imageUrl = temperatureRadarUrl,
                visible = true,
                transparency = 0.4f,
                zIndex = 0f
            )
        }
        
        // Precipitation overlay (middle z-index)
        if (activeLayer == RadarMapViewModel.WeatherLayer.PRECIPITATION && precipitationRadarUrl != null) {
            WeatherOverlay(
                imageUrl = precipitationRadarUrl,
                visible = true,
                transparency = 0.3f,
                zIndex = 1f
            )
        }
        
        // Wind overlay (highest z-index for weather layers)
        if (activeLayer == RadarMapViewModel.WeatherLayer.WIND && windRadarUrl != null) {
            WeatherOverlay(
                imageUrl = windRadarUrl,
                visible = true,
                transparency = 0.4f,
                zIndex = 2f
            )
        }
        
        // Triangular area if we have at least 3 stations
        if (showTriangleLayer && selectedStations.size >= 3) {
            val stationPositions = selectedStations.take(3).map {
                LatLng(it.latitude, it.longitude)
            }
            Polygon(
                points = stationPositions,
                fillColor = Color.Blue.copy(alpha = 0.2f),
                strokeColor = Color.Blue.copy(alpha = 0.5f),
                strokeWidth = 2f,
                zIndex = 3f
            )
        }
        
        // Station markers
        selectedStations.forEach { station ->
            val position = LatLng(station.latitude, station.longitude)
            Marker(
                state = MarkerState(position = position),
                title = station.name,
                snippet = "Distance: ${String.format(java.util.Locale.US, "%.1f", station.distance)} km",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                zIndex = 4f,
                onClick = {
                    onMarkerClick(station)
                    true
                }
            )
        }
        
        // User location marker on top with highest z-index
        myLocation?.let { userLocation ->
            Marker(
                state = MarkerState(userLocation),
                title = "You are here",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                zIndex = 999f // Always on top
            )
            Circle(
                center = userLocation,
                radius = 500.0, // 500 meters
                fillColor = Color.Blue.copy(alpha = 0.15f),
                strokeColor = Color.Blue.copy(alpha = 0.8f),
                strokeWidth = 2f,
                zIndex = 998f // Just under the marker
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

/**
 * A toggle switch for map layers in carousel mode
 */
@Composable
fun LayerToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.5f
            ),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked && enabled,
            onCheckedChange = { if (enabled) onCheckedChange() },
            enabled = enabled
        )
    }
}