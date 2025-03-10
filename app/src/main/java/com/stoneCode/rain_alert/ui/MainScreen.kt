package com.stoneCode.rain_alert.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.StoneCode.rain_alert.R
import com.stoneCode.rain_alert.ui.dialogs.LocationDialog
import com.stoneCode.rain_alert.ui.dialogs.StationSelectDialog
import com.stoneCode.rain_alert.ui.map.MapDisplayMode
import com.stoneCode.rain_alert.ui.map.SharedMapComponent
import com.stoneCode.rain_alert.viewmodel.WeatherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onStartServiceClick: () -> Unit,
    onStopServiceClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewHistoryClick: () -> Unit,
    onOpenStationWebsiteClick: (String) -> Unit,  // New function to open station website
    onMapClick: () -> Unit, // Function to open the weather map
    onOpenAppSettings: () -> Unit, // Function to open app settings for permissions
    onOpenBatterySettings: () -> Unit, // Function to open battery optimization settings
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val isServiceRunning by weatherViewModel.isServiceRunning.observeAsState(false)
    val lastUpdateTime by weatherViewModel.lastUpdateTime.observeAsState("")
    val isDataReady by weatherViewModel.isDataReady.observeAsState(false)
    val stationData by weatherViewModel.stationData.observeAsState(emptyList())
    var weatherData by remember { mutableStateOf("Loading...") }
    var isRefreshing by remember { mutableStateOf(false) }
    var longPressDetected by remember { mutableStateOf(false) }
    var initialContainerSize by remember { mutableStateOf(0.dp) }

    // Help dialog state
    var showHelpDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(weatherViewModel.weatherData, isRefreshing, isDataReady) {
        weatherViewModel.weatherData.value?.let { data ->
            if (!isRefreshing) {
                weatherData = data
            } else {
                if (!longPressDetected) {
                    delay(500)
                }

                if (isDataReady) {
                    weatherData = data
                    isRefreshing = false
                    longPressDetected = false
                }
            }
        }
    }

    // Ensure we have location data when the screen is shown
    LaunchedEffect(Unit) {
        // Trigger location update when the screen is shown
        if (weatherViewModel.getLastKnownLocation() == null) {
            weatherViewModel.viewModelScope.launch {
                try {
                    weatherViewModel.updateWeatherStatus()
                } catch (e: Exception) {
                    Log.e("MainScreen", "Failed to get location: ${e.message}")
                }
            }
        }
    }

    // Help Dialog
    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false },
            onOpenAppSettings = onOpenAppSettings,
            onOpenBatterySettings = onOpenBatterySettings
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // This will add necessary padding for status bar
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ),
                horizontalArrangement = Arrangement.SpaceBetween, // Changed to SpaceBetween for proper layout
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left section with App title and Switch grouped together
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    // App Title
                    AppTitle(compact = true)
                    
                    // Add spacing between title and switch
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Weather Alert Service Switch with text labels
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isServiceRunning)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isServiceRunning) "On" else "Off",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isServiceRunning)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                            )

                            Switch(
                                checked = isServiceRunning,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) onStartServiceClick() else onStopServiceClick()
                                },
                                thumbContent = {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isServiceRunning)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    checkedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
                
                // Right section with navigation icons - pushed to the right
                Row {
                    // Help/Info button
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = "Help & Information",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onViewHistoryClick) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View Alert History",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        content = { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Image - Choose based on dark mode
                val backgroundResId = if (isSystemInDarkTheme()) {
                    R.drawable.background_nature_dark
                } else {
                    R.drawable.background_nature
                }

                Image(
                    painter = painterResource(id = backgroundResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dialog state
                    var showLocationDialog by rememberSaveable { mutableStateOf(false) }
                    var showStationSelectDialog by rememberSaveable { mutableStateOf(false) }
                    val availableStations by weatherViewModel.availableStations.observeAsState(emptyList())
                    val selectedStationIds by weatherViewModel.selectedStationIds.observeAsState(emptyList())

                    // Launch effect to fetch fresh stations when dialog is about to show
                    LaunchedEffect(showStationSelectDialog) {
                        if (showStationSelectDialog) {
                            // Fetch fresh stations when dialog opens
                            weatherViewModel.fetchAvailableStations()
                        }
                    }

                    // Filter stationData to only include selected stations
                    val displayedStations = stationData.filter { stationObs ->
                        selectedStationIds.contains(stationObs.station.id)
                    }

                    // Weather Map Component
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            // Title at the top - darker for light mode readability
                            Text(
                                text = "Weather Radar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                                color = MaterialTheme.colorScheme.onSurface // Ensures good contrast in both themes
                            )

                            // Map container with fixed height
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            ) {
                                // Get center from current location, stations, or use default
                                val center = weatherViewModel.getLastKnownLocation()?.let {
                                    com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
                                } ?: if (displayedStations.isNotEmpty()) {
                                    val lat = displayedStations.map { it.station.latitude }.average()
                                    val lng = displayedStations.map { it.station.longitude }.average()
                                    com.google.android.gms.maps.model.LatLng(lat, lng)
                                } else {
                                    // Default US center
                                    com.google.android.gms.maps.model.LatLng(40.0, -98.0)
                                }

                                // Render map component
                                SharedMapComponent(
                                    modifier = Modifier.fillMaxSize(),
                                    displayMode = MapDisplayMode.CAROUSEL,
                                    centerLatLng = center,
                                    selectedStations = displayedStations.map { it.station },
                                    isLoading = isRefreshing,
                                    onRefresh = {
                                        isRefreshing = true
                                        weatherViewModel.updateWeatherStatus()
                                    },
                                    onMyLocationClick = {
                                        // Center map on user's location without refreshing data
                                        weatherViewModel.getLastKnownLocation()?.let { location ->
                                            // Pass the location to the SharedMapComponent
                                            val userLocation = com.google.android.gms.maps.model.LatLng(
                                                location.latitude,
                                                location.longitude
                                            )
                                            // Let the SharedMapComponent handle the camera movement
                                        }
                                    },
                                    myLocation = weatherViewModel.getLastKnownLocation()?.let {
                                        com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
                                    },
                                    onToggleFullScreen = onMapClick,
                                    onChangeLocationClick = { showLocationDialog = true }
                                )
                            }
                        }
                    }

                    // Station Data Component
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        if (displayedStations.isNotEmpty()) {
                            StationDataComponent(
                                stations = displayedStations,
                                onSelectStationsClick = { showStationSelectDialog = true },
                                onStationLongClick = onOpenStationWebsiteClick,
                                isLoading = isRefreshing
                            )
                        } else {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = "No Stations",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No weather stations selected", style = MaterialTheme.typography.bodyLarge)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { showStationSelectDialog = true }) {
                                        Text("Select Stations")
                                    }
                                }
                            }
                        }
                    }

                    // Location Dialog
                    if (showLocationDialog) {
                        LocationDialog(
                            onDismiss = { showLocationDialog = false },
                            onConfirm = { zipCode ->
                                weatherViewModel.updateCustomLocation(zipCode, true)
                                showLocationDialog = false
                            },
                            onUseDeviceLocation = {
                                weatherViewModel.updateCustomLocation(null, false)
                                showLocationDialog = false
                            }
                        )
                    }

                    // Station Select Dialog
                    if (showStationSelectDialog) {
                        StationSelectDialog(
                            stations = availableStations,
                            selectedStationIds = selectedStationIds,
                            onDismiss = { showStationSelectDialog = false },
                            onConfirm = { stationIds ->
                                weatherViewModel.updateSelectedStations(stationIds)
                                showStationSelectDialog = false
                            }
                        )
                    }

                    // Add space at the bottom for better scrolling
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    )
}

