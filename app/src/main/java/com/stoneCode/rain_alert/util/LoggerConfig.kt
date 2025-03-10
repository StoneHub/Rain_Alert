package com.stoneCode.rain_alert.util

import android.content.Context
import android.util.Log
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Utility class to configure logging early in the application lifecycle
 * to prevent "Too many Flogger logs received before configuration" warnings
 */
object LoggerConfig {
    private const val TAG = "LoggerConfig"
    
    /**
     * Initialize all loggers needed by the application and libraries
     * Call this method in Application.onCreate() before any other initialization
     */
    fun init(context: Context) {
        try {
            // Configure the OkHttp logger to properly track connection leaks
            Logger.getLogger(okhttp3.OkHttpClient::class.java.name).apply {
                level = Level.FINE
            }
            
            // Configure Google Play Services loggers
            Logger.getLogger("com.google.android.gms").apply {
                level = Level.INFO
            }
            
            // Configure Google Maps loggers
            Logger.getLogger("com.google.maps").apply {
                level = Level.INFO
            }
            
            Log.d(TAG, "Logger configuration initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring loggers: ${e.message}")
        }
    }
}