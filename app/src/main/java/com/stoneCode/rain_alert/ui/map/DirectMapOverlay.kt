package com.stoneCode.rain_alert.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GroundOverlay
import com.google.maps.android.compose.GroundOverlayPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * A Composable that adds a ground overlay to a Google Map using Jetpack Compose.
 * Enhanced version with better error handling and loading indicator.
 */
@Composable
fun WeatherOverlay(
    imageUrl: String?,
    visible: Boolean = true,
    bounds: LatLngBounds = LatLngBounds.builder()
        .include(com.google.android.gms.maps.model.LatLng(24.0, -125.0))
        .include(com.google.android.gms.maps.model.LatLng(49.0, -66.0))
        .build(),
    transparency: Float = 0.3f,  // 0.0 is fully opaque, 1.0 is fully transparent
    zIndex: Float = 0f
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Load bitmap from URL
    LaunchedEffect(imageUrl, visible) {
        if (imageUrl != null && visible) {
            isLoading = true
            loadError = null
            try {
                Log.d("WeatherOverlay", "Starting to load bitmap from URL: $imageUrl")
                bitmap = loadBitmapFromUrl(imageUrl)
                if (bitmap != null) {
                    Log.d("WeatherOverlay", "Bitmap loaded successfully: ${bitmap?.width}x${bitmap?.height}")
                } else {
                    Log.e("WeatherOverlay", "Failed to load bitmap - result was null")
                    loadError = "Failed to load overlay image"
                }
            } catch (e: Exception) {
                Log.e("WeatherOverlay", "Error loading bitmap: ${e.message}")
                loadError = e.message
                bitmap = null
            } finally {
                isLoading = false
            }
        } else {
            bitmap = null
            isLoading = false
            loadError = null
        }
    }

    // Convert bitmap to BitmapDescriptor
    val bitmapDescriptor = remember(bitmap) {
        bitmap?.let { BitmapDescriptorFactory.fromBitmap(it) }
    }

    // Add overlay to map
    if (visible && bitmapDescriptor != null) {
        GroundOverlay(
            position = GroundOverlayPosition.create(bounds),
            image = bitmapDescriptor,
            transparency = transparency,
            visible = true,
            zIndex = zIndex
        )
        
        // Log information about the displayed overlay
        DisposableEffect(imageUrl) {
            Log.d("WeatherOverlay", "Weather overlay displayed: $imageUrl")
            onDispose {
                Log.d("WeatherOverlay", "Weather overlay removed: $imageUrl")
            }
        }
    }
    
    // Clean up bitmap when component is removed or URL changes
    DisposableEffect(imageUrl) {
        onDispose {
            bitmap = null
        }
    }
}


/**
 * Load a bitmap from a URL
 */
private suspend fun loadBitmapFromUrl(urlString: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            Log.d("WeatherOverlay", "Loading bitmap from URL: $urlString")
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 30000  // 30-second timeout
            connection.readTimeout = 30000     // 30-second read timeout
            connection.connect()
            
            // Check the response code
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("WeatherOverlay", "HTTP error when loading bitmap: $responseCode ${connection.responseMessage}")
                return@withContext null
            }
            
            val input: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            Log.d("WeatherOverlay", "Bitmap loaded successfully: ${bitmap != null}, size: ${bitmap?.width ?: 0}x${bitmap?.height ?: 0}")
            bitmap
        } catch (e: Exception) {
            Log.e("WeatherOverlay", "Error loading bitmap from URL: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }
}
