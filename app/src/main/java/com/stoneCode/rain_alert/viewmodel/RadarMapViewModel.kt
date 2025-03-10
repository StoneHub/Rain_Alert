package com.stoneCode.rain_alert.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.repository.RadarMapRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for the radar map screen.
 * Handles shared state between both map views (carousel and fullscreen).
 */
class RadarMapViewModel(application: Application) : AndroidViewModel(application) {
    
    private val radarMapRepository = RadarMapRepository()
    private val firebaseLogger = FirebaseLogger.getInstance()
    
    // Debounce mechanism for radar data fetching
    private var fetchRadarJob: Job? = null
    private var lastFetchTime: Long = 0
    private var lastBounds: LatLngBounds? = null
    
    // Constants for debouncing and optimization
    companion object {
        const val FETCH_DEBOUNCE_MS = 1500L  // Debounce time in milliseconds
        const val MIN_FETCH_INTERVAL_MS = 10000L  // Minimum time between fetches
        const val SIGNIFICANT_MOVE_PERCENT = 20  // Percentage of viewport change to trigger fetch
    }
    
    // Map center and zoom
    private val _mapCenter = MutableLiveData<LatLng>(LatLng(40.0, -98.0)) // Default to center of US
    val mapCenter: LiveData<LatLng> = _mapCenter
    
    private val _mapZoom = MutableLiveData<Float>(4f)
    val mapZoom: LiveData<Float> = _mapZoom
    
    // Map bounds for weather overlays
    private val _mapBounds = MutableLiveData<LatLngBounds>()
    val mapBounds: LiveData<LatLngBounds> = _mapBounds
    
    // Track last known user location for persistence between views
    private val _lastKnownLocation = MutableLiveData<LatLng?>()
    val lastKnownLocation: LiveData<LatLng?> = _lastKnownLocation
    
    // Selected stations
    private val _selectedStations = MutableLiveData<List<WeatherStation>>(emptyList())
    val selectedStations: LiveData<List<WeatherStation>> = _selectedStations
    
    // Current weather layer URLs
    private val _precipitationRadarUrl = MutableLiveData<String>()
    val precipitationRadarUrl: LiveData<String> = _precipitationRadarUrl
    
    private val _windRadarUrl = MutableLiveData<String>()
    val windRadarUrl: LiveData<String> = _windRadarUrl
    
    private val _temperatureRadarUrl = MutableLiveData<String>()
    val temperatureRadarUrl: LiveData<String> = _temperatureRadarUrl
    
    // Active layer tracking for mutual exclusivity
    private val _activeLayer = MutableLiveData<WeatherLayer>(WeatherLayer.PRECIPITATION)
    val activeLayer: LiveData<WeatherLayer> = _activeLayer
    
    // For backwards compatibility
    private val _showTemperatureLayer = MutableLiveData<Boolean>(false)
    val showTemperatureLayer: LiveData<Boolean> = _showTemperatureLayer
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error handling
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage
    
    /**
     * Available weather layers
     */
    enum class WeatherLayer {
        NONE,
        PRECIPITATION,
        WIND,
        TEMPERATURE
    }
    
    /**
     * Update the user's last known location
     */
    fun updateLastKnownLocation(location: LatLng?) {
        _lastKnownLocation.value = location
    }
    
    /**
     * Update the selected stations and recalculate map view
     */
    fun updateSelectedStations(stations: List<WeatherStation>) {
        _selectedStations.value = stations
        
        if (stations.isNotEmpty()) {
            val (center, zoom) = radarMapRepository.calculateMapViewForStations(stations)
            _mapCenter.value = center
            _mapZoom.value = zoom
            
            // Fetch radar data for the new center
            fetchRadarData(center)
        }
    }
    
    /**
     * Set the active weather layer (only one can be active at a time)
     */
    fun setActiveLayer(layer: WeatherLayer) {
        _activeLayer.value = layer
        
        // Update showTemperatureLayer for backward compatibility
        _showTemperatureLayer.value = (layer == WeatherLayer.TEMPERATURE)
        
        // Fetch data for the layer if needed
        if (layer == WeatherLayer.TEMPERATURE && _temperatureRadarUrl.value == null) {
            fetchTemperatureData()
        }
    }
    
