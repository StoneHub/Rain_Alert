package com.stoneCode.rain_alert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.repository.WeatherRepository
import com.stoneCode.rain_alert.service.RainService
import com.stoneCode.rain_alert.ui.AlertHistoryScreen
import com.stoneCode.rain_alert.ui.MainScreen
import com.stoneCode.rain_alert.ui.SettingsScreen
import com.stoneCode.rain_alert.ui.map.WeatherMapScreen
import com.stoneCode.rain_alert.ui.theme.Rain_AlertTheme
import com.stoneCode.rain_alert.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var firebaseLogger: FirebaseLogger

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isFineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val isForegroundServiceLocationGranted =
            permissions[Manifest.permission.FOREGROUND_SERVICE_LOCATION] ?: false
        val isPostNotificationsGranted =
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false

        Log.d("MainActivity", "Fine Location Granted: $isFineLocationGranted")
        Log.d(
            "MainActivity",
            "Foreground Service Location Granted: $isForegroundServiceLocationGranted"
        )
        Log.d("MainActivity", "Post Notifications Granted: $isPostNotificationsGranted")

        if (isFineLocationGranted && isForegroundServiceLocationGranted && isPostNotificationsGranted) {
            startRainService()
        } else {
            if (!isFineLocationGranted) {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                Log.w("MainActivity", "Location permission denied")
            }
            if (!isForegroundServiceLocationGranted) {
                Toast.makeText(
                    this,
                    "Foreground service location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
                Log.w("MainActivity", "Foreground service location permission denied")
            }
            if (!isPostNotificationsGranted) {
                Toast.makeText(
                    this,
                    "Notification permission denied, you will not receive alerts.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.w("MainActivity", "Post notifications permission denied")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        weatherViewModel = WeatherViewModel(application)
        weatherRepository = WeatherRepository(this)
        
        // Initialize Firebase Logger
        firebaseLogger = FirebaseLogger.getInstance()
        firebaseLogger.initialize(this)
        
        // Check for permissions right away if coming from a notification
        if (intent != null && (intent.hasExtra("checkPermissions") || intent.action == "android.settings.APP_NOTIFICATION_SETTINGS")) {
            Log.d("MainActivity", "Checking permissions on launch")
            if (hasRequiredPermissions()) {
                // If permissions are now granted and we were coming from a notification, start the service
                startRainService()
            } else {
                // If still missing permissions, request them
                requestRequiredPermissions()
            }
        }
        
        enableEdgeToEdge()
        setContent {
            Rain_AlertTheme {
                AppNavigation()
            }
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        
        // Log screen views when navigation destination changes
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        
        LaunchedEffect(currentBackStackEntry) {
            currentBackStackEntry?.destination?.route?.let { route ->
                val screenName = when (route) {
                    "main" -> "Main Screen"
                    "alert_history" -> "Alert History"
                    "settings" -> "Settings"
                    "weather_map" -> "Weather Map"
                    "privacy_policy" -> "Privacy Policy"
                    else -> route
                }
                firebaseLogger.logScreenView(screenName, "MainActivity")
            }
        }
        
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    onStartServiceClick = {
                        Log.d("MainActivity", "Start Service button clicked")
                        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                hasRequiredPermissions()
                            } else {
                                // For older versions - need at least the location permission
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            }
                        ) {
                            startRainService()
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestRequiredPermissions()
                            } else {
                                // For older versions - request just the location permission
                                requestPermissions(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    100
                                )
                            }
                        }
                    },
                    onStopServiceClick = {
                        Log.d("MainActivity", "Stopping RainService")
                        val stopIntent = Intent(this@MainActivity, RainService::class.java)
                        stopIntent.action = "STOP_SERVICE"
                        stopService(stopIntent)
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                    onViewHistoryClick = {
                        navController.navigate("alert_history")
                    },
                    onOpenStationWebsiteClick = { stationUrl ->
                        openStationWebsite(stationUrl)
                    },
                    onMapClick = {
                        navController.navigate("weather_map")
                    },
                    onOpenAppSettings = {
                        Log.d("MainActivity", "Opening app settings")
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error opening app settings", e)
                            Toast.makeText(this@MainActivity, "Could not open app settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenBatterySettings = {
                        Log.d("MainActivity", "Opening battery optimization settings")
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error opening battery settings", e)
                            Toast.makeText(this@MainActivity, "Could not open battery settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    weatherViewModel = weatherViewModel
                )
            }
            
            composable("alert_history") {
                AlertHistoryScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    weatherViewModel = weatherViewModel
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onSimulateRainClick = {
                        simulateRain()
                    },
                    onSimulateFreezeClick = {
                        Log.d("MainActivity", "Simulating Freeze Warning")
                        val simulateFreezeIntent = Intent(this@MainActivity, RainService::class.java)
                        simulateFreezeIntent.action = "SIMULATE_FREEZE"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(simulateFreezeIntent)
                        } else {
                            startService(simulateFreezeIntent)
                        }
                    },
                    onPrivacyPolicyClick = {
                        navController.navigate("privacy_policy")
                    }
                )
            }
            
            composable("privacy_policy") {
                PrivacyPolicyScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("weather_map") {
                WeatherMapScreen(
                    myLocation = weatherRepository.getLastKnownLocation()?.let { 
                        LatLng(it.latitude, it.longitude) 
                    },
                    onRefresh = {
                        // Refresh weather data
                        weatherViewModel.updateWeatherStatus()
                    },
                    onMyLocationClick = {
                        // Request location permission if needed
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (!hasRequiredPermissions()) {
                                requestRequiredPermissions()
                            }
                        }
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        weatherViewModel.registerServiceStatusListener()
        weatherViewModel.updateWeatherStatus()
    }

    override fun onPause() {
        super.onPause()
        weatherViewModel.unregisterServiceStatusListener()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun hasRequiredPermissions(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val foregroundServiceLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val postNotificationsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        Log.d("MainActivity", "Has Required Permissions - Fine Location Granted: $fineLocationGranted")
        Log.d("MainActivity", "Has Required Permissions - Foreground Service Location Granted: $foregroundServiceLocationGranted")
        Log.d("MainActivity", "Has Required Permissions - Post Notifications Granted: $postNotificationsGranted")

        return fineLocationGranted && foregroundServiceLocationGranted && postNotificationsGranted
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        Log.d("MainActivity", "Requesting permissions: $permissionsToRequest")

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startRainService() {
        Log.d("MainActivity", "Starting RainService")
        val serviceIntent = Intent(this, RainService::class.java).apply {
            action = "START_RAIN_CHECK"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        // Log service start to Firebase
        firebaseLogger.logServiceStatusChanged(true)
    }

    private fun simulateRain() {
        Log.d("MainActivity", "Simulating Rain")
        val serviceIntent = Intent(this, RainService::class.java).apply {
            action = "SIMULATE_RAIN"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun openStationWebsite(stationUrl: String) {
        Log.d("MainActivity", "Opening Station Website: $stationUrl")
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stationUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening station URL: $stationUrl", e)
            Toast.makeText(this, "Could not open station website", Toast.LENGTH_SHORT).show()
        }
    }

}