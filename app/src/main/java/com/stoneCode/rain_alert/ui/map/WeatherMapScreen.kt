package com.stoneCode.rain_alert.ui.map

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.*
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.viewmodel.RadarMapViewModel

/**
 * A weather map screen that shows current weather data overlays and station information.
 * Uses the SharedMapComponent for consistent functionality with the carousel view.
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
    // Initialize with precipitation layer if no layer is active
    LaunchedEffect(Unit) {
        Log.d("WeatherMapScreen", "Initializing WeatherMapScreen")
        if (radarMapViewModel.activeLayer.value == RadarMapViewModel.WeatherLayer.NONE) {
            Log.d("WeatherMapScreen", "Setting default precipitation layer")
            radarMapViewModel.setActiveLayer(RadarMapViewModel.WeatherLayer.PRECIPITATION)
        }
        
        // Ensure radar data is fetched
        if (radarMapViewModel.precipitationRadarUrl.value == null) {
            Log.d("WeatherMapScreen", "Fetching radar data")
            radarMapViewModel.fetchRadarData()
        }
    }

    // Use the shared map component with fullscreen display mode
    SharedMapComponent(
        modifier = modifier,
        displayMode = MapDisplayMode.FULLSCREEN,
        centerLatLng = centerLatLng,
        myLocation = myLocation,
        selectedStations = selectedStations,
        isLoading = isLoading,
        onRefresh = {
            Log.d("WeatherMapScreen", "Refreshing data")
            onRefresh()
            radarMapViewModel.refreshRadarData()
        },
        onMyLocationClick = onMyLocationClick,
        onBackClick = onBackClick,
        radarMapViewModel = radarMapViewModel
    )
}