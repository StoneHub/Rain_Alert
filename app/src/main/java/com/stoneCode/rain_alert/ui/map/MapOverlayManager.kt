package com.stoneCode.rain_alert.ui.map

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.stoneCode.rain_alert.util.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper object for managing ground overlays on Google Maps
 */
object MapOverlayManager {
    
    private const val TAG = "MapOverlayManager"
    
    // Continental US bounds for overlay placement (can be reused)
    val DEFAULT_US_BOUNDS = LatLngBounds(
        LatLng(21.0, -126.0),  // Southwest corner
        LatLng(50.0, -65.0)    // Northeast corner
    )
    
    /**
     * Non-composable function to add a ground overlay to GoogleMap
     * Use this from non-Composable contexts like callbacks
     */
    fun addGroundOverlayFromUrl(
        googleMap: GoogleMap,
        context: Context,
        imageUrl: String,
        bounds: LatLngBounds = DEFAULT_US_BOUNDS,
        alpha: Float = 0.7f,
        zIndex: Float = 0f,
        callback: (GroundOverlay?) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Loading bitmap from: $imageUrl")
                // Load the bitmap from URL
                val bitmap = BitmapUtils.loadBitmapFromUrl(imageUrl)
                
                // Add overlay on main thread if bitmap loaded successfully
                withContext(Dispatchers.Main) {
                    bitmap?.let { bmp ->
                        val options = GroundOverlayOptions()
                            .positionFromBounds(bounds)
                            .image(BitmapDescriptorFactory.fromBitmap(bmp))
                            .transparency(1f - alpha)
                            .zIndex(zIndex)
                            .visible(true)
                        
                        try {
                            val overlay = googleMap.addGroundOverlay(options)
                            callback(overlay)
                            Log.d(TAG, "Added overlay successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error adding ground overlay", e)
                            callback(null)
                        }
                    } ?: run {
                        Log.e(TAG, "Failed to load bitmap from $imageUrl")
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load overlay", e)
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }
    
    /**
     * Remove overlay safely
     */
    fun removeOverlay(overlay: GroundOverlay?) {
        try {
            overlay?.remove()
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay", e)
        }
    }
}
