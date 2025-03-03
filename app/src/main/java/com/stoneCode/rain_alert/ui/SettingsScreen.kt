package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.util.Log
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.data.UserPreferences
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit,
    onSimulateRainClick: () -> Unit = {},
    onSimulateFreezeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }
    val firebaseLogger = remember { FirebaseLogger.getInstance() }
    
    // State for settings (initialized with defaults, will be updated with actual values)
    var freezeThreshold by remember { mutableStateOf(AppConfig.FREEZE_THRESHOLD_F) }
    var freezeDurationHours by remember { mutableStateOf(AppConfig.FREEZE_DURATION_HOURS) }
    var rainProbabilityThreshold by remember { mutableStateOf(AppConfig.RAIN_PROBABILITY_THRESHOLD) }
    var enableRainNotifications by remember { mutableStateOf(true) }
    var enableFreezeNotifications by remember { mutableStateOf(true) }
    var customSounds by remember { mutableStateOf(true) }
    
    // Flag to track if settings are loaded from preferences
    var settingsLoaded by remember { mutableStateOf(false) }
    
    // Load current preferences
    LaunchedEffect(Unit) {
        try {
            // Load each preference individually with proper error handling
            userPreferences.freezeThreshold.first().let {
                freezeThreshold = it
            }
            
            userPreferences.freezeDurationHours.first().let {
                freezeDurationHours = it
            }
            
            userPreferences.rainProbabilityThreshold.first().let {
                rainProbabilityThreshold = it
            }
            
            userPreferences.enableRainNotifications.first().let {
                enableRainNotifications = it
            }
            
            userPreferences.enableFreezeNotifications.first().let {
                enableFreezeNotifications = it
            }
            
            userPreferences.useCustomSounds.first().let {
                customSounds = it
            }
            
            // Mark settings as loaded to avoid overwriting with defaults
            settingsLoaded = true
            
            // Log that settings were loaded successfully
            Log.d("SettingsScreen", "Loaded preferences: " +
                "freezeThreshold=$freezeThreshold, " +
                "freezeDurationHours=$freezeDurationHours, " +
                "rainProbabilityThreshold=$rainProbabilityThreshold, " +
                "enableRainNotifications=$enableRainNotifications, " +
                "enableFreezeNotifications=$enableFreezeNotifications, " +
                "customSounds=$customSounds")
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error loading preferences", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Weather Thresholds",
                style = MaterialTheme.typography.titleLarge
            )
            
            // Freeze threshold slider
            Text("Freeze Warning Threshold: ${freezeThreshold.toInt()}°F")
            Slider(
                value = freezeThreshold.toFloat(),
                onValueChange = { freezeThreshold = it.toDouble() },
                valueRange = 25f..45f,
                steps = 20,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Freeze duration slider
            Text("Freeze Duration Threshold: $freezeDurationHours hours")
            Slider(
                value = freezeDurationHours.toFloat(),
                onValueChange = { freezeDurationHours = it.toInt() },
                valueRange = 1f..12f,
                steps = 11,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Rain probability slider
            Text("Rain Probability Threshold: $rainProbabilityThreshold%")
            Slider(
                value = rainProbabilityThreshold.toFloat(),
                onValueChange = { rainProbabilityThreshold = it.toInt() },
                valueRange = 10f..90f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleLarge
            )
            
            // Rain notification toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rain Notifications")
                Switch(
                    checked = enableRainNotifications,
                    onCheckedChange = { enableRainNotifications = it }
                )
            }
            
            // Freeze notification toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Freeze Warnings")
                Switch(
                    checked = enableFreezeNotifications,
                    onCheckedChange = { enableFreezeNotifications = it }
                )
            }
            
            // Custom sounds toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Use Custom Sounds")
                Switch(
                    checked = customSounds,
                    onCheckedChange = { customSounds = it }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Developer Tools Section
            Text(
                text = "Developer Tools",
                style = MaterialTheme.typography.titleLarge
            )
            
            Text(
                "Use these tools to test notification functionality",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Simulation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Simulate Rain button
                OutlinedButton(
                    onClick = onSimulateRainClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simulate Rain")
                }
                
                // Simulate Freeze button
                OutlinedButton(
                    onClick = onSimulateFreezeClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simulate Freeze")
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Save button
            Button(
                onClick = {
                    coroutineScope.launch {
                        // Log before saving
                        Log.d("SettingsScreen", "Saving preferences: " +
                            "freezeThreshold=$freezeThreshold, " +
                            "freezeDurationHours=$freezeDurationHours, " +
                            "rainProbabilityThreshold=$rainProbabilityThreshold")
                            
                        // Save to UserPreferences
                        userPreferences.updateFreezeThreshold(freezeThreshold)
                        userPreferences.updateFreezeDurationHours(freezeDurationHours)
                        userPreferences.updateRainProbabilityThreshold(rainProbabilityThreshold)
                        userPreferences.updateEnableRainNotifications(enableRainNotifications)
                        userPreferences.updateEnableFreezeNotifications(enableFreezeNotifications)
                        userPreferences.updateUseCustomSounds(customSounds)
                        
                        // Verify the values were stored correctly by reading them back
                        val verifyFreezeThreshold = userPreferences.freezeThreshold.first()
                        val verifyFreezeDuration = userPreferences.freezeDurationHours.first()
                        val verifyRainThreshold = userPreferences.rainProbabilityThreshold.first()
                        
                        Log.d("SettingsScreen", "Verified saved preferences: " +
                            "freezeThreshold=$verifyFreezeThreshold, " +
                            "freezeDurationHours=$verifyFreezeDuration, " +
                            "rainProbabilityThreshold=$verifyRainThreshold")
                        
                        // Log to Firebase
                        firebaseLogger.logSettingsChanged(
                            freezeThreshold = freezeThreshold,
                            rainProbabilityThreshold = rainProbabilityThreshold,
                            enableRainNotifications = enableRainNotifications,
                            enableFreezeNotifications = enableFreezeNotifications,
                            useCustomSounds = customSounds,
                            freezeDurationHours = freezeDurationHours
                        )
                    }
                    onBackPressed()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}