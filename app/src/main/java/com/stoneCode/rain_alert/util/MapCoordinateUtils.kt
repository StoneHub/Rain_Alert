package com.stoneCode.rain_alert.util

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tan

/**
 * Utility functions for working with map coordinates and transformations.
 * Provides conversion between different coordinate systems used by Google Maps and WMS services.
 */
object MapCoordinateUtils {
    /**
     * Converts from WGS84 (GPS) coordinates to EPSG:3857 (Web Mercator)
     */
    fun latLngToEPSG3857(latLng: LatLng): Pair<Double, Double> {
        val x = latLng.longitude * 20037508.34 / 180.0
        
        // Convert latitude to y coordinate
        var y = ln(tan((90.0 + latLng.latitude) * PI / 360.0)) / (PI / 180.0)
        y = y * 20037508.34 / 180.0
        
        return Pair(x, y)
    }
    
    /**
     * Converts from EPSG:3857 (Web Mercator) to WGS84 (GPS) coordinates
     */
    fun epsg3857ToLatLng(x: Double, y: Double): LatLng {
        val lon = x * 180.0 / 20037508.34
        var lat = y * 180.0 / 20037508.34
        lat = 180.0 / PI * (2.0 * kotlin.math.atan(kotlin.math.exp(lat * PI / 180.0)) - PI / 2.0)
        return LatLng(lat, lon)
    }
    
    /**
     * Creates a bbox parameter string in EPSG:3857 format for WMS requests
     * from a LatLngBounds object
     */
    fun latLngBoundsToBbox(bounds: LatLngBounds): String {
        val southwest = bounds.southwest
        val northeast = bounds.northeast
        
        val (swX, swY) = latLngToEPSG3857(southwest)
        val (neX, neY) = latLngToEPSG3857(northeast)
        
        // WMS 1.3.0 uses the format: minX,minY,maxX,maxY
        return "$swX,$swY,$neX,$neY"
    }
    
    /**
     * Converts a WMS bbox string to a LatLngBounds object
     */
    fun bboxToLatLngBounds(bbox: String): LatLngBounds {
        val parts = bbox.split(",").map { it.toDouble() }
        if (parts.size != 4) {
            throw IllegalArgumentException("Invalid bbox string. Expected format: minX,minY,maxX,maxY")
        }
        
        val (minX, minY, maxX, maxY) = parts
        
        val southwest = epsg3857ToLatLng(minX, minY)
        val northeast = epsg3857ToLatLng(maxX, maxY)
        
        return LatLngBounds.builder()
            .include(southwest)
            .include(northeast)
            .build()
    }
    
    /**
     * Creates a reasonable bbox string for the continental United States
     * Used as a fallback when no specific bounds are available
     */
    fun getDefaultUsBbox(): String {
        // These bounds approximately cover the continental United States
        return "-14200679.12,2500000,-7400000,6505689.94"
    }
    
}