package com.stoneCode.rain_alert

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.util.Log
import com.stoneCode.rain_alert.ui.theme.Rain_AlertTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.service.RainService
import com.stoneCode.rain_alert.ui.MainScreen
import com.stoneCode.rain_alert.ui.SettingsScreen
import com.stoneCode.rain_alert.repository.WeatherRepository
import com.stoneCode.rain_alert.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {
    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var firebaseLogger: FirebaseLogger

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        weatherViewModel = WeatherViewModel(application)
        weatherRepository = WeatherRepository(this)
        
        // Initialize Firebase Logger
        firebaseLogger = FirebaseLogger.getInstance()
        firebaseLogger.initialize(this)
        
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
        
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    onStartServiceClick = {
                        Log.d("MainActivity", "Start Service button clicked")
                        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                hasRequiredPermissions()
                            } else {
                                TODO("VERSION.SDK_INT < UPSIDE_DOWN_CAKE")
                            }
                        ) {
                            startRainService()
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                requestRequiredPermissions()
                            }
                        }
                    },
                    onStopServiceClick = {
                        Log.d("MainActivity", "Stopping RainService")
                        val stopIntent = Intent(this@MainActivity, RainService::class.java)
                        stopIntent.action = "STOP_SERVICE"
                        stopService(stopIntent)
                    },
                    onSimulateRainClick = {
                        Log.d("MainActivity", "Simulate Rain button clicked")
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
                    onOpenWeatherWebsiteClick = {
                        Log.d("MainActivity", "Open Weather Website button clicked")
                        openWeatherWebsite()
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                    weatherViewModel = weatherViewModel
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBackPressed = {
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
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

    private fun openWeatherWebsite() {
        Log.d("MainActivity", "Opening Weather Website")
        val location = weatherRepository.getLastKnownLocation()
        if (location != null) {
            val websiteUrl = "https://forecast.weather.gov/MapClick.php?lat=${location.latitude}&lon=${location.longitude}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
            startActivity(intent)
        } else {
            Log.w("MainActivity", "Could not get location to open weather website")
            Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
        }
    }
}