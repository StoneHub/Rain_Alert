package com.stoneCode.rain_alert.ui.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.rememberCameraPositionState
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.viewmodel.RadarMapViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Helper function to calculate map bounds from a center point and zoom level
 * This is needed because the map's projection might not be available immediately
 */
private fun calculateBoundsFromCenterAndZoom(center: LatLng, zoom: Float): LatLngBounds? {
    try {
        // Earth radius in meters
        val earthRadius = 6371000.0
        val latRadians = Math.toRadians(center.latitude)
        
        // Calculate meters per pixel at given latitude and zoom level
        val metersPerPx = earthRadius * Math.cos(latRadians) * 2 * Math.PI / Math.pow(2.0, zoom.toDouble() + 8)
        
        // Approximate width and height of the viewport in meters
        val widthMeters = 1000.0 * metersPerPx  // Assuming 1000px width
        val heightMeters = 600.0 * metersPerPx   // Assuming 600px height
        
        // Calculate lat/lng span based on distance
        val latSpan = heightMeters / earthRadius * (180.0 / Math.PI)
        val lngSpan = widthMeters / (earthRadius * Math.cos(latRadians)) * (180.0 / Math.PI)
        
        // Create bounds
        return LatLngBounds.builder()
            .include(LatLng(center.latitude - latSpan/2, center.longitude - lngSpan/2))
            .include(LatLng(center.latitude + latSpan/2, center.longitude + lngSpan/2))
            .build()
    } catch (e: Exception) {
        Log.e("SharedMapComponent", "Error calculating bounds", e)
        return null
    }
}

/**
 * A shared map component that can be used in both carousel and fullscreen modes.
 * Centralizes the common functionality between different map views.
 * 
 * Used in both the vertical layout of MainScreen (replacing WeatherCarousel) and
 * in the fullscreen map view.
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
    
    // Log current state for debugging
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
    
    // Initialize map and fetch radar data - IMPORTANT for carousel mode
    LaunchedEffect(key1 = displayMode, key2 = myLocation) {
        val locationToUse = myLocation ?: lastLocation ?: centerLatLng
        val zoom = if (displayMode == MapDisplayMode.FULLSCREEN) 12f else 9f
        
        // Update camera position
        cameraPositionState.position = CameraPosition.fromLatLngZoom(locationToUse, zoom)
        
        // Update viewModel with current location and zoom
        radarMapViewModel.updateMapCenter(locationToUse)
        radarMapViewModel.updateMapZoom(zoom)
        
        // Save last known location
        if (myLocation != null) {
            radarMapViewModel.updateLastKnownLocation(myLocation)
        }
        
        // Calculate bounds for radar data
        val bounds = calculateBoundsFromCenterAndZoom(locationToUse, zoom)
        if (bounds != null) {
            // Update camera with calculated bounds
            radarMapViewModel.updateMapCamera(
                locationToUse,
                zoom,
                bounds
            )
            
            // Force fetch radar data immediately - important for carousel mode
            Log.d("SharedMapComponent", "Forcing radar data fetch for $displayMode mode")
            radarMapViewModel.fetchRadarData(locationToUse)
            
            // Small delay to ensure fetching completes
            delay(500)
        }
        
        initialized.value = true
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
                if (activeLayer != RadarMapViewModel.WeatherLayer.NONE) {
                    radarMapViewModel.fetchRadarData(currentPos.target)
                }
            }
        }
    }
    
    // Additional effect to ensure radar data is loaded after initialization
    // This is critical for carousel mode to show radar data without requiring user interaction
    LaunchedEffect(initialized.value, activeLayer) {
        if (initialized.value) {
            // Add a small delay to ensure map is fully loaded
            delay(1000)
            
            // Force fetch radar data again after initialization
            val locationToUse = myLocation ?: lastLocation ?: centerLatLng
            Log.d("SharedMapComponent", "Ensuring radar data is loaded for $activeLayer after initialization")
            radarMapViewModel.fetchRadarData(locationToUse)
        }
    }
    
    // UI based on display mode
    when (displayMode) {
        MapDisplayMode.FULLSCREEN -> FullscreenMapView(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            myLocation = myLocation,
            selectedStations = selectedStations,
            showTriangleLayer = showTriangleLayer,
            isLoading = isLoading || isRadarLoading,
            activeLayer = activeLayer,
            precipitationRadarUrl = precipitationRadarUrl,
            windRadarUrl = windRadarUrl,
            temperatureRadarUrl = temperatureRadarUrl,
            onMarkerClick = { selectedStation = it },
            onMyLocationClick = {
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
            onRefresh = onRefresh,
            onBackClick = onBackClick,
            onToggleLayer = { layer -> radarMapViewModel.toggleLayer(layer) },
            initialized = initialized
        )
        
        MapDisplayMode.CAROUSEL -> CarouselMapView(
            modifier = modifier,
            cameraPositionState = cameraPositionState,
            myLocation = myLocation,
            selectedStations = selectedStations,
            showTriangleLayer = showTriangleLayer,
            isLoading = isLoading || isRadarLoading,
            activeLayer = activeLayer,
            precipitationRadarUrl = precipitationRadarUrl,
            windRadarUrl = windRadarUrl,
            temperatureRadarUrl = temperatureRadarUrl,
            onMarkerClick = { selectedStation = it },
            onMyLocationClick = {
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
            onRefresh = {
                onRefresh()
                // Force radar data refresh when requested explicitly in carousel mode
                radarMapViewModel.refreshRadarData()
            },
            onToggleFullScreen = onToggleFullScreen,
            onChangeLocationClick = onChangeLocationClick,
            onToggleLayer = { layer -> 
                radarMapViewModel.toggleLayer(layer)
                // Force fetch data when layer is changed in carousel mode
                // Add a small delay to ensure the layer change is processed first
                coroutineScope.launch {
                    delay(300) // Brief delay to ensure layer toggle is processed
                    val currentPos = cameraPositionState.position
                    Log.d("SharedMapComponent", "Refreshing radar data after changing to layer: $layer")
                    radarMapViewModel.fetchRadarData(currentPos.target)
                }
            },
            showControls = showControls,
            onToggleControls = { showControls = !showControls }
        )
    }
    
    // Station info dialog
    selectedStation?.let { station ->
        StationInfoDialog(
            station = station,
            onDismiss = { selectedStation = null },
            isFullscreen = displayMode == MapDisplayMode.FULLSCREEN
        )
        
        // Clean up when the component is disposed
        DisposableEffect(Unit) {
            onDispose {
                selectedStation = null
            }
        }
    }
}

/**
 * Full screen map view with top app bar and controls
 */