    /**
     * Toggle a specific weather layer
     */
    fun toggleLayer(layer: WeatherLayer) {
        val currentLayer = _activeLayer.value ?: WeatherLayer.PRECIPITATION
        
        // If the layer is already active, turn it off
        if (currentLayer == layer) {
            _activeLayer.value = WeatherLayer.NONE
            if (layer == WeatherLayer.TEMPERATURE) {
                _showTemperatureLayer.value = false
            }
        } else {
            // Otherwise, make it the active layer
            _activeLayer.value = layer
            
            // Update showTemperatureLayer for backward compatibility
            _showTemperatureLayer.value = (layer == WeatherLayer.TEMPERATURE)
            
            // Fetch data if needed
            if (layer == WeatherLayer.TEMPERATURE && _temperatureRadarUrl.value == null) {
                fetchTemperatureData()
            }
        }
    }
    
    /**
     * Fetch current radar data for all weather layers with debouncing and optimization
     */
    fun fetchRadarData(center: LatLng = _mapCenter.value ?: LatLng(40.0, -98.0)) {
        val currentTime = System.currentTimeMillis()
        val currentBounds = _mapBounds.value
        
        // Skip if we don't have bounds yet
        if (currentBounds == null) {
            Log.d("RadarMapViewModel", "Skipping radar fetch: No bounds available")
            return
        }
        
        // Check if we've moved significantly from the last fetch
        val shouldFetchDueToMovement = lastBounds?.let { prevBounds ->
            // Calculate if we've moved significantly as a percentage of viewport size
            val prevWidth = Math.abs(prevBounds.northeast.longitude - prevBounds.southwest.longitude)
            val prevHeight = Math.abs(prevBounds.northeast.latitude - prevBounds.southwest.latitude)
            val currentWidth = Math.abs(currentBounds.northeast.longitude - currentBounds.southwest.longitude)
            val currentHeight = Math.abs(currentBounds.northeast.latitude - currentBounds.southwest.latitude)
            
            // Calculate center point movement as percentage of viewport
            val prevCenter = LatLng(
                (prevBounds.northeast.latitude + prevBounds.southwest.latitude) / 2,
                (prevBounds.northeast.longitude + prevBounds.southwest.longitude) / 2
            )
            val currentCenter = LatLng(
                (currentBounds.northeast.latitude + currentBounds.southwest.latitude) / 2,
                (currentBounds.northeast.longitude + currentBounds.southwest.longitude) / 2
            )
            
            val latMove = Math.abs(currentCenter.latitude - prevCenter.latitude) / prevHeight * 100
            val lngMove = Math.abs(currentCenter.longitude - prevCenter.longitude) / prevWidth * 100
            
            // Also check for significant zoom change
            val sizeChangePercent = Math.abs(currentWidth * currentHeight - prevWidth * prevHeight) / 
                                  (prevWidth * prevHeight) * 100
            
            val significantMove = Math.max(latMove, lngMove) > SIGNIFICANT_MOVE_PERCENT || 
                                sizeChangePercent > SIGNIFICANT_MOVE_PERCENT
            
            if (significantMove) {
                Log.d("RadarMapViewModel", "Significant map movement detected: lat=$latMove%, lng=$lngMove%, size=$sizeChangePercent%")
            }
            
            significantMove
        } ?: true  // If we don't have previous bounds, we should fetch
        
        // Check if enough time has passed since last fetch
        val timeThresholdPassed = currentTime - lastFetchTime > MIN_FETCH_INTERVAL_MS
        
        // Decide if we should fetch now
        val shouldFetch = shouldFetchDueToMovement && timeThresholdPassed
        
        if (!shouldFetch) {
            Log.d("RadarMapViewModel", "Skipping radar fetch: ${if (!shouldFetchDueToMovement) "No significant movement" else "Time threshold not passed"}")
            return
        }
        
        // Cancel previous job if it's still running
        fetchRadarJob?.cancel()
        
        // Create a new debounced job
        fetchRadarJob = viewModelScope.launch {
            // Apply debounce delay
            delay(FETCH_DEBOUNCE_MS)
            
            Log.d("RadarMapViewModel", "Starting radar data fetch with bounds: $currentBounds")
            _isLoading.value = true
            _errorMessage.value = null
            
            val fetchStartTime = System.currentTimeMillis()
            var fetchSuccess = false
            
            try {
                // Update for tracking
                lastBounds = currentBounds
                lastFetchTime = currentTime
                
                // Fetch precipitation radar
                radarMapRepository.getPrecipitationRadarUrl(currentBounds).fold(
                    onSuccess = { url ->
                        _precipitationRadarUrl.value = url
                        Log.d("RadarMapViewModel", "Successfully loaded precipitation data: $url")
                    },
                    onFailure = { error ->
                        val errorMsg = "Failed to load precipitation data: ${error.message}"
                        _errorMessage.value = errorMsg
                        Log.e("RadarMapViewModel", errorMsg, error)
                    }
                )
                
                // Fetch wind radar
                radarMapRepository.getWindRadarUrl(currentBounds).fold(
                    onSuccess = { url ->
                        _windRadarUrl.value = url
                        Log.d("RadarMapViewModel", "Successfully loaded wind data: $url")
                    },
                    onFailure = { error ->
                        val errorMsg = "Failed to load wind data: ${error.message}"
                        // Don't override previous error message if there was one
                        if (_errorMessage.value == null) {
                            _errorMessage.value = errorMsg
                        }
                        Log.e("RadarMapViewModel", errorMsg, error)
                    }
                )
                
                // Fetch temperature data
                radarMapRepository.getTemperatureRadarUrl(currentBounds).fold(
                    onSuccess = { url ->
                        _temperatureRadarUrl.value = url
                        Log.d("RadarMapViewModel", "Successfully loaded temperature data: $url")
                    },
                    onFailure = { error ->
                        val errorMsg = "Error fetching temperature data: ${error.message}"
                        Log.e("RadarMapViewModel", errorMsg, error)
                        // Don't override previous error messages
                    }
                )
                
                fetchSuccess = true
            } catch (e: Exception) {
                _errorMessage.value = "Error loading radar data: ${e.message}"
                Log.e("RadarMapViewModel", "Error loading radar data", e)
            } finally {
                _isLoading.value = false
                val fetchTime = System.currentTimeMillis() - fetchStartTime
                
                // Log to Firebase for analysis
                firebaseLogger.logApiStatus(
                    isSuccess = fetchSuccess,
                    endpoint = "radar_data",
                    responseTime = fetchTime,
                    errorMessage = _errorMessage.value
                )
                
                Log.d("RadarMapViewModel", "Radar data fetch completed in ${fetchTime}ms")
            }
        }
    }
    
