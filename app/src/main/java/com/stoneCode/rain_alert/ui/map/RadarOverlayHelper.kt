package com.stoneCode.rain_alert.ui.map

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class for managing radar overlays on a GoogleMap
 */
object RadarOverlayHelper {
    
    private const val TAG = "RadarOverlayHelper"
    
    // Continental US bounds for overlay placement
    val DEFAULT_US_BOUNDS = LatLngBounds(
        LatLng(24.0, -125.0),  // Southwest corner
        LatLng(49.0, -66.0)    // Northeast corner
    )

    // Track active overlays (useful for cleanup)
    private var activeOverlays = mutableListOf<GroundOverlay>()
    
    // References to active overlays by type (optional, for selective updates)
    private var precipitationOverlay: GroundOverlay? = null
    private var windOverlay: GroundOverlay? = null
    private var temperatureOverlay: GroundOverlay? = null
    
    /**
     * Add a precipitation layer overlay to the map
     */
    fun showPrecipitationOverlay(
        map: GoogleMap?, 
        context: Context,
        url: String?,
        showLayer: Boolean
    ) {
        if (map == null || !showLayer || url == null) {
            // Remove existing overlay if conditions aren't met
            removePrecipitationOverlay()
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Add the overlay
                android.util.Log.d(TAG, "Adding precipitation overlay from URL: $url")
                
                // First remove any existing overlay of this type
                removePrecipitationOverlay()
                
                // Use your existing overlay manager or create one
                val overlayManager = RadarOverlayManager(context)
                overlayManager.togglePrecipitationOverlay(map, url, true, 0.7f)
                
                // The overlay will be tracked within the RadarOverlayManager
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error adding precipitation overlay", e)
            }
        }
    }
    
    /**
     * Add a wind layer overlay to the map
     */
    fun showWindOverlay(
        map: GoogleMap?, 
        context: Context,
        url: String?,
        showLayer: Boolean
    ) {
        if (map == null || !showLayer || url == null) {
            // Remove existing overlay if conditions aren't met
            removeWindOverlay()
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Add the overlay
                android.util.Log.d(TAG, "Adding wind overlay from URL: $url")
                
                // First remove any existing overlay of this type
                removeWindOverlay()
                
                // Use your existing overlay manager
                val overlayManager = RadarOverlayManager(context)
                overlayManager.toggleWindOverlay(map, url, true, 0.6f)
                
                // The overlay will be tracked within the RadarOverlayManager
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error adding wind overlay", e)
            }
        }
    }
    
    /**
     * Add a temperature layer overlay to the map
     */
    fun showTemperatureOverlay(
        map: GoogleMap?, 
        context: Context,
        url: String?,
        showLayer: Boolean
    ) {
        if (map == null || !showLayer || url == null) {
            // Remove existing overlay if conditions aren't met
            removeTemperatureOverlay()
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Add the overlay
                android.util.Log.d(TAG, "Adding temperature overlay from URL: $url")
                
                // First remove any existing overlay of this type
                removeTemperatureOverlay()
                
                // Use your existing overlay manager
                val overlayManager = RadarOverlayManager(context)
                overlayManager.toggleTemperatureOverlay(map, url, true, 0.7f)
                
                // The overlay will be tracked within the RadarOverlayManager
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error adding temperature overlay", e)
            }
        }
    }
    
    /**
     * Remove the precipitation overlay
     */
    fun removePrecipitationOverlay() {
        try {
            precipitationOverlay?.remove()
            precipitationOverlay = null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error removing precipitation overlay", e)
        }
    }
    
    /**
     * Remove the wind overlay
     */
    fun removeWindOverlay() {
        try {
            windOverlay?.remove()
            windOverlay = null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error removing wind overlay", e)
        }
    }
    
    /**
     * Remove the temperature overlay
     */
    fun removeTemperatureOverlay() {
        try {
            temperatureOverlay?.remove()
            temperatureOverlay = null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error removing temperature overlay", e)
        }
    }
    
    /**
     * Remove all overlays
     */
    fun removeAllOverlays() {
        removePrecipitationOverlay()
        removeWindOverlay()
        removeTemperatureOverlay()
        
        // Also clean up any other overlays in the active overlays list
        for (overlay in activeOverlays) {
            try {
                overlay.remove()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error removing overlay", e)
            }
        }
        activeOverlays.clear()
    }
}
