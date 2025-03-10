package com.stoneCode.rain_alert.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.viewmodel.RadarMapViewModel
import java.util.Locale

/**
 * The core map content with weather overlays
 */
@Composable
fun MapContent(
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
    fullScreen: Boolean,
    showDiagnostics: Boolean = false
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
                transparency = 0.3f,
                zIndex = 0f,
                showDiagnostics = showDiagnostics
            )
        }
        
        // Precipitation overlay (middle z-index)
        if (activeLayer == RadarMapViewModel.WeatherLayer.PRECIPITATION && precipitationRadarUrl != null) {
            WeatherOverlay(
                imageUrl = precipitationRadarUrl,
                visible = true,
                transparency = 0.2f, // Decreased from 0.5f for better visibility (less transparency)
                zIndex = 1f,
                showDiagnostics = showDiagnostics
            )
        }
        
        // Wind overlay (highest z-index for weather layers)
        if (activeLayer == RadarMapViewModel.WeatherLayer.WIND && windRadarUrl != null) {
            WeatherOverlay(
                imageUrl = windRadarUrl,
                visible = true,
                transparency = 0.3f,
                zIndex = 2f,
                showDiagnostics = showDiagnostics
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
                snippet = "Distance: ${String.format(Locale.US, "%.1f", station.distance)} km",
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