package com.stoneCode.rain_alert

import android.app.Application
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.firebase.FirestoreManager
import com.stoneCode.rain_alert.util.LoggerConfig
import com.stoneCode.rain_alert.util.VersionManager
import kotlinx.coroutines.launch

/**
 * Custom Application class to handle application-level initialization
 */
class RainAlertApplication : Application() {
    private lateinit var versionManager: VersionManager
    private val TAG = "RainAlertApplication"

    override fun onCreate() {
        super.onCreate()
        
        // Initialize loggers first to avoid "Too many Flogger logs received before configuration" warnings
        LoggerConfig.init(this)
        
        // Initialize Firebase components
        FirebaseLogger.getInstance().initialize(this)
        FirestoreManager.getInstance().initialize(this)
        
        // Initialize version manager
        versionManager = VersionManager(this)
        
        // Use the application process lifecycle to check for updates after initialization
        // This will run once when the app starts, but outside the critical initialization path
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            try {
                val wasUpdated = versionManager.checkForUpdate()
                if (wasUpdated) {
                    Log.i(TAG, "App was updated, performing post-update actions")
                    // Perform post-update steps here
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for app updates", e)
            }
        }
    }
}