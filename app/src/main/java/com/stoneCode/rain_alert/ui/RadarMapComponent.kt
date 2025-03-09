package com.stoneCode.rain_alert.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.ui.map.MapDisplayMode
import com.stoneCode.rain_alert.ui.map.SharedMapComponent
import com.stoneCode.rain_alert.viewmodel.RadarMapViewModel

/**
 * A radar map component that shows current weather data overlays.
 * Uses the SharedMapComponent with carousel display mode for consistent functionality with fullscreen view.
 */
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (fullScreen) 500.dp else 200.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Use the shared map component with carousel display mode
        SharedMapComponent(
            modifier = Modifier.fillMaxSize(),
            displayMode = MapDisplayMode.CAROUSEL,
            centerLatLng = centerLatLng,
            myLocation = myLocation,
            selectedStations = selectedStations,
            isLoading = isLoading,
            onRefresh = onRefresh,
            onMyLocationClick = onMyLocationClick,
            onToggleFullScreen = onToggleFullScreen,
            onChangeLocationClick = onChangeLocationClick,
            radarMapViewModel = radarMapViewModel
        )
    }
}