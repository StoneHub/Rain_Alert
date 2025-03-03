package com.stoneCode.rain_alert.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.repository.RadarMapRepository
import com.stoneCode.rain_alert.ui.map.MapOverlayManager
import com.stoneCode.rain_alert.viewmodel.RadarMapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "RadarMapComponent"

/** A component that displays a radar map with weather data overlays.
 *
 * This implementation uses AndroidView to host a native MapView, giving direct access
 * to the GoogleMap instance for overlay manipulation.
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
    var showStationsLayer by remember { mutableStateOf(true) }
    var showTriangleLayer by remember { mutableStateOf(selectedStations.size >= 3) }

    // Get radar data from view model
    val precipitationRadarUrl by radarMapViewModel.precipitationRadarUrl.observeAsState()
    val windRadarUrl by radarMapViewModel.windRadarUrl.observeAsState()
    val isRadarLoading by radarMapViewModel.isLoading.observeAsState(false)
    val errorMessage by radarMapViewModel.errorMessage.observeAsState()

    // Get repository instance for calculations
    val radarMapRepository = remember { RadarMapRepository(radarMapViewModel.getApplication()) }

    // Use myLocation as default center if available
    val initialCenter = myLocation ?: centerLatLng

    // Get map data from view model
    val mapCenter by radarMapViewModel.mapCenter.observeAsState(initialCenter)
    val mapZoom by radarMapViewModel.mapZoom.observeAsState(if (fullScreen) 12f else 9f)
    
    // Key to track if we've initialized the map position (prevents jumps on recomposition)
    val initialized = remember { mutableStateOf(false) }
    
    // References to store map and overlays
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var precipitationOverlay by remember { mutableStateOf<GroundOverlay?>(null) }
    var windOverlay by remember { mutableStateOf<GroundOverlay?>(null) }
    
    // Context for adding overlays
    val context = LocalContext.current

    // Initialize MapView with lifecycle awareness
    val mapView = rememberMapViewWithLifecycle()
    
    // Box that contains the map and UI controls
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (fullScreen) 500.dp else 200.dp)
            .clip(shape = RoundedCornerShape(12.dp))
    ) {
        // MapView wrapped in AndroidView
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        ) { mapView ->
            // Get map asynchronously
            mapView.getMapAsync { map ->
                googleMap = map
                
                // Configure map settings
                map.uiSettings.apply {
                    isZoomControlsEnabled = false
                    isMapToolbarEnabled = false
                    isMyLocationButtonEnabled = false
                    isCompassEnabled = fullScreen
                }
                
                // Set my location enabled if available
                map.isMyLocationEnabled = myLocation != null
                
                // Initial camera position
                if (!initialized.value) {
                    // Set initial position based on selected stations or my location
                    if (selectedStations.isNotEmpty()) {
                        val (center, zoom) = radarMapRepository.calculateMapViewForStations(selectedStations)
                        radarMapViewModel.updateMapCenter(center)
                        radarMapViewModel.updateMapZoom(zoom)
                        
                        map.moveCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(center, zoom)
                            )
                        )
                    } else if (myLocation != null) {
                        radarMapViewModel.updateMapCenter(myLocation)
                        radarMapViewModel.updateMapZoom(12f)
                        
                        map.moveCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(myLocation, 12f)
                            )
                        )
                    } else {
                        map.moveCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(mapCenter, mapZoom)
                            )
                        )
                    }
                    
                    initialized.value = true
                    
                    // Fetch radar data for initial position
                    radarMapViewModel.fetchRadarData(map.cameraPosition.target)
                }
                
                // Set up camera change listener to update ViewModel
                map.setOnCameraIdleListener {
                    if (initialized.value) {
                        val position = map.cameraPosition
                        radarMapViewModel.updateMapCenter(position.target)
                        radarMapViewModel.updateMapZoom(position.zoom)
                    }
                }
                
                // Set up marker click listener
                map.setOnMarkerClickListener { marker ->
                    val stationId = marker.tag as? String
                    if (stationId != null) {
                        selectedStation = selectedStations.find { it.id == stationId }
                        true
                    } else {
                        false
                    }
                }
                
                // Update map with overlays and markers
                updateMapContent(
                    map = map,
                    context = context,
                    selectedStations = selectedStations,
                    myLocation = myLocation,
                    showStationsLayer = showStationsLayer,
                    showTriangleLayer = showTriangleLayer,
                    precipitationRadarUrl = if (showPrecipitationLayer) precipitationRadarUrl else null,
                    windRadarUrl = if (showWindLayer) windRadarUrl else null,
                    currentPrecipitationOverlay = precipitationOverlay,
                    currentWindOverlay = windOverlay,
                    onPrecipitationOverlayCreated = { precipitationOverlay = it },
                    onWindOverlayCreated = { windOverlay = it }
                )
            }
        }
        
        // Effect to update map when layer visibility or radar URLs change
        LaunchedEffect(
            showPrecipitationLayer, showWindLayer, showStationsLayer, showTriangleLayer,
            precipitationRadarUrl, windRadarUrl, selectedStations, myLocation
        ) {
            googleMap?.let { map ->
                updateMapContent(
                    map = map,
                    context = context,
                    selectedStations = selectedStations,
                    myLocation = myLocation,
                    showStationsLayer = showStationsLayer,
                    showTriangleLayer = showTriangleLayer,
                    precipitationRadarUrl = if (showPrecipitationLayer) precipitationRadarUrl else null,
                    windRadarUrl = if (showWindLayer) windRadarUrl else null,
                    currentPrecipitationOverlay = precipitationOverlay,
                    currentWindOverlay = windOverlay,
                    onPrecipitationOverlayCreated = { precipitationOverlay = it },
                    onWindOverlayCreated = { windOverlay = it }
                )
            }
        }
        
        // Effect to update camera position when mapCenter or mapZoom change in ViewModel
        LaunchedEffect(mapCenter, mapZoom) {
            googleMap?.let { map ->
                if (initialized.value) {
                    val currentTarget = map.cameraPosition.target
                    val currentZoom = map.cameraPosition.zoom
                    
                    // Only animate if there's an actual change to avoid loops
                    if (currentTarget != mapCenter || currentZoom != mapZoom) {
                        map.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition(mapCenter, mapZoom, 0f, 0f)
                            ),
                            1000,
                            null
                        )
                    }
                }
            }
        }
        
        // Clean up overlays when component is disposed
        DisposableEffect(Unit) {
            onDispose {
                MapOverlayManager.removeOverlay(precipitationOverlay)
                MapOverlayManager.removeOverlay(windOverlay)
                precipitationOverlay = null
                windOverlay = null
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

/**
 * Updates the map content with overlays, markers, and other visual elements
 */