@Composable
private fun FullscreenMapView(
    modifier: Modifier,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    myLocation: LatLng?,
    selectedStations: List<WeatherStation>,
    showTriangleLayer: Boolean,
    isLoading: Boolean,
    activeLayer: RadarMapViewModel.WeatherLayer,
    precipitationRadarUrl: String?,
    windRadarUrl: String?,
    temperatureRadarUrl: String?,
    onMarkerClick: (WeatherStation) -> Unit,
    onMyLocationClick: () -> Unit,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onToggleLayer: (RadarMapViewModel.WeatherLayer) -> Unit,
    initialized: androidx.compose.runtime.MutableState<Boolean>
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top app bar
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
            onPrecipitationToggle = { onToggleLayer(RadarMapViewModel.WeatherLayer.PRECIPITATION) },
            onWindLayerToggle = { onToggleLayer(RadarMapViewModel.WeatherLayer.WIND) },
            onTemperatureToggle = { onToggleLayer(RadarMapViewModel.WeatherLayer.TEMPERATURE) },
            showPrecipitationLayer = activeLayer == RadarMapViewModel.WeatherLayer.PRECIPITATION,
            showWindLayer = activeLayer == RadarMapViewModel.WeatherLayer.WIND,
            showTemperatureLayer = activeLayer == RadarMapViewModel.WeatherLayer.TEMPERATURE,
            modifier = Modifier.padding(8.dp)
        )
        
        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        
        // Map container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Map content
            MapContent(
                cameraPositionState = cameraPositionState,
                myLocation = myLocation,
                selectedStations = selectedStations,
                showTriangleLayer = showTriangleLayer,
                onMarkerClick = onMarkerClick,
                activeLayer = activeLayer,
                precipitationRadarUrl = precipitationRadarUrl,
                windRadarUrl = windRadarUrl,
                temperatureRadarUrl = temperatureRadarUrl,
                onMapLoaded = {
                    Log.d("SharedMapComponent", "Map loaded in fullscreen mode")
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
                        onClick = onMyLocationClick,
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

/**
 * Carousel map view with compact controls
 */
@Composable
private fun CarouselMapView(
    modifier: Modifier,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    myLocation: LatLng?,
    selectedStations: List<WeatherStation>,
    showTriangleLayer: Boolean,
    isLoading: Boolean,
    activeLayer: RadarMapViewModel.WeatherLayer,
    precipitationRadarUrl: String?,
    windRadarUrl: String?,
    temperatureRadarUrl: String?,
    onMarkerClick: (WeatherStation) -> Unit,
    onMyLocationClick: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onChangeLocationClick: () -> Unit,
    onToggleLayer: (RadarMapViewModel.WeatherLayer) -> Unit,
    showControls: Boolean,
    onToggleControls: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Map content
        MapContent(
            cameraPositionState = cameraPositionState,
            myLocation = myLocation,
            selectedStations = selectedStations,
            showTriangleLayer = showTriangleLayer,
            onMarkerClick = onMarkerClick,
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
        if (isLoading) {
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
                onClick = onMyLocationClick,
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
            onClick = onToggleControls,
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
        
        // Layer controls panel
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
                    
                    // Layer toggles
                    LayerToggle(
                        label = "Precipitation",
                        checked = activeLayer == RadarMapViewModel.WeatherLayer.PRECIPITATION,
                        onCheckedChange = { onToggleLayer(RadarMapViewModel.WeatherLayer.PRECIPITATION) }
                    )
                    
                    LayerToggle(
                        label = "Wind",
                        checked = activeLayer == RadarMapViewModel.WeatherLayer.WIND,
                        onCheckedChange = { onToggleLayer(RadarMapViewModel.WeatherLayer.WIND) }
                    )
                    
                    LayerToggle(
                        label = "Temperature",
                        checked = activeLayer == RadarMapViewModel.WeatherLayer.TEMPERATURE,
                        onCheckedChange = { onToggleLayer(RadarMapViewModel.WeatherLayer.TEMPERATURE) }
                    )
                }
            }
        }
    }
}