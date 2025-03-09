package com.stoneCode.rain_alert.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationProvider(private val context: Context) {
    private val TAG = "LocationProvider"
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // For caching purposes
    private var lastLocation: Location? = null
    private var lastLocationTime: Long = 0
    private val MAX_LOCATION_AGE_MS = 15 * 60 * 1000L // 15 minutes

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Gets the last known location or returns null if no location available or no permission
     */
    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        // Return cached location if it's fresh enough
        lastLocation?.let {
            if (System.currentTimeMillis() - lastLocationTime < MAX_LOCATION_AGE_MS) {
                return it
            }
        }

        return null
    }

    /**
     * Requests a fresh location update. This is a suspending function that will
     * complete when the location is received or an error occurs.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resumeWithException(SecurityException("Location permission not granted"))
            return@suspendCancellableCoroutine
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(10000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Cache the location
                    lastLocation = location
                    lastLocationTime = System.currentTimeMillis()

                    // Remove the callback to prevent more updates
                    fusedLocationClient.removeLocationUpdates(this)

                    // Complete the coroutine
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
            }
        }

        try {
            // Try to get last location first for faster response
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Cache the location
                    lastLocation = location
                    lastLocationTime = System.currentTimeMillis()

                    // Complete the coroutine
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                } else {
                    // If last location is null, request a fresh update
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }.addOnFailureListener { e ->
                // If getting last location fails, request a fresh update
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }

        // Ensure we clean up when the coroutine is cancelled
        continuation.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}