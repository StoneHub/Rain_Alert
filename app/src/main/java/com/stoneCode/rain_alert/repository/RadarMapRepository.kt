package com.stoneCode.rain_alert.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.data.AppConfig
import com.stoneCode.rain_alert.util.MapCoordinateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Repository for fetching radar map data from Weather.gov API
 */
class RadarMapRepository {
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Get precipitation radar tile URL for a specific area
     * Note: Weather.gov API provides radar data as WMS tiles
     */
    suspend fun getPrecipitationRadarUrl(mapBounds: LatLngBounds? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Use the NWS WMS service for precipitation data (Quantitative Precipitation Forecast)
            // See: https://digital.weather.gov/ndfd.conus/wms?REQUEST=GetCapabilities
            
            // Use our helper function to create the WMS URL
            val radarUrl = createWmsUrl("ndfd.conus.qpf", mapBounds) // Quantitative Precipitation Forecast
            
            Log.d("RadarMapRepository", "Generated WMS URL: $radarUrl")
            
            // Verify the URL works by making a request
            val request = Request.Builder()
                .url(radarUrl)
                .header("User-Agent", "${AppConfig.USER_AGENT} (${AppConfig.CONTACT_EMAIL})")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e("RadarMapRepository", "Failed to verify radar URL: ${response.code}")
                return@withContext Result.failure(Exception("Failed to get radar data: ${response.code} - ${response.message}"))
            }
            
            Result.success(radarUrl)
        } catch (e: Exception) {
            Log.e("RadarMapRepository", "Error fetching radar data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get wind speed radar tile URL for a specific area
     */
    suspend fun getWindRadarUrl(mapBounds: LatLngBounds? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Use the NWS WMS service for wind data
            // See: https://digital.weather.gov/ndfd.conus/wms?REQUEST=GetCapabilities
            
            // Use our helper function to create the WMS URL
            val windUrl = createWmsUrl("ndfd.conus.windspd", mapBounds) // Wind Speed layer
            
            Log.d("RadarMapRepository", "Generated Wind WMS URL: $windUrl")
            
            // Verify the URL works
            val request = Request.Builder()
                .url(windUrl)
                .header("User-Agent", "${AppConfig.USER_AGENT} (${AppConfig.CONTACT_EMAIL})")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e("RadarMapRepository", "Failed to verify wind URL: ${response.code}")
                return@withContext Result.failure(Exception("Failed to get wind data: ${response.code} - ${response.message}"))
            }
            
            Result.success(windUrl)
        } catch (e: Exception) {
            Log.e("RadarMapRepository", "Error fetching wind data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get temperature radar tile URL for a specific area
     */
    suspend fun getTemperatureRadarUrl(mapBounds: LatLngBounds? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Format the current date for the VTIT parameter
            val calendar = java.util.Calendar.getInstance()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'00:00", java.util.Locale.US)
            val validTime = dateFormat.format(calendar.time)
            
            // Calculate bbox from the current map view or use a default
            val bbox = if (mapBounds != null) {
                MapCoordinateUtils.latLngBoundsToBbox(mapBounds)
            } else {
                MapCoordinateUtils.getDefaultUsBbox()
            }
            
            // Create temperature-specific URL using the working format
            val temperatureUrl = "https://digital.weather.gov/ndfd/wms?" +
                "LAYERS=ndfd.conus.maxt" +
                "&FORMAT=image/png" +
                "&TRANSPARENT=TRUE" +
                "&SEASON=0" +
                "&VERSION=1.3.0" +
                "&VTIT=$validTime" +
                "&EXCEPTIONS=INIMAGE" +
                "&SERVICE=WMS" +
                "&REQUEST=GetMap" +
                "&STYLES=" +
                "&CRS=EPSG:3857" +
                "&WIDTH=1000" +
                "&HEIGHT=600" +
                "&BBOX=$bbox"
            
            Log.d("RadarMapRepository", "Generated Temperature WMS URL with BBOX: $bbox")
            
            // Verify the URL works by making a request
            val request = Request.Builder()
                .url(temperatureUrl)
                .header("User-Agent", "${AppConfig.USER_AGENT} (${AppConfig.CONTACT_EMAIL})")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e("RadarMapRepository", "Failed to verify temperature URL: ${response.code}")
                return@withContext Result.failure(Exception("Failed to get temperature data: ${response.code} - ${response.message}"))
            }
            
            Result.success(temperatureUrl)
        } catch (e: Exception) {
            Log.e("RadarMapRepository", "Error fetching temperature data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate a WMS URL for the specified layer and parameters
     * Now using dynamic bbox based on current map view
     */
    fun createWmsUrl(
        layer: String,
        mapBounds: LatLngBounds? = null,
        width: Int = 1000,
        height: Int = 600
    ): String {
        // Format the current date for the VTIT parameter
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.US)
        val validTime = dateFormat.format(calendar.time)
        
        // Check if this is a precipitation layer request
        val isPrecipitationLayer = layer.contains("qpf")
        
        // Calculate bbox from the current map view or use a default
        val bbox = if (mapBounds != null) {
            MapCoordinateUtils.latLngBoundsToBbox(mapBounds)
        } else if (isPrecipitationLayer) {
            // Use Southeast-specific bbox for precipitation to focus better on SC
            MapCoordinateUtils.getSoutheastUsBbox()
        } else {
            MapCoordinateUtils.getDefaultUsBbox()
        }
        
        Log.d("RadarMapRepository", "Using BBOX: $bbox for layer: $layer")
        
        return "https://digital.weather.gov/ndfd.conus/wms?" +
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
    }
    
    /**
     * Calculate the center point and zoom level for a set of weather stations
     */
    fun calculateMapViewForStations(stations: List<WeatherStation>): Pair<LatLng, Float> {
        // If no stations, default to center of US
        if (stations.isEmpty()) {
            return Pair(LatLng(40.0, -98.0), 4f)
        }
        
        // If single station, center on it with default zoom
        if (stations.size == 1) {
            return Pair(
                LatLng(stations[0].latitude, stations[0].longitude),
                8f
            )
        }
        
        // For multiple stations, calculate the bounding box
        var minLat = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = Double.MIN_VALUE
        
        stations.forEach { station ->
            minLat = minOf(minLat, station.latitude)
            maxLat = maxOf(maxLat, station.latitude)
            minLng = minOf(minLng, station.longitude)
            maxLng = maxOf(maxLng, station.longitude)
        }
        
        // Calculate center
        val centerLat = (minLat + maxLat) / 2
        val centerLng = (minLng + maxLng) / 2
        
        // Calculate appropriate zoom level
        // Simple formula: based on the largest dimension of the bounding box
        val latSpan = maxLat - minLat
        val lngSpan = maxLng - minLng
        val maxSpan = maxOf(latSpan, lngSpan)
        
        // Map span to zoom level (approximate)
        val zoom = when {
            maxSpan > 10.0 -> 4f
            maxSpan > 5.0 -> 6f
            maxSpan > 2.0 -> 7f
            maxSpan > 1.0 -> 8f
            maxSpan > 0.5 -> 9f
            else -> 10f
        }
        
        return Pair(LatLng(centerLat, centerLng), zoom)
    }
}