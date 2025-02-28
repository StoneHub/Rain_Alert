package com.stoneCode.rain_alert.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.StoneCode.rain_alert.R
import com.stoneCode.rain_alert.viewmodel.WeatherViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun MainScreen(
    onStartServiceClick: () -> Unit,
    onStopServiceClick: () -> Unit,
    onSimulateFreezeClick: () -> Unit,
    onSimulateRainClick: () -> Unit,
    onOpenWeatherWebsiteClick: () -> Unit,
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val isServiceRunning by weatherViewModel.isServiceRunning.observeAsState(false)
    val lastUpdateTime by weatherViewModel.lastUpdateTime.observeAsState("")
    val isDataReady by weatherViewModel.isDataReady.observeAsState(false) // Observe isDataReady
    var weatherData by remember { mutableStateOf("Loading...") }
    var isRefreshing by remember { mutableStateOf(false) }
    var longPressDetected by remember { mutableStateOf(false) }
    var initialContainerSize by remember { mutableStateOf(0.dp) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                weatherViewModel.updateWeatherStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            weatherViewModel.stopServiceChecker()
        }
    }

    // Key the LaunchedEffect on weatherViewModel.weatherData to trigger updates
    LaunchedEffect(weatherViewModel.weatherData, isRefreshing, isDataReady) {
        weatherViewModel.weatherData.value?.let { data ->
            if (!isRefreshing) {
                // If not refreshing, update immediately
                weatherData = data
            } else {
                if (!longPressDetected) {
                    // Short delay before starting to scramble
                    delay(500)
                }

                if (isDataReady) {
                    // If data is ready, update weatherData and stop refreshing
                    weatherData = data
                    isRefreshing = false
                    longPressDetected = false // Reset long press flag
                } else {
                    // If data is not ready, keep the previous data or "Loading..."
                    // (Optional) You could also show a loading indicator here if needed
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Image
                Image(
                    painter = painterResource(id = R.drawable.background_nature),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // App Title
                    AppTitle()

                    // Weather Banner Section
                    WeatherBanner(
                        weatherData = weatherData,
                        lastUpdateTime = lastUpdateTime,
                        isRefreshing = isRefreshing,
                        longPressDetected = longPressDetected,
                        onLongPress = {
                            longPressDetected = true
                            isRefreshing = true
                            weatherViewModel.updateWeatherStatus()
                        },
                        weatherViewModel = weatherViewModel,
                        onSizeCalculated = { size ->
                            if (initialContainerSize < size) {
                                initialContainerSize = size
                            }
                        },
                        containerSize = initialContainerSize
                    )

                    // Control Buttons Section
                    ControlButtons(
                        isServiceRunning = isServiceRunning,
                        onStartServiceClick = onStartServiceClick,
                        onStopServiceClick = onStopServiceClick,
                        onSimulateRainClick = onSimulateRainClick,
                        onSimulateFreezeClick = onSimulateFreezeClick,
                        onOpenWeatherWebsiteClick = onOpenWeatherWebsiteClick
                    )
                }
            }
        }
    )
}