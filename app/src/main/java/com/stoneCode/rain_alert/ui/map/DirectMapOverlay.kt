package com.stoneCode.rain_alert.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
 * This creates a direct overlay on the map that will be visible to the user.
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

    // Load bitmap from URL
    LaunchedEffect(imageUrl, visible) {
        if (imageUrl != null && visible) {
            try {
                bitmap = loadBitmapFromUrl(imageUrl)
            } catch (e: Exception) {
                android.util.Log.e("WeatherOverlay", "Error loading bitmap: ${e.message}")
            }
        } else {
            bitmap = null
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
            android.util.Log.d("WeatherOverlay", "Loading bitmap from URL: $urlString")
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 30000  // 30-second timeout
            connection.readTimeout = 30000     // 30-second read timeout
            connection.connect()
            
            val input: InputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            android.util.Log.d("WeatherOverlay", "Bitmap loaded successfully: ${bitmap != null}")
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("WeatherOverlay", "Error loading bitmap from URL", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}
