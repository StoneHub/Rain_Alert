package com.stoneCode.rain_alert.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * A helper class that manages radar overlays on a GoogleMap.
 * This class handles loading images and adding/removing overlays properly.
 */
class RadarOverlayManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RadarOverlayManager"
        
        // Continental US bounds for overlay placement (used as default)
        val DEFAULT_US_BOUNDS = LatLngBounds(
            LatLng(24.0, -125.0),  // Southwest corner
            LatLng(49.0, -66.0)    // Northeast corner
        )
    }
    
    // Keep track of active overlays
    private var precipitationOverlay: GroundOverlay? = null
    private var windOverlay: GroundOverlay? = null
    private var temperatureOverlay: GroundOverlay? = null
    
    /**
     * Show or hide the precipitation overlay on the map
     */
    fun togglePrecipitationOverlay(map: GoogleMap, url: String?, show: Boolean, alpha: Float = 0.7f) {
        // Remove existing overlay if it exists
        precipitationOverlay?.remove()
        precipitationOverlay = null
        
        // If not showing or no URL, just return
        if (!show || url == null) return
        
        // Load and add overlay
        addOverlayFromUrl(map, url, alpha, 0f) { overlay ->
            precipitationOverlay = overlay
            Log.d(TAG, "Added precipitation overlay successfully: ${overlay != null}")
        }
    }
    
    /**
     * Show or hide the wind overlay on the map
     */
    fun toggleWindOverlay(map: GoogleMap, url: String?, show: Boolean, alpha: Float = 0.6f) {
        // Remove existing overlay if it exists
        windOverlay?.remove()
        windOverlay = null
        
        // If not showing or no URL, just return
        if (!show || url == null) return
        
        // Load and add overlay
        addOverlayFromUrl(map, url, alpha, 1f) { overlay ->
            windOverlay = overlay
            Log.d(TAG, "Added wind overlay successfully: ${overlay != null}")
        }
    }
    
    /**
     * Show or hide the temperature overlay on the map
     */
    fun toggleTemperatureOverlay(map: GoogleMap, url: String?, show: Boolean, alpha: Float = 0.7f) {
        // Remove existing overlay if it exists
        temperatureOverlay?.remove()
        temperatureOverlay = null
        
        // If not showing or no URL, just return
        if (!show || url == null) return
        
        // Load and add overlay
        addOverlayFromUrl(map, url, alpha, 2f) { overlay ->
            temperatureOverlay = overlay
            Log.d(TAG, "Added temperature overlay successfully: ${overlay != null}")
        }
    }
    
    /**
     * Add an overlay from a URL with proper error handling
     */
    private fun addOverlayFromUrl(
        map: GoogleMap,
        url: String,
        alpha: Float = 0.7f,
        zIndex: Float = 0f,
        callback: (GroundOverlay?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Loading bitmap from: $url")
                val bitmap = loadBitmapFromUrl(url)
                
                // Add overlay on main thread
                withContext(Dispatchers.Main) {
                    bitmap?.let {
                        try {
                            val options = GroundOverlayOptions()
                                .positionFromBounds(DEFAULT_US_BOUNDS)
                                .image(BitmapDescriptorFactory.fromBitmap(it))
                                .transparency(1f - alpha)
                                .zIndex(zIndex)
                                .visible(true)
                            
                            val overlay = map.addGroundOverlay(options)
                            callback(overlay)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error adding ground overlay", e)
                            callback(null)
                        }
                    } ?: run {
                        Log.e(TAG, "Failed to load bitmap from $url")
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bitmap", e)
                withContext(Dispatchers.Main) {
                    callback(null)
                }
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
        
        temperatureOverlay?.remove()
        temperatureOverlay = null
        
        Log.d(TAG, "Removed all overlays")
    }
    
    /**
     * Load a bitmap from a URL with improved error handling
     */
    private suspend fun loadBitmapFromUrl(urlString: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 20000  // 20-second timeout
                connection.readTimeout = 20000     // 20-second read timeout
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
            } finally {
                connection?.disconnect()
            }
        }
    }
}
