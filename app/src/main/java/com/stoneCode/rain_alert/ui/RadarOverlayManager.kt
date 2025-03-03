package com.stoneCode.rain_alert.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * A helper class that manages radar overlays on a GoogleMap using the android-maps-utils library.
 * This class handles loading images and adding/removing overlays properly.
 */
class RadarOverlayManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RadarOverlayManager"
    }
    
    // Keep track of active overlays
    private var precipitationOverlay: GroundOverlay? = null
    private var windOverlay: GroundOverlay? = null
    
    // Continental US bounds for overlay placement
    private val continentalUsBounds = LatLngBounds(
        com.google.android.gms.maps.model.LatLng(21.0, -126.0),  // Southwest corner
        com.google.android.gms.maps.model.LatLng(50.0, -65.0)    // Northeast corner
    )
    
    /**
     * Show or hide the precipitation overlay on the map with proper z-indexing to ensure
     * markers remain visible
     */
    fun togglePrecipitationOverlay(map: GoogleMap, url: String?, show: Boolean, alpha: Float = 0.7f) {
        // Remove existing overlay if it exists
        precipitationOverlay?.remove()
        precipitationOverlay = null
        
        // If not showing or no URL, just return
        if (!show || url == null) return
        
        // Load and add overlay
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = loadBitmapFromUrl(url)
                
                // Add overlay on main thread
                withContext(Dispatchers.Main) {
                    bitmap?.let {
                        val options = GroundOverlayOptions()
                            .positionFromBounds(continentalUsBounds)
                            .image(BitmapDescriptorFactory.fromBitmap(it))
                            .transparency(1f - alpha)
                            .zIndex(0f)  // Lowest z-index so markers appear on top
                            .visible(true)
                        
                        precipitationOverlay = map.addGroundOverlay(options)
                        Log.d(TAG, "Added precipitation overlay successfully")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load precipitation overlay", e)
            }
        }
    }
    
    /**
     * Show or hide the wind overlay on the map with proper z-indexing to ensure
     * markers remain visible
     */
    fun toggleWindOverlay(map: GoogleMap, url: String?, show: Boolean, alpha: Float = 0.6f) {
        // Remove existing overlay if it exists
        windOverlay?.remove()
        windOverlay = null
        
        // If not showing or no URL, just return
        if (!show || url == null) return
        
        // Load and add overlay
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bitmap = loadBitmapFromUrl(url)
                
                // Add overlay on main thread
                withContext(Dispatchers.Main) {
                    bitmap?.let {
                        val options = GroundOverlayOptions()
                            .positionFromBounds(continentalUsBounds)
                            .image(BitmapDescriptorFactory.fromBitmap(it))
                            .transparency(1f - alpha)
                            .zIndex(0.5f)  // Above precipitation but below markers
                            .visible(true)
                        
                        windOverlay = map.addGroundOverlay(options)
                        Log.d(TAG, "Added wind overlay successfully")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load wind overlay", e)
            }
        }
    }
    
    /**
     * Remove all overlays from the map
     */
    fun removeAllOverlays() {
        precipitationOverlay?.remove()
        precipitationOverlay = null
        
        windOverlay?.remove()
        windOverlay = null
        
        Log.d(TAG, "Removed all overlays")
    }
    
    /**
     * Load a bitmap from a URL with improved error handling
     */
    private suspend fun loadBitmapFromUrl(urlString: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading bitmap from: $urlString")
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 15000  // 15-second timeout
                connection.readTimeout = 15000     // 15-second read timeout
                connection.connect()
                
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error when loading bitmap: $responseCode")
                    return@withContext null
                }
                
                val input: InputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)
                Log.d(TAG, "Bitmap loaded successfully: ${bitmap != null}")
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bitmap from URL", e)
                null
            }
        }
    }
}