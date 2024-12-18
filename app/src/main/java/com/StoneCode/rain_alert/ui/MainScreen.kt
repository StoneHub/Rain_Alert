package com.StoneCode.rain_alert.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.StoneCode.rain_alert.viewmodel.WeatherViewModel

@Composable
fun MainScreen(
    onStartServiceClick: () -> Unit,
    onStopServiceClick: () -> Unit, // Add this
    onSimulateFreezeClick: () -> Unit, // Add this
    onSimulateRainClick: () -> Unit,
    onOpenWeatherWebsiteClick: () -> Unit,
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    val isServiceRunning by weatherViewModel.isServiceRunning.observeAsState(false)
    val lastUpdateTime by weatherViewModel.lastUpdateTime.observeAsState("")
    val weatherData by weatherViewModel.weatherData.observeAsState("Loading...")

    // Log the state changes
    Log.d("MainScreen", "Service Running State: $isServiceRunning")
    Log.d("MainScreen", "Last Update Time: $lastUpdateTime")
    Log.d("MainScreen", "Weather Data: $weatherData")

    // Observe lifecycle events to refresh data
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("MainScreen", "Lifecycle Event: ON_RESUME")
                weatherViewModel.updateWeatherStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            weatherViewModel.stopServiceChecker() // Stop checking when the screen is disposed
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (isServiceRunning) {
                        onStopServiceClick() // Call a new function to stop the service
                        Log.d("MainScreen", "Stop Service Button Clicked")
                    } else {
                        onStartServiceClick()
                        Log.d("MainScreen", "Start Service Button Clicked")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceRunning) Color.Green else Color.Red
                )
            ) {
                Text(if (isServiceRunning) "Stop Service" else "Start Service")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                onSimulateRainClick()
                Log.d("MainScreen", "Simulate Rain Button Clicked")
            }) {
                Text("Simulate Rain")
            }

            Button(onClick = {
                onSimulateFreezeClick()
                Log.d("MainScreen", "Simulate Freeze Button Clicked")
            }) {
                Text("Simulate Freeze")
            }

            Spacer(modifier = Modifier.height(16.dp))

            ServiceStatusIndicator(isServiceRunning)

            Spacer(modifier = Modifier.height(16.dp))

            WeatherUpdateSection(weatherData, lastUpdateTime, weatherViewModel, context)

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                onOpenWeatherWebsiteClick()
                Log.d("MainScreen", "Open Weather Website Button Clicked")
            }) {
                Text("Open Weather Website")
            }
        }
    }
}

@Composable
fun ServiceStatusIndicator(isServiceRunning: Boolean) {
    Log.d("ServiceStatusIndicator", "Service Running: $isServiceRunning")
    Text(
        text = "Service Status: ${if (isServiceRunning) "Running" else "Not Running"}",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
fun WeatherUpdateSection(
    weatherData: String,
    lastUpdateTime: String,
    weatherViewModel: WeatherViewModel,
    context: Context
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Log.d("WeatherUpdateSection", "Weather Data: $weatherData")
        Log.d("WeatherUpdateSection", "Last Update Time: $lastUpdateTime")

        Text(
            text = "Weather Data:\n$weatherData",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Last Updated: $lastUpdateTime",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            Log.d("WeatherUpdateSection", "Refresh Weather Button Clicked")
            weatherViewModel.updateWeatherStatus(context)
        }) {
            Text("Refresh Weather")
        }
    }
}