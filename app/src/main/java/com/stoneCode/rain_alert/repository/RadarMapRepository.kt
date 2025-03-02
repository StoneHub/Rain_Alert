package com.stoneCode.rain_alert.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.data.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Repository for fetching radar map data from Weather.gov API
 */
class RadarMapRepository(private val context: Context) {
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Get precipitation radar tile URL for a specific area
     * Note: Weather.gov API provides radar data as WMS tiles
     */
    suspend fun getPrecipitationRadarUrl(center: LatLng): Result<String> = withContext(Dispatchers.IO) {
        try {
            // National Weather Service radar endpoint for reflectivity
            // Documentation: https://www.weather.gov/documentation/services-web-api
            // We're using the standard reflectivity product which shows precipitation
            
            // Update to use the NWS base reflectivity product for the Continental US
            // This provides a nationwide view of precipitation
            val radarUrl = "https://radar.weather.gov/ridge/standard/CONUS_BREF_LIT.png"
            
            // Verify the URL works by making a request
            val request = Request.Builder()
                .url(radarUrl)
                .header("User-Agent", "${AppConfig.USER_AGENT} (${AppConfig.CONTACT_EMAIL})")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e("RadarMapRepository", "Failed to verify radar URL: ${response.code}")
                return@withContext Result.failure(Exception("Failed to get radar data: ${response.code}"))
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
    suspend fun getWindRadarUrl(center: LatLng): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Use wind velocity product from NWS for the Continental US
            // This provides a nationwide view of wind patterns
            val windUrl = "https://radar.weather.gov/ridge/standard/CONUS_VELO_LIT.png"
            
            // Verify the URL works
            val request = Request.Builder()
                .url(windUrl)
                .header("User-Agent", "${AppConfig.USER_AGENT} (${AppConfig.CONTACT_EMAIL})")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e("RadarMapRepository", "Failed to verify wind URL: ${response.code}")
                return@withContext Result.failure(Exception("Failed to get wind data: ${response.code}"))
            }
            
            Result.success(windUrl)
        } catch (e: Exception) {
            Log.e("RadarMapRepository", "Error fetching wind data", e)
            Result.failure(e)
        }
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