@Composable
fun HelpDialog(
    onDismiss: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    // Using a custom dialog layout to fix scrolling issues
    AlertDialog(
        onDismissRequest = onDismiss,
        // No title parameter - we'll include it in the content
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)  // Fixed height to ensure dialog is properly sized
            ) {
                // Title section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Rain Alert Help",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Scrollable content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // What the app does
                    SectionWithIcon(
                        icon = Icons.Default.WaterDrop,
                        title = "What Rain Alert Does",
                        description = "Monitors local weather stations for precipitation and freezing conditions, alerting you when potentially dangerous weather is detected."
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // How it works
                    SectionWithIcon(
                        icon = Icons.Default.LocationOn,
                        title = "How It Works",
                        description = "Aggregates data from multiple nearby weather stations, weighing their reliability based on distance. The app periodically checks conditions in the background to deliver timely alerts."
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Required permissions
                    SectionWithIcon(
                        icon = Icons.Default.Notifications,
                        title = "Required Permissions",
                        description = "• Location: To find nearest weather stations\n• Notifications: For weather alerts\n• Background service: For continuous monitoring"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Troubleshooting
                    SectionWithIcon(
                        icon = Icons.Default.BatteryAlert,
                        title = "Troubleshooting",
                        description = "• Ensure location permissions are granted\n• Disable battery optimization to prevent background service interruptions\n• Check notification settings if alerts aren't appearing"
                    )
                }

                // Buttons section - outside the scroll area
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedButton(
                        onClick = onOpenAppSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check Permissions")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onOpenBatterySettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Battery Optimization")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        },
        // These are now null since we're handling everything in the content
        confirmButton = {},
        dismissButton = {},
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun SectionWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 28.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
