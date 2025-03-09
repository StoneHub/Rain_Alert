package com.stoneCode.rain_alert.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.repository.RadarMapRepository
import com.stoneCode.rain_alert.ui.map.WeatherOverlay
import com.stoneCode.rain_alert.viewmodel.RadarMapViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RadarMapComponent(
    modifier: Modifier = Modifier,
    centerLatLng: LatLng = LatLng(40.0, -98.0),
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
    val coroutineScope = rememberCoroutineScope()

    var selectedStation by remember { mutableStateOf<WeatherStation?>(null) }

    // Refs to user toggles
    var showControls by remember { mutableStateOf(false) }
    var showPrecipitationLayer by remember { mutableStateOf(true) }
    var showWindLayer by remember { mutableStateOf(false) }
    var showStationsLayer by remember { mutableStateOf(true) }
    var showTriangleLayer by remember { mutableStateOf(selectedStations.size >= 3) }

    // Weather data state

    val precipitationRadarUrl by radarMapViewModel.precipitationRadarUrl.observeAsState()
    val windRadarUrl by radarMapViewModel.windRadarUrl.observeAsState()
    val temperatureRadarUrl by radarMapViewModel.temperatureRadarUrl.observeAsState()
    val isRadarLoading by radarMapViewModel.isLoading.observeAsState(false)
    val isTemperatureLayerEnabled by radarMapViewModel.showTemperatureLayer.observeAsState(false)

    val radarMapRepository = remember { RadarMapRepository(radarMapViewModel.getApplication()) }

    // Map camera state
    val initialized = remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(myLocation ?: centerLatLng, if (fullScreen) 12f else 9f)
    }

    // Center on user location only once initially, if available
    LaunchedEffect(myLocation) {
        if (!initialized.value && myLocation != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(myLocation, 12f)
            radarMapViewModel.updateMapCenter(myLocation)
            radarMapViewModel.updateMapZoom(12f)
            initialized.value = true
        }
    }

    // If we still haven't initialized and we have stations
    LaunchedEffect(selectedStations, myLocation) {
        if (!initialized.value) {
            if (selectedStations.isNotEmpty()) {
                val (center, zoom) = radarMapRepository.calculateMapViewForStations(selectedStations)
                radarMapViewModel.updateMapCenter(center)
                radarMapViewModel.updateMapZoom(zoom)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(center, zoom)
            } else if (myLocation != null) {
                radarMapViewModel.updateMapCenter(myLocation)
                radarMapViewModel.updateMapZoom(12f)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(myLocation, 12f)
            } else {
                // Use fallback centerLatLng if nothing else
                radarMapViewModel.updateMapCenter(centerLatLng)
                radarMapViewModel.updateMapZoom(if (fullScreen) 12f else 9f)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    centerLatLng,
                    if (fullScreen) 12f else 9f
                )
            }
            initialized.value = true
        }
    }

    // Listen for changes to the camera; update the ViewModel only if user has finished moving
    // so it doesn't keep forcing new center on every tiny drag
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && initialized.value) {
            val currentPos = cameraPositionState.position
            radarMapViewModel.updateMapCenter(currentPos.target)
            radarMapViewModel.updateMapZoom(currentPos.zoom)
        }
    }

    // One-time fetch when we have a valid center
    val mapCenter by radarMapViewModel.mapCenter.observeAsState(centerLatLng)
    LaunchedEffect(mapCenter) {
        if (initialized.value) {
            // Pull new radar data for that center
            radarMapViewModel.fetchRadarData(mapCenter)
        }
    }

    // Map composable
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (fullScreen) 500.dp else 200.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = (myLocation != null),
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
                rotationGesturesEnabled = true,
                scrollGesturesEnabled = true,
                tiltGesturesEnabled = true,
                zoomGesturesEnabled = true
            ),
            onMapLoaded = {
                Log.d("RadarMapComponent", "Map loaded and ready")
            }
        ) {
            // Display weather layers
            // Temperature layer (lowest z-index so it appears below other layers)
            if (isTemperatureLayerEnabled && temperatureRadarUrl != null) {
                WeatherOverlay(
                    imageUrl = temperatureRadarUrl,
                    visible = true,
                    transparency = 0.4f,
                    zIndex = 0f
                )
            }
            
            // Precipitation layer (middle z-index)
            if (showPrecipitationLayer && precipitationRadarUrl != null) {
                WeatherOverlay(
                    imageUrl = precipitationRadarUrl,
                    visible = true,
                    transparency = 0.3f,
                    zIndex = 1f
                )
            }
            
            // Wind layer (highest z-index for weather overlays)
            if (showWindLayer && windRadarUrl != null) {
                WeatherOverlay(
                    imageUrl = windRadarUrl,
                    visible = true,
                    transparency = 0.4f,
                    zIndex = 2f
                )
            }

            // Triangular area
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
            if (showStationsLayer) {
                selectedStations.forEach { station ->
                    val position = LatLng(station.latitude, station.longitude)
                    Marker(
                        state = MarkerState(position = position),
                        title = station.name,
                        snippet = "Distance: ${String.format(java.util.Locale.US, "%.1f", station.distance)} km",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        zIndex = 4f,  // above polygons
                        onClick = {
                            selectedStation = station
                            true
                        }
                    )
                }
            }

            // Finally, show myLocation marker on top with a large z-index
            myLocation?.let { userLocation ->
                Marker(
                    state = MarkerState(userLocation),
                    title = "You are here",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    zIndex = 999f // always on top
                )
                Circle(
                    center = userLocation,
                    radius = 500.0,
                    fillColor = Color.Blue.copy(alpha = 0.15f),
                    strokeColor = Color.Blue.copy(alpha = 0.9f),
                    strokeWidth = 2f,
                    zIndex = 998f // just under the marker
                )
            }
        }

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
                imageVector = if (fullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (fullScreen) "Exit Full Screen" else "Full Screen",
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
                        radarMapViewModel.updateMapCenter(loc)
                        radarMapViewModel.updateMapZoom(12f)
                        // Use the coroutineScope defined above to animate the camera
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(CameraPosition(loc, 12f, 0f, 0f)),
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
            MapControls(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 56.dp),
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

        // No forecast scrubber
    }

    // Dialog for station info
    if (selectedStation != null) {
        Dialog(onDismissRequest = { selectedStation = null }) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth(if (fullScreen) 0.7f else 0.9f)
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

/**
 * A small card with toggles for map layers, plus refresh.
 */
@Composable
fun MapControls(
    modifier: Modifier = Modifier,
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
            Text("Layers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            LayerToggle("Precipitation", showPrecipitation, onTogglePrecipitation)
            LayerToggle("Wind", showWind, onToggleWind)
            LayerToggle("Temperature", showTemperature, onToggleTemperature)
            LayerToggle("Stations", showStations, onToggleStations)
            LayerToggle("Coverage Area", showTriangle, onToggleTriangle, enabled = triangleEnabled)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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

