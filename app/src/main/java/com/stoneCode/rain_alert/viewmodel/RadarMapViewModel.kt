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
 * Simplified to focus on current weather data without forecast features.
 */
class RadarMapViewModel(application: Application) : AndroidViewModel(application) {
    
    private val radarMapRepository = RadarMapRepository(application)
    
    // Map center and zoom
    private val _mapCenter = MutableLiveData<LatLng>(LatLng(40.0, -98.0)) // Default to center of US
    val mapCenter: LiveData<LatLng> = _mapCenter
    
    private val _mapZoom = MutableLiveData<Float>(4f)
    val mapZoom: LiveData<Float> = _mapZoom
    
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
    
    private val _showTemperatureLayer = MutableLiveData<Boolean>(false)
    val showTemperatureLayer: LiveData<Boolean> = _showTemperatureLayer
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error handling
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage
    
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
     */
    fun toggleTemperatureLayer() {
        val currentValue = _showTemperatureLayer.value ?: false
        _showTemperatureLayer.value = !currentValue
        
        // Fetch temperature data if it's now enabled but we don't have data yet
        if (!currentValue && _temperatureRadarUrl.value == null) {
            fetchTemperatureData()
        }
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