private fun updateMapContent(
    map: GoogleMap,
    context: Context,
    selectedStations: List<WeatherStation>,
    myLocation: LatLng?,
    showStationsLayer: Boolean,
    showTriangleLayer: Boolean,
    precipitationRadarUrl: String?,
    windRadarUrl: String?,
    currentPrecipitationOverlay: GroundOverlay?,
    currentWindOverlay: GroundOverlay?,
    onPrecipitationOverlayCreated: (GroundOverlay?) -> Unit,
    onWindOverlayCreated: (GroundOverlay?) -> Unit
) {
    // Clear map of all markers
    map.clear()
    
    // Add triangular area between stations if enabled and enough stations
    if (showTriangleLayer && selectedStations.size >= 3) {
        val stationPositions = selectedStations.take(3).map {
            LatLng(it.latitude, it.longitude)
        }
        
        map.addPolygon(
            PolygonOptions()
                .addAll(stationPositions)
                .fillColor(Color.Blue.copy(alpha = 0.2f).toArgb())
                .strokeColor(Color.Blue.copy(alpha = 0.5f).toArgb())
                .strokeWidth(2f)
                .zIndex(1f) // Above radar overlays but below markers
        )
    }
    
    // Add station markers if layer is enabled
    if (showStationsLayer) {
        selectedStations.forEach { station ->
            val position = LatLng(station.latitude, station.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(station.name)
                    .snippet("Distance: ${String.format("%.1f", station.distance)} km")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .zIndex(2.0f) // Above overlays and triangles
            )
            
            // Store station ID as marker tag for identification in click handler
            marker?.tag = station.id
        }
    }
    
    // Always show user location marker on top of everything if available
    if (myLocation != null) {
        map.addMarker(
            MarkerOptions()
                .position(myLocation)
                .title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .zIndex(3.0f)  // Highest z-index to always be on top
        )
    }
    
    // Handle precipitation overlay
    if (precipitationRadarUrl != null) {
        // Remove existing overlay
        MapOverlayManager.removeOverlay(currentPrecipitationOverlay)
        
        // Add new overlay
        MapOverlayManager.addGroundOverlayFromUrl(
            googleMap = map,
            context = context,
            imageUrl = precipitationRadarUrl,
            alpha = 0.7f,
            zIndex = 0f,
            callback = { overlay ->
                onPrecipitationOverlayCreated(overlay)
            }
        )
    } else {
        // Remove overlay if the layer is disabled
        MapOverlayManager.removeOverlay(currentPrecipitationOverlay)
        onPrecipitationOverlayCreated(null)
    }
    
    // Handle wind overlay
    if (windRadarUrl != null) {
        // Remove existing overlay
        MapOverlayManager.removeOverlay(currentWindOverlay)
        
        // Add new overlay
        MapOverlayManager.addGroundOverlayFromUrl(
            googleMap = map,
            context = context,
            imageUrl = windRadarUrl,
            alpha = 0.5f,
            zIndex = 0.5f, // Just above precipitation overlay
            callback = { overlay ->
                onWindOverlayCreated(overlay)
            }
        )
    } else {
        // Remove overlay if the layer is disabled
        MapOverlayManager.removeOverlay(currentWindOverlay)
        onWindOverlayCreated(null)
    }
}

/**
 * Remembers a MapView with lifecycle handling
 */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    
    DisposableEffect(lifecycle, mapView) {
        // Create lifecycle observer
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        
        // Add the observer to the lifecycle
        lifecycle.addObserver(lifecycleObserver)
        
        // Remove observer when effect leaves composition
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            mapView.onDestroy()
        }
    }
    
    return mapView
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
