package com.stoneCode.rain_alert.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GroundOverlay
import com.google.maps.android.compose.GroundOverlayPosition
import com.google.maps.android.compose.Polygon
import com.stoneCode.rain_alert.util.MapCoordinateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * A Composable that adds a ground overlay to a Google Map using Jetpack Compose.
 * Enhanced version with better error handling, loading indicator, and diagnostic capabilities.
 */
@Composable
fun WeatherOverlay(
    imageUrl: String?,
    visible: Boolean = true,
    customBounds: LatLngBounds? = null,
    transparency: Float = 0.5f,  // 0.0 is fully opaque, 1.0 is fully transparent
    zIndex: Float = 0f,
    showDiagnostics: Boolean = false
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Try to extract bbox from the URL or use the custom bounds provided
    val bounds = remember(imageUrl, customBounds) {
        try {
            if (imageUrl != null && imageUrl.contains("BBOX=")) {
                val bboxParam = imageUrl.substringAfter("BBOX=").substringBefore("&")
                MapCoordinateUtils.bboxToLatLngBounds(bboxParam)
            } else {
                customBounds ?: LatLngBounds.builder()
                    .include(LatLng(24.0, -125.0))
                    .include(LatLng(49.0, -66.0))
                    .build()
            }
        } catch (e: Exception) {
            Log.e("WeatherOverlay", "Error extracting bbox from URL: ${e.message}")
            customBounds ?: LatLngBounds.builder()
                .include(LatLng(24.0, -125.0))
                .include(LatLng(49.0, -66.0))
                .build()
        }
    }

    // Advanced URL parameters extraction for logging and diagnostics
    val urlParameters = remember(imageUrl) {
        imageUrl?.let {
            // Extract key parameters from URL for analysis
            val map = mutableMapOf<String, String>()
            val queryString = it.substringAfter('?', "")
            queryString.split('&').forEach { param ->
                val parts = param.split('=', limit = 2)
                if (parts.size == 2) {
                    map[parts[0]] = parts[1]
                }
            }
            map
        } ?: emptyMap()
    }

    // Load bitmap from URL with enhanced logging
    LaunchedEffect(imageUrl, visible) {
        if (imageUrl != null && visible) {
            isLoading = true
            loadError = null
            val startTime = System.currentTimeMillis()

            try {
                Log.d("WeatherOverlay", "===== Weather Overlay Loading =====")
                Log.d("WeatherOverlay", "URL: $imageUrl")
                Log.d("WeatherOverlay", "Map Bounds: $bounds")
                Log.d("WeatherOverlay", "Layer: ${urlParameters["LAYERS"]}")
                if (urlParameters.containsKey("BBOX")) {
                    Log.d("WeatherOverlay", "BBOX: ${urlParameters["BBOX"]}")
                }

                bitmap = loadBitmapFromUrl(imageUrl)
                val loadTime = System.currentTimeMillis() - startTime

                if (bitmap != null) {
                    Log.d("WeatherOverlay", "✓ SUCCESS: Bitmap loaded in ${loadTime}ms")
                    Log.d("WeatherOverlay", "Dimensions: ${bitmap?.width}x${bitmap?.height}")

                    // Check if the bitmap is too small or empty - potential problem indicator
                    if ((bitmap?.width ?: 0) < 50 || (bitmap?.height ?: 0) < 50) {
                        Log.w("WeatherOverlay", "⚠ WARNING: Bitmap dimensions are very small - possible empty image")
                    }
                } else {
                    Log.e("WeatherOverlay", "✗ ERROR: Failed to load bitmap - result was null")
                    loadError = "Failed to load overlay image"
                }
            } catch (e: Exception) {
                Log.e("WeatherOverlay", "✗ ERROR: ${e.message}")
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

        // Show diagnostic info overlay if enabled
        if (showDiagnostics) {
            Polygon(
                points = listOf(
                    LatLng(bounds.southwest.latitude, bounds.southwest.longitude),
                    LatLng(bounds.southwest.latitude, bounds.northeast.longitude),
                    LatLng(bounds.northeast.latitude, bounds.northeast.longitude),
                    LatLng(bounds.northeast.latitude, bounds.southwest.longitude)
                ),
                fillColor = androidx.compose.ui.graphics.Color.Transparent,
                strokeColor = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.8f),
                strokeWidth = 2f,
                zIndex = zIndex + 0.1f
            )
        }
    }

    // Clean up bitmap
    DisposableEffect(imageUrl) {
        onDispose {
            bitmap = null
        }
    }
}

/**
 * Helper to load a Bitmap from a URL on an IO thread with enhanced error handling and logging.
 */
private suspend fun loadBitmapFromUrl(urlString: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val startTime = System.currentTimeMillis()
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 30000  // 30-second timeout
            connection.readTimeout = 30000

            // Add a User-Agent to avoid potential server restrictions
            connection.setRequestProperty("User-Agent", "Rain Alert App (Android)")
            connection.connect()

            val responseCode = connection.responseCode
            val connectTime = System.currentTimeMillis() - startTime
            Log.d("WeatherOverlay", "Connection established in ${connectTime}ms, response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("WeatherOverlay", "HTTP error when loading bitmap: $responseCode ${connection.responseMessage}")
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.e("WeatherOverlay", "The radar image was not found (404) - may indicate server configuration issue")
                } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    Log.e("WeatherOverlay", "Access forbidden (403) - may need to check User-Agent requirements")
                }
                return@withContext null
            }

            // Log content type and size for debugging
            val contentType = connection.contentType
            val contentLength = connection.contentLength
            Log.d("WeatherOverlay", "Content type: $contentType, size: ${contentLength / 1024}KB")

            if (contentType?.startsWith("image/") != true) {
                Log.w("WeatherOverlay", "WARNING: Server returned non-image content type: $contentType")
            }

            val input: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            
            // Important! Close the input stream to prevent connection leaks
            input.close()

            val totalTime = System.currentTimeMillis() - startTime
            Log.d("WeatherOverlay", "Total bitmap load time: ${totalTime}ms")

            bitmap
        } catch (e: Exception) {
            Log.e("WeatherOverlay", "Error loading bitmap: ${e.message}")
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }
}
