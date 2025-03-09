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
    // Use the shared map component with fullscreen display mode
    SharedMapComponent(
        modifier = modifier,
        displayMode = MapDisplayMode.FULLSCREEN,
        centerLatLng = centerLatLng,
        myLocation = myLocation,
        selectedStations = selectedStations,
        isLoading = isLoading,
        onRefresh = onRefresh,
        onMyLocationClick = onMyLocationClick,
        onBackClick = onBackClick,
        radarMapViewModel = radarMapViewModel
    )
}