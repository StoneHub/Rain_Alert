package com.stoneCode.rain_alert.repository

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class WeatherRepository(private val context: Context) {

    private var lastFreezeCheckTime: Long = 0
    private var isFreezing: Boolean = false

    fun checkForRain(): Boolean {
        // Simulate checking for rain based on location
        val lastKnownLocation = getLastKnownLocation()
        if (lastKnownLocation != null) {
            // Log the location for testing
            Log.d("WeatherRepository", "Location: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")

            // Simulate rain based on location coordinates
            return lastKnownLocation.latitude > 0 && lastKnownLocation.longitude > 0
        } else {
            Log.w("WeatherRepository", "Location not available")
            return false // Default to false if location is not available
        }
    }

    fun checkForFreezeWarning(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFreezeCheckTime > 4 * 60 * 60 * 1000) { // 4 hours
            // Simulate checking for freezing conditions
            isFreezing = Math.random() < 0.5 // Simulate 50% chance of freezing
            lastFreezeCheckTime = currentTime
            Log.d("WeatherRepository", "Freeze check performed: isFreezing=$isFreezing")
        }
        return isFreezing
    }

    private fun getLastKnownLocation(): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted
            Log.w("WeatherRepository", "Location permission not granted")
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var bestLocation: Location? = null
        val providers = locationManager.getProviders(true)

        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                bestLocation = location
            }
        }
        return bestLocation
    }
}