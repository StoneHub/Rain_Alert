package com.stoneCode.rain_alert.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.*
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
 *
 * This file is unchanged from before; the main layering fixes are done by adjusting
 * z-indexes in the call sites (i.e., RadarMapComponent).
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
                    Log.d("WeatherOverlay", "Bitmap loaded: ${bitmap?.width}x${bitmap?.height}")
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

    // Add overlay to map if everything is ready
    if (visible && bitmapDescriptor != null) {
        GroundOverlay(
            position = GroundOverlayPosition.create(bounds),
            image = bitmapDescriptor,
            transparency = transparency,
            visible = true,
            zIndex = zIndex
        )
    }

    // Clean up bitmap
    DisposableEffect(imageUrl) {
        onDispose {
            bitmap = null
        }
    }
}

/**
 * Helper to load a Bitmap from a URL on an IO thread.
 */
private suspend fun loadBitmapFromUrl(urlString: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 30000  // 30-second timeout
            connection.readTimeout = 30000
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("WeatherOverlay", "HTTP error when loading bitmap: $responseCode ${connection.responseMessage}")
                return@withContext null
            }

            val input: InputStream = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.e("WeatherOverlay", "Error loading bitmap: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }
}
