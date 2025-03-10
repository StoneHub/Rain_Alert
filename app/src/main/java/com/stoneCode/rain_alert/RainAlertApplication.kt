package com.stoneCode.rain_alert

import android.app.Application
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.firebase.FirestoreManager
import com.stoneCode.rain_alert.util.LoggerConfig

/**
 * Custom Application class to handle application-level initialization
 */
class RainAlertApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize loggers first to avoid "Too many Flogger logs received before configuration" warnings
        LoggerConfig.init(this)
        
        // Initialize Firebase components
        FirebaseLogger.getInstance().initialize(this)
        FirestoreManager.getInstance().initialize(this)
    }
}