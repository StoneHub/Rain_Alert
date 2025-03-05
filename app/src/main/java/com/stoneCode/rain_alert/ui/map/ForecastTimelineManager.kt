package com.stoneCode.rain_alert.ui.map

import android.util.Log
import com.stoneCode.rain_alert.data.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Manager class to fetch and manage forecast timeline data for radar animations
 */
class ForecastTimelineManager {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Cache of generated URLs for each time step to avoid regenerating them
    private val urlCache = mutableMapOf<Long, String>()
    
    /**
     * Generate a list of time steps for forecast animation
     * 
     * @param hoursInPast Number of hours in the past to include in the timeline
     * @param hoursInFuture Number of hours in the future to include in the timeline
     * @param intervalHours Interval between time steps in hours
     * @return List of ForecastTimeStep objects with timestamp and formatted labels
     */
    suspend fun generateTimeSteps(
        hoursInPast: Int = 2,
        hoursInFuture: Int = 18,
        intervalHours: Int = 1
    ): List<ForecastTimeStep> = withContext(Dispatchers.Default) {
        val timeSteps = mutableListOf<ForecastTimeStep>()
        val calendar = Calendar.getInstance()
        
        // Reset to the current hour
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Move back to the start time
        calendar.add(Calendar.HOUR_OF_DAY, -hoursInPast)
        
        // Calculate the total number of steps
        val totalSteps = (hoursInPast + hoursInFuture) / intervalHours + 1
        
        // Format for displaying time
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)
        val dateFormatter = SimpleDateFormat("MMM dd", Locale.US)
        
        for (i in 0 until totalSteps) {
            val date = calendar.time
            val timestamp = date.time / 1000 // Convert to seconds
            
            timeSteps.add(
                ForecastTimeStep(
                    timestamp = timestamp,
                    label = timeFormatter.format(date),
                    dateLabel = dateFormatter.format(date)
                )
            )
            
            // Move forward by the interval
            calendar.add(Calendar.HOUR_OF_DAY, intervalHours)
        }
        
        timeSteps
    }
    
    /**
     * Create radar URL for a specific time step
     * 
     * @param layer The radar layer to fetch (e.g., "ndfd.conus.qpf")
     * @param timestamp The timestamp to fetch the radar for
     * @param bbox The bounding box for the radar image
     * @param width The width of the radar image
     * @param height The height of the radar image
     * @return URL for the radar image
     */
    fun createRadarUrlForTime(
        layer: String,
        timestamp: Long,
        bbox: String = "-14200679.12,2500000,-7400000,6505689.94",
        width: Int = 1000,
        height: Int = 600
    ): String {
        // Return cached URL if available
        urlCache[timestamp]?.let { return it }
        
        // Convert timestamp to Date
        val date = Date(timestamp * 1000) // Convert seconds to milliseconds
        
        // Format the date for the VTIT parameter
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val validTime = dateFormat.format(date)
        
        // Build the URL
        val url = "https://digital.weather.gov/ndfd.conus/wms?" +
            "SERVICE=WMS" +
            "&VERSION=1.3.0" +
            "&REQUEST=GetMap" +
            "&LAYERS=$layer" +
            "&FORMAT=image/png" +
            "&TRANSPARENT=TRUE" +
            "&CRS=EPSG:3857" +
            "&WIDTH=$width" +
            "&HEIGHT=$height" +
            "&BBOX=$bbox" +
            "&VTIT=$validTime"
        
        // Cache the URL
        urlCache[timestamp] = url
        
        return url
    }
    
    /**
     * Verify if the radar URL is valid by making a request
     * @return True if the URL is valid, false otherwise
     */
    suspend fun verifyRadarUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "${AppConfig.USER_AGENT} (${AppConfig.CONTACT_EMAIL})")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            val isSuccessful = response.isSuccessful
            if (!isSuccessful) {
                Log.e("ForecastTimelineManager", "Failed to verify radar URL: ${response.code}")
            } else {
                Log.d("ForecastTimelineManager", "Successfully verified radar URL: $url")
            }
            
            response.close()
            isSuccessful
        } catch (e: Exception) {
            Log.e("ForecastTimelineManager", "Error verifying radar URL: ${e.message}")
            false
        }
    }
    
    /**
     * Get a list of preloaded, verified radar URLs for the given time steps and layer
     * This is useful for ensuring all URLs are valid before starting animation
     * 
     * @param layer The radar layer to use
     * @param timeSteps The list of time steps to preload
     * @return Map of timestamp to radar URL for each valid time step
     */
    suspend fun preloadRadarUrls(
        layer: String,
        timeSteps: List<ForecastTimeStep>
    ): Map<Long, String> = withContext(Dispatchers.IO) {
        val validUrls = mutableMapOf<Long, String>()
        
        for (timeStep in timeSteps) {
            val url = createRadarUrlForTime(layer, timeStep.timestamp)
            
            if (verifyRadarUrl(url)) {
                validUrls[timeStep.timestamp] = url
            } else {
                Log.w("ForecastTimelineManager", "Invalid radar URL for time ${timeStep.label}: $url")
            }
        }
        
        // Return only valid URLs
        validUrls
    }
    
    /**
     * Clear the URL cache
     */
    fun clearCache() {
        urlCache.clear()
    }
    
    companion object {
        // Common layer names for convenience
        const val LAYER_PRECIPITATION = "ndfd.conus.qpf"
        const val LAYER_WIND = "ndfd.conus.windspd"
        const val LAYER_TEMPERATURE = "ndfd.conus.maxt"
        
        // Singleton instance
        @Volatile private var instance: ForecastTimelineManager? = null
        
        fun getInstance(): ForecastTimelineManager {
            return instance ?: synchronized(this) {
                instance ?: ForecastTimelineManager().also { instance = it }
            }
        }
    }
}
