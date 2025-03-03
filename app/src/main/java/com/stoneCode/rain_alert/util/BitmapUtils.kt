package com.stoneCode.rain_alert.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility functions for working with bitmaps for map overlays
 */
object BitmapUtils {
    
    /**
     * Load a bitmap from a URL
     */
    suspend fun loadBitmapFromUrl(urlString: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                
                val input: InputStream = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Create a BitmapDescriptor from a URL
     */
    suspend fun createBitmapDescriptorFromUrl(urlString: String): BitmapDescriptor? {
        val bitmap = loadBitmapFromUrl(urlString)
        return bitmap?.let { BitmapDescriptorFactory.fromBitmap(it) }
    }
}
