package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.repository.RadarMapRepository
import com.stoneCode.rain_alert.ui.interop.MapViewCapture
import com.stoneCode.rain_alert.ui.interop.MapViewCaptureHelper
import com.stoneCode.rain_alert.ui.map.RadarOverlayHelper
import com.stoneCode.rain_alert.viewmodel.RadarMapViewModel

/** A component that displays a radar map with weather data overlays.
 *
 * Added modifications:
 * - Accepts an optional myLocation parameter to center the map on your location.
 * - Shows a marker for your current location.
 * - Maintains the camera position by using myLocation (if provided) as the default center.
 * - Long-click (via a simple onClick, since long click isn't directly supported) on station markers
 *   will show a dialog with station information.
 * - Preserves camera position when component recomposes
 * - Properly aligned radar imagery overlays
 */
@Composable
fun RadarMapComponent(
    modifier: Modifier = Modifier,
    centerLatLng: LatLng = LatLng(40.0, -98.0), // Default to center of US
    myLocation: LatLng? = null,
    selectedStations: List<WeatherStation> = emptyList(),
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    fullScreen: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    onMyLocationClick: () -> Unit = {},
    onChangeLocationClick: () -> Unit = {},
    radarMapViewModel: RadarMapViewModel = viewModel()
) {
    // State for showing station info dialog when marker is clicked.
    var selectedStation by remember { mutableStateOf<WeatherStation?>(null) }

    // Layer visibility controls
    var showControls by remember { mutableStateOf(false) }
    var showPrecipitationLayer by remember { mutableStateOf(true) }
    var showWindLayer by remember { mutableStateOf(false) }
    var showTemperatureLayer by remember { mutableStateOf(false) }
    var showStationsLayer by remember { mutableStateOf(true) }
    var showTriangleLayer by remember { mutableStateOf(selectedStations.size >= 3) }

    // Get radar data from view model
    val precipitationRadarUrl by radarMapViewModel.precipitationRadarUrl.observeAsState()
    val windRadarUrl by radarMapViewModel.windRadarUrl.observeAsState()
    val temperatureRadarUrl by radarMapViewModel.temperatureRadarUrl.observeAsState()
    val isRadarLoading by radarMapViewModel.isLoading.observeAsState(false)
    val errorMessage by radarMapViewModel.errorMessage.observeAsState()
    val isTemperatureLayerEnabled by radarMapViewModel.showTemperatureLayer.observeAsState(false)

    // Get repository instance for calculations
    val radarMapRepository = remember { RadarMapRepository(radarMapViewModel.getApplication()) }

    // Use myLocation as default center if available
    val initialCenter = myLocation ?: centerLatLng

    // Key to track if we've initialized the map position (prevents jumps on recomposition)
    val initialized = remember { mutableStateOf(false) }
    
    // IMPORTANT: Remember the camera position state to prevent reset on recomposition
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, if (fullScreen) 12f else 9f)
    }
    
    // Initialize with auto-fit zoom based on selected stations, if available
    LaunchedEffect(selectedStations, myLocation) {
        if (!initialized.value) {
            if (selectedStations.isNotEmpty()) {
                val (center, zoom) = radarMapRepository.calculateMapViewForStations(selectedStations)
                radarMapViewModel.updateMapCenter(center)
                radarMapViewModel.updateMapZoom(zoom)
                // Update camera position
                cameraPositionState.position = CameraPosition.fromLatLngZoom(center, zoom)
            } else if (myLocation != null) {
                radarMapViewModel.updateMapCenter(myLocation)
                // Set a default zoom for your location
                radarMapViewModel.updateMapZoom(12f)
                // Update camera position
                cameraPositionState.position = CameraPosition.fromLatLngZoom(myLocation, 12f)
            }
            initialized.value = true
        }
    }

    // Get map data from view model
    val mapCenter by radarMapViewModel.mapCenter.observeAsState(initialCenter)
    val mapZoom by radarMapViewModel.mapZoom.observeAsState(if (fullScreen) 12f else 9f)
    
    // Update the view model with the latest camera position when it changes
    // This ensures we remember where the user panned/zoomed
    LaunchedEffect(cameraPositionState.position) {
        if (initialized.value && !cameraPositionState.isMoving) {
            radarMapViewModel.updateMapCenter(cameraPositionState.position.target)
            radarMapViewModel.updateMapZoom(cameraPositionState.position.zoom)
        }
    }

    // Animate camera position if the map center or zoom is explicitly changed
    // (e.g., when clicking on location button)
    LaunchedEffect(mapCenter, mapZoom) {
        if (initialized.value) {
            val currentTarget = cameraPositionState.position.target
            val currentZoom = cameraPositionState.position.zoom
            
            // Only animate if there's an actual change to avoid loops
            if (currentTarget != mapCenter || currentZoom != mapZoom) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition(mapCenter, mapZoom, 0f, 0f)
                    ),
                    durationMs = 1000
                )
            }
        }
    }

    // Fetch radar data if needed
    LaunchedEffect(mapCenter) {
        radarMapViewModel.fetchRadarData(mapCenter)
    }

    // Get context for radar overlay helper
    val context = LocalContext.current
    
    // We'll use this flag to know when the GoogleMap is ready for overlays
    var mapReady by remember { mutableStateOf(false) }
    
    // We'll use this to store the GoogleMap instance for overlay management
    var googleMapInstance by remember { mutableStateOf<GoogleMap?>(null) }
    
    // Clean up resources when component is dismounted
    var mapViewRef by remember { mutableStateOf<com.google.android.gms.maps.MapView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            // Clean up map overlays
            RadarOverlayHelper.removeAllOverlays()
            
            // Clean up MapView to prevent memory leaks
            mapViewRef?.let { mapView ->
                mapView.onPause()
                mapView.onDestroy()
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (fullScreen) 500.dp else 200.dp)
            .clip(shape = RoundedCornerShape(12.dp))
    ) {
        // Google Map with radar overlay
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                // Enable showing the blue dot for user's location
                isMyLocationEnabled = myLocation != null,
                // Use terrain type to see mountains and terrain details
                mapType = com.google.maps.android.compose.MapType.TERRAIN
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false, // We'll use our own button
                compassEnabled = fullScreen,
                // Enable all gestures for better interaction
                rotationGesturesEnabled = true,
                scrollGesturesEnabled = true,
                tiltGesturesEnabled = true,
                zoomGesturesEnabled = true
            ),
            onMapLoaded = {
                android.util.Log.d("RadarMapComponent", "Map loaded and ready for overlays")
                mapReady = true
            }
        ) {
            // Add triangular area between stations if enabled and enough stations
            // This goes between the overlays and markers
            if (showTriangleLayer && selectedStations.size >= 3) {
                val stationPositions = selectedStations.take(3).map {
                    LatLng(it.latitude, it.longitude)
                }
                Polygon(
                    points = stationPositions,
                    fillColor = Color.Blue.copy(alpha = 0.2f),
                    strokeColor = Color.Blue.copy(alpha = 0.5f),
                    strokeWidth = 2f,
                    zIndex = 1f // Above radar overlays but below markers
                )
            }
            
            // Add station markers if layer is enabled
            if (showStationsLayer) {
                selectedStations.forEach { station ->
                    val position = LatLng(station.latitude, station.longitude)
                    Marker(
                        state = MarkerState(position = position),
                        title = station.name,
                        snippet = "Distance: ${String.format("%.1f", station.distance)} km",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        zIndex = 2.0f, // Above overlays and triangles
                        onClick = {
                            // Simulate a long click by showing a dialog with station info
                            selectedStation = station
                            true
                        }
                    )
                }
            }
            
            // Always show user location marker on top of everything if available
            if (myLocation != null) {
                Marker(
                    state = MarkerState(position = myLocation),
                    title = "You are here",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    zIndex = 3.0f  // Highest z-index to always be on top
                )
            }
        }
        
        // Get the GoogleMap instance for overlays using MapViewCapture
        MapViewCapture(
            modifier = Modifier.size(1.dp),
            onMapReady = { googleMap ->
                googleMapInstance = googleMap
                radarMapViewModel.setGoogleMapInstance(googleMap)
                android.util.Log.d("RadarMapComponent", "GoogleMap instance obtained via MapViewCapture")
                
                // Apply overlays immediately if URLs are available
                precipitationRadarUrl?.let { url ->
                    RadarOverlayHelper.showPrecipitationOverlay(googleMap, context, url, showPrecipitationLayer)
                }
                
                windRadarUrl?.let { url ->
                    RadarOverlayHelper.showWindOverlay(googleMap, context, url, showWindLayer)
                }
                
                temperatureRadarUrl?.let { url ->
                    RadarOverlayHelper.showTemperatureOverlay(googleMap, context, url, isTemperatureLayerEnabled)
                }
            },
            onViewCreated = { mapView ->
                mapViewRef = mapView
            }
        )
        
        // Update overlays when layer visibility or URLs change
        LaunchedEffect(googleMapInstance, showPrecipitationLayer, precipitationRadarUrl) {
            googleMapInstance?.let { map ->
                RadarOverlayHelper.showPrecipitationOverlay(
                    map = map,
                    context = context,
                    url = precipitationRadarUrl,
                    showLayer = showPrecipitationLayer
                )
            }
        }
        
        LaunchedEffect(googleMapInstance, showWindLayer, windRadarUrl) {
            googleMapInstance?.let { map ->
                RadarOverlayHelper.showWindOverlay(
                    map = map,
                    context = context,
                    url = windRadarUrl,
                    showLayer = showWindLayer
                )
            }
        }
        
        LaunchedEffect(googleMapInstance, isTemperatureLayerEnabled, temperatureRadarUrl) {
            googleMapInstance?.let { map ->
                RadarOverlayHelper.showTemperatureOverlay(
                    map = map,
                    context = context,
                    url = temperatureRadarUrl,
                    showLayer = isTemperatureLayerEnabled
                )
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Main map controls for full screen, location, and my location
        // Full screen button (top left)
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
                imageVector = if (fullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (fullScreen) "Exit Full Screen" else "Full Screen",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Location buttons (top right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // My location button
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

            // Manual location button
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

        // Layer controls (conditionally visible)
        if (showControls) {
            MapControls(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 56.dp),
                isFullScreen = fullScreen,
                onToggleFullScreen = onToggleFullScreen,
                onRefresh = onRefresh,
                showPrecipitation = showPrecipitationLayer,
                onTogglePrecipitation = { showPrecipitationLayer = !showPrecipitationLayer },
                showWind = showWindLayer,
                onToggleWind = { showWindLayer = !showWindLayer },
                showTemperature = isTemperatureLayerEnabled,
                onToggleTemperature = { radarMapViewModel.toggleTemperatureLayer() },
                showStations = showStationsLayer,
                onToggleStations = { showStationsLayer = !showStationsLayer },
                showTriangle = showTriangleLayer,
                onToggleTriangle = { showTriangleLayer = !showTriangleLayer },
                triangleEnabled = selectedStations.size >= 3
            )
        }

        // Toggle for controls visibility
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
    }

    // Dialog for displaying station info when marker is clicked
    if (selectedStation != null) {
        Dialog(onDismissRequest = { selectedStation = null }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth(if (fullScreen) 0.7f else 0.9f) // Adjust width based on screen mode
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Station: ${selectedStation!!.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Distance: ${String.format("%.1f", selectedStation!!.distance)} km",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Additional info about the station (if available)
                    selectedStation!!.stationUrl?.let { url ->
                        if (url.isNotEmpty()) {
                            Text(
                                text = "Station URL: $url",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Close button
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

@Composable
fun MapControls(
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    onRefresh: () -> Unit = {},
    showPrecipitation: Boolean = true,
    onTogglePrecipitation: () -> Unit = {},
    showWind: Boolean = false,
    onToggleWind: () -> Unit = {},
    showTemperature: Boolean = false,
    onToggleTemperature: () -> Unit = {},
    showStations: Boolean = true,
    onToggleStations: () -> Unit = {},
    showTriangle: Boolean = false,
    onToggleTriangle: () -> Unit = {},
    triangleEnabled: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Refresh control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Map",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Layer toggles
            Text(
                text = "Layers",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Precipitation layer toggle
            LayerToggle(
                label = "Precipitation",
                checked = showPrecipitation,
                onCheckedChange = onTogglePrecipitation
            )

            // Wind layer toggle
            LayerToggle(
                label = "Wind",
                checked = showWind,
                onCheckedChange = onToggleWind
            )
            
            // Temperature layer toggle
            LayerToggle(
                label = "Temperature",
                checked = showTemperature,
                onCheckedChange = onToggleTemperature
            )

            // Stations layer toggle
            LayerToggle(
                label = "Stations",
                checked = showStations,
                onCheckedChange = onToggleStations
            )

            // Triangle overlay toggle
            LayerToggle(
                label = "Coverage Area",
                checked = showTriangle,
                onCheckedChange = onToggleTriangle,
                enabled = triangleEnabled
            )
        }
    }
}

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
            color = if (enabled)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked && enabled,
            onCheckedChange = { if (enabled) onCheckedChange() },
            enabled = enabled
        )
    }
}


// No longer needed since we're using RadarOverlayManager directly

// Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
fun PreviewRadarMapComponent() {
    MaterialTheme {
        val previewStations = listOf(
            WeatherStation(
                id = "station1",
                name = "Station 1",
                latitude = 40.0,
                longitude = -98.0,
                distance = 10.0,
                stationUrl = ""
            ),
            WeatherStation(
                id = "station2",
                name = "Station 2",
                latitude = 40.1,
                longitude = -98.1,
                distance = 15.0,
                stationUrl = ""
            ),
            WeatherStation(
                id = "station3",
                name = "Station 3",
                latitude = 40.2,
                longitude = -97.9,
                distance = 12.0,
                stationUrl = ""
            )
        )

        Column {
            RadarMapComponent(
                selectedStations = previewStations,
                myLocation = LatLng(40.1, -98.0)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Loading State:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            RadarMapComponent(
                isLoading = true
            )
        }
    }
}