    /**
     * Update map camera position including bounds
     */
    fun updateMapCamera(center: LatLng, zoom: Float, bounds: LatLngBounds? = null) {
        _mapCenter.value = center
        _mapZoom.value = zoom
        bounds?.let { _mapBounds.value = it }
    }
    
    /**
     * Update map center
     */
    fun updateMapCenter(center: LatLng) {
        _mapCenter.value = center
    }
    
    /**
     * Update map zoom level
     */
    fun updateMapZoom(zoom: Float) {
        _mapZoom.value = zoom
    }
    
    /**
     * Fetch temperature data specifically
     */
    private fun fetchTemperatureData() {
        viewModelScope.launch {
            try {
                val currentBounds = _mapBounds.value
                
                radarMapRepository.getTemperatureRadarUrl(currentBounds).fold(
                    onSuccess = { url ->
                        _temperatureRadarUrl.value = url
                        Log.d("RadarMapViewModel", "Loaded temperature data: $url")
                    },
                    onFailure = { error ->
                        Log.e("RadarMapViewModel", "Error fetching temperature data: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("RadarMapViewModel", "Error loading temperature data", e)
            }
        }
    }
    
    /**
     * Refresh all weather data
     */
    fun refreshRadarData() {
        _mapCenter.value?.let { center ->
            fetchRadarData(center)
        }
    }
}