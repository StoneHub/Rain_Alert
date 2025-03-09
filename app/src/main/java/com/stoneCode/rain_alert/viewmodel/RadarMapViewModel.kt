package com.stoneCode.rain_alert.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.repository.RadarMapRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the radar map screen.
 * Handles shared state between both map views (carousel and fullscreen).
 */
class RadarMapViewModel(application: Application) : AndroidViewModel(application) {
    
    private val radarMapRepository = RadarMapRepository(application)
    
    // Map center and zoom
    private val _mapCenter = MutableLiveData<LatLng>(LatLng(40.0, -98.0)) // Default to center of US
    val mapCenter: LiveData<LatLng> = _mapCenter
    
    private val _mapZoom = MutableLiveData<Float>(4f)
    val mapZoom: LiveData<Float> = _mapZoom
    
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
     * Check if a specific layer is active
     */
    fun isLayerActive(layer: WeatherLayer): Boolean {
        return _activeLayer.value == layer
    }
    
    /**
     * Fetch current radar data for all weather layers
     */
    fun fetchRadarData(center: LatLng = _mapCenter.value ?: LatLng(40.0, -98.0)) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Fetch precipitation radar
                radarMapRepository.getPrecipitationRadarUrl(center).fold(
                    onSuccess = { url ->
                        _precipitationRadarUrl.value = url
                    },
                    onFailure = { error ->
                        _errorMessage.value = "Failed to load precipitation data: ${error.message}"
                    }
                )
                
                // Fetch wind radar
                radarMapRepository.getWindRadarUrl(center).fold(
                    onSuccess = { url ->
                        _windRadarUrl.value = url
                    },
                    onFailure = { error ->
                        // Don't override previous error message if there was one
                        if (_errorMessage.value == null) {
                            _errorMessage.value = "Failed to load wind data: ${error.message}"
                        }
                    }
                )
                
                // Fetch temperature data
                radarMapRepository.getTemperatureRadarUrl(center).fold(
                    onSuccess = { url ->
                        _temperatureRadarUrl.value = url
                        Log.d("RadarMapViewModel", "Loaded temperature data: $url")
                    },
                    onFailure = { error ->
                        Log.e("RadarMapViewModel", "Error fetching temperature data: ${error.message}")
                        // Don't override previous error messages
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error loading radar data: ${e.message}"
                Log.e("RadarMapViewModel", "Error loading radar data", e)
            } finally {
                _isLoading.value = false
            }
        }
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
     * Toggle temperature layer visibility
     * @deprecated Use toggleLayer(WeatherLayer.TEMPERATURE) instead
     */
    fun toggleTemperatureLayer() {
        toggleLayer(WeatherLayer.TEMPERATURE)
    }
    
    /**
     * Fetch temperature data specifically
     */
    private fun fetchTemperatureData() {
        viewModelScope.launch {
            try {
                val center = _mapCenter.value ?: LatLng(40.0, -98.0)
                
                radarMapRepository.getTemperatureRadarUrl(center).fold(
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