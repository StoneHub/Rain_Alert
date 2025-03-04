package com.stoneCode.rain_alert.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.repository.RadarMapRepository
import kotlinx.coroutines.launch

class RadarMapViewModel(application: Application) : AndroidViewModel(application) {
    
    // Reference to GoogleMap instance for overlays
    var googleMapInstance: GoogleMap? = null
        private set
    
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
    
    // Temperature layer - enabled by default
    private val _showTemperatureLayer = MutableLiveData<Boolean>(true)
    val showTemperatureLayer: LiveData<Boolean> = _showTemperatureLayer
    
    private val _temperatureRadarUrl = MutableLiveData<String>()
    val temperatureRadarUrl: LiveData<String> = _temperatureRadarUrl
    
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
                
                // Also fetch temperature data to have it ready
                fetchTemperatureData()
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
    
    /**
     * Set the GoogleMap instance for use with overlays
     */
    fun setGoogleMapInstance(map: GoogleMap) {
        googleMapInstance = map
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
     * Fetch temperature radar data
     * This function is now public and can be called directly or via toggleTemperatureLayer()
     */
    fun fetchTemperatureData() {
        viewModelScope.launch {
            // Don't set loading to true here if we're just pre-loading the data
            // _isLoading.value = true
            try {
                // Get temperature data from the repository instead of hardcoding it here
                // This ensures we use the same bbox and parameters as other layers
                val center = _mapCenter.value ?: LatLng(40.0, -98.0)
                
                // Use the repository method for consistent handling
                radarMapRepository.getTemperatureRadarUrl(center).fold(
                    onSuccess = { url ->
                        _temperatureRadarUrl.value = url
                        android.util.Log.d("RadarMapViewModel", "Loading temperature bitmap from URL: $url")
                    },
                    onFailure = { error ->
                        android.util.Log.e("RadarMapViewModel", "Error fetching temperature data: ${error.message}")
                        // Don't set error message if we're just preloading
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("RadarMapViewModel", "Error loading temperature data", e)
                // Don't set error message here if we're just pre-loading the data
                // _errorMessage.value = "Error loading temperature data: ${e.message}"
            } finally {
                // Don't set loading to false here if we're just pre-loading the data
                // _isLoading.value = false
            }
        }
    }
    
    /**
     * Helper method to get current time formatted for WMS requests
     */
    private fun getCurrentFormattedTime(): String {
        val calendar = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:00", java.util.Locale.US)
        return dateFormat.format(calendar.time)
    }
}