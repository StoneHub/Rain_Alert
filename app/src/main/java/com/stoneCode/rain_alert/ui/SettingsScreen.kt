package com.stoneCode.rain_alert.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.data.UserPreferences
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }
    val firebaseLogger = remember { FirebaseLogger.getInstance() }
    
    // State for settings
    var freezeThreshold by remember { mutableStateOf(AppConfig.FREEZE_THRESHOLD_F) }
    var rainProbabilityThreshold by remember { mutableStateOf(AppConfig.RAIN_PROBABILITY_THRESHOLD) }
    var enableRainNotifications by remember { mutableStateOf(true) }
    var enableFreezeNotifications by remember { mutableStateOf(true) }
    var customSounds by remember { mutableStateOf(true) }
    
    // Load current preferences
    LaunchedEffect(Unit) {
        freezeThreshold = userPreferences.freezeThreshold.first()
        rainProbabilityThreshold = userPreferences.rainProbabilityThreshold.first()
        enableRainNotifications = userPreferences.enableRainNotifications.first()
        enableFreezeNotifications = userPreferences.enableFreezeNotifications.first()
        customSounds = userPreferences.useCustomSounds.first()
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
            
            // Save button
            Button(
                onClick = {
                    coroutineScope.launch {
                        // Save to UserPreferences
                        userPreferences.updateFreezeThreshold(freezeThreshold)
                        userPreferences.updateRainProbabilityThreshold(rainProbabilityThreshold)
                        userPreferences.updateEnableRainNotifications(enableRainNotifications)
                        userPreferences.updateEnableFreezeNotifications(enableFreezeNotifications)
                        userPreferences.updateUseCustomSounds(customSounds)
                        
                        // Log to Firebase
                        firebaseLogger.logSettingsChanged(
                            freezeThreshold = freezeThreshold,
                            rainProbabilityThreshold = rainProbabilityThreshold,
                            enableRainNotifications = enableRainNotifications,
                            enableFreezeNotifications = enableFreezeNotifications,
                            useCustomSounds = customSounds
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