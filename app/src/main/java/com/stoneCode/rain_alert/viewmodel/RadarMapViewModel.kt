package com.stoneCode.rain_alert.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.repository.RadarMapRepository
import kotlinx.coroutines.launch

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
    
    // Radar URLs
    private val _precipitationRadarUrl = MutableLiveData<String>()
    val precipitationRadarUrl: LiveData<String> = _precipitationRadarUrl
    
    private val _windRadarUrl = MutableLiveData<String>()
    val windRadarUrl: LiveData<String> = _windRadarUrl
    
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
     * Fetch radar data for precipitation and wind
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
            } catch (e: Exception) {
                _errorMessage.value = "Error loading radar data: ${e.message}"
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
     * Refresh radar data
     */
    fun refreshRadarData() {
        _mapCenter.value?.let { center ->
            fetchRadarData(center)
        }
    }
    
//    /**
//     * Get the application context
//     */
//    fun getApplication(): Application {
//        return getApplication<Application>()
//    }
}