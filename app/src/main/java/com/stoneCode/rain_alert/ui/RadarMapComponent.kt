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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberTileOverlayState
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.viewmodel.RadarMapViewModel
import java.net.URL

/**
 * A component that displays a radar map with weather data overlays
 */
@Composable
fun RadarMapComponent(
    modifier: Modifier = Modifier,
    centerLatLng: LatLng = LatLng(40.0, -98.0), // Default to center of US
    selectedStations: List<WeatherStation> = emptyList(),
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    fullScreen: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    radarMapViewModel: RadarMapViewModel = viewModel()
) {
    var showControls by remember { mutableStateOf(true) }
    var showPrecipitationLayer by remember { mutableStateOf(true) }
    var showWindLayer by remember { mutableStateOf(false) }
    var showStationsLayer by remember { mutableStateOf(true) }
    var showTriangleLayer by remember { mutableStateOf(selectedStations.size >= 3) }
    
    // Get radar data from view model
    val precipitationRadarUrl by radarMapViewModel.precipitationRadarUrl.observeAsState()
    val windRadarUrl by radarMapViewModel.windRadarUrl.observeAsState()
    val isRadarLoading by radarMapViewModel.isLoading.observeAsState(false)
    val errorMessage by radarMapViewModel.errorMessage.observeAsState()
    
    // Fetch radar data if needed
    LaunchedEffect(centerLatLng) {
        radarMapViewModel.updateMapCenter(centerLatLng)
        radarMapViewModel.fetchRadarData(centerLatLng)
    }
    
    // Create camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerLatLng, if (fullScreen) 8f else 6f)
    }
    
    // Create radar tile providers if URLs are available
    val precipTileProvider = remember(precipitationRadarUrl) {
        precipitationRadarUrl?.let { url ->
            object : UrlTileProvider(512, 512) {
                override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
                    // Base URL for precipitation radar tiles
                    val tileUrl = url.replace(
                        "BBOX=${centerLatLng.longitude - 2},${centerLatLng.latitude - 2},${centerLatLng.longitude + 2},${centerLatLng.latitude + 2}",
                        "BBOX=${centerLatLng.longitude - 2},${centerLatLng.latitude - 2},${centerLatLng.longitude + 2},${centerLatLng.latitude + 2}"
                    )
                    return URL(tileUrl)
                }
            }
        }
    }
    
    val windTileProvider = remember(windRadarUrl) {
        windRadarUrl?.let { url ->
            object : UrlTileProvider(512, 512) {
                override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
                    // Base URL for wind radar tiles
                    val tileUrl = url.replace(
                        "BBOX=${centerLatLng.longitude - 2},${centerLatLng.latitude - 2},${centerLatLng.longitude + 2},${centerLatLng.latitude + 2}",
                        "BBOX=${centerLatLng.longitude - 2},${centerLatLng.latitude - 2},${centerLatLng.longitude + 2},${centerLatLng.latitude + 2}"
                    )
                    return URL(tileUrl)
                }
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
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = fullScreen
            )
        ) {
            // Add station markers if layer is enabled
            if (showStationsLayer) {
                selectedStations.forEach { station ->
                    val position = LatLng(station.latitude, station.longitude)
                    Marker(
                        state = rememberMarkerState(position = position),
                        title = station.name,
                        snippet = "Distance: ${String.format("%.1f", station.distance)} km",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }
            
            // Add triangular area between stations if enabled and enough stations
            if (showTriangleLayer && selectedStations.size >= 3) {
                val stationPositions = selectedStations.take(3).map { 
                    LatLng(it.latitude, it.longitude) 
                }
                
                Polygon(
                    points = stationPositions,
                    fillColor = Color.Blue.copy(alpha = 0.2f),
                    strokeColor = Color.Blue.copy(alpha = 0.5f),
                    strokeWidth = 2f
                )
            }
            
            // Add precipitation radar overlay if layer is enabled and URL is available
            if (showPrecipitationLayer && precipTileProvider != null) {
                TileOverlay(
                    tileProvider = precipTileProvider,
                    transparency = 0.3f,
                    zIndex = 1f,
                    visible = true,
                    state = rememberTileOverlayState()
                )
            }
            
            // Add wind radar overlay if layer is enabled and URL is available
            if (showWindLayer && windTileProvider != null) {
                TileOverlay(
                    tileProvider = windTileProvider,
                    transparency = 0.4f,
                    zIndex = 2f,
                    visible = true,
                    state = rememberTileOverlayState()
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
        
        // Map controls (conditionally visible)
        if (showControls) {
            MapControls(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
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
                imageVector = if (showControls) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showControls) "Hide Controls" else "Show Controls",
                tint = MaterialTheme.colorScheme.onSurface
            )
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
            // Refresh and fullscreen controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                
                IconButton(
                    onClick = onToggleFullScreen,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = if (isFullScreen) "Exit Full Screen" else "Full Screen Map",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            HorizontalDivider()
            
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
                centerLatLng = LatLng(40.1, -98.0)
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