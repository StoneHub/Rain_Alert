package com.stoneCode.rain_alert.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.repository.RadarMapRepository
import com.stoneCode.rain_alert.ui.map.ForecastTimeStep
import com.stoneCode.rain_alert.ui.map.ForecastTimelineManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RadarMapViewModel(application: Application) : AndroidViewModel(application) {
    
    // Timeline manager for forecast animations
    private val forecastTimelineManager = ForecastTimelineManager.getInstance()
    
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
    
    // Forecast animation properties
    private val _forecastTimeSteps = MutableLiveData<List<ForecastTimeStep>>(emptyList())
    val forecastTimeSteps: LiveData<List<ForecastTimeStep>> = _forecastTimeSteps
    
    private val _currentTimeIndex = MutableLiveData<Int>(0)
    val currentTimeIndex: LiveData<Int> = _currentTimeIndex
    
    private val _isAnimationPlaying = MutableLiveData<Boolean>(false)
    val isAnimationPlaying: LiveData<Boolean> = _isAnimationPlaying
    
    private val _currentAnimationRadarUrl = MutableLiveData<String>()
    val currentAnimationRadarUrl: LiveData<String> = _currentAnimationRadarUrl
    
    private val _forecastAnimationEnabled = MutableLiveData<Boolean>(false)
    val forecastAnimationEnabled: LiveData<Boolean> = _forecastAnimationEnabled
    
    private val _forecastAnimationLayer = MutableLiveData<String>(ForecastTimelineManager.LAYER_PRECIPITATION)
    val forecastAnimationLayer: LiveData<String> = _forecastAnimationLayer
    
    private var animationJob: Job? = null
    
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
    
    /**
     * Initialize forecast animation timeline
     * @param layer The radar layer to animate
     * @param hoursInPast Number of hours in the past to include
     * @param hoursInFuture Number of hours in the future to include
     * @param intervalHours Interval between time steps in hours
     */
    fun initForecastAnimation(
        layer: String = ForecastTimelineManager.LAYER_PRECIPITATION,
        hoursInPast: Int = 2,
        hoursInFuture: Int = 18,
        intervalHours: Int = 1
    ) {
        // Stop any existing animation
        stopAnimation()
        
        // Set the animation layer
        _forecastAnimationLayer.value = layer
        Log.d("RadarMapViewModel", "Initializing forecast animation with layer: $layer")
        
        // Generate time steps asynchronously
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Generate time steps
                val timeSteps = forecastTimelineManager.generateTimeSteps(
                    hoursInPast = hoursInPast,
                    hoursInFuture = hoursInFuture,
                    intervalHours = intervalHours
                )
                
                // Preload the first few URLs
                val initialTimeSteps = timeSteps.take(5)
                val preloadedUrls = forecastTimelineManager.preloadRadarUrls(layer, initialTimeSteps)
                
                if (preloadedUrls.isNotEmpty()) {
                    _forecastTimeSteps.value = timeSteps
                    _currentTimeIndex.value = 0
                    _forecastAnimationEnabled.value = true
                    
                    // Set the current URL to the first time step
                    if (timeSteps.isNotEmpty()) {
                        updateCurrentFrameUrl()
                    }
                } else {
                    _errorMessage.value = "Unable to load forecast animation data"
                    _forecastAnimationEnabled.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error initializing forecast animation: ${e.message}"
                _forecastAnimationEnabled.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update the current time index and URL for the animation
     */
    fun updateCurrentTimeIndex(index: Int) {
        val timeSteps = _forecastTimeSteps.value ?: return
        if (index < 0 || index >= timeSteps.size) return
        
        _currentTimeIndex.value = index
        updateCurrentFrameUrl()
    }
    
    /**
     * Toggle play/pause for the animation
     */
    fun toggleAnimation() {
        val isPlaying = _isAnimationPlaying.value ?: false
        if (isPlaying) {
            stopAnimation()
        } else {
            startAnimation()
        }
    }
    
    /**
     * Start the animation
     */
    fun startAnimation() {
        val timeSteps = _forecastTimeSteps.value ?: return
        if (timeSteps.isEmpty()) return
        
        // Cancel any existing animation job
        animationJob?.cancel()
        
        // Set playing state immediately for UI response
        _isAnimationPlaying.value = true
        
        // Start a new animation job
        animationJob = viewModelScope.launch {
            // Preload all URLs in the background first to avoid jumpy animation
            _isLoading.value = true
            try {
                // Preload all frames
                val layer = _forecastAnimationLayer.value ?: ForecastTimelineManager.LAYER_PRECIPITATION
                val preloadedUrls = forecastTimelineManager.preloadRadarUrls(layer, timeSteps)
                
                // Only continue with animation if we have URLs
                if (preloadedUrls.isNotEmpty()) {
                    _isLoading.value = false
                    
                    // Animation loop - use delay to control frame rate
                    while (isActive) {
                        // Get the next index, wrapping around to 0 if at the end
                        val currentIndex = _currentTimeIndex.value ?: 0
                        val nextIndex = (currentIndex + 1) % timeSteps.size
                        
                        // Update the current index
                        _currentTimeIndex.value = nextIndex
                        updateCurrentFrameUrl()
                        
                        // Delay before showing the next frame
                        delay(750) // 750ms between frames - slowed down for smoother appearance
                    }
                } else {
                    _errorMessage.value = "Unable to load forecast animation frames"
                    stopAnimation()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error preloading animation frames: ${e.message}"
                stopAnimation()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Stop the animation
     */
    fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
        _isAnimationPlaying.value = false
    }
    
    /**
     * Update the current frame URL based on the current time index
     */
    private fun updateCurrentFrameUrl() {
        val timeSteps = _forecastTimeSteps.value ?: return
        val currentIndex = _currentTimeIndex.value ?: 0
        if (currentIndex < 0 || currentIndex >= timeSteps.size) return
        
        val layer = _forecastAnimationLayer.value ?: ForecastTimelineManager.LAYER_PRECIPITATION
        val timestamp = timeSteps[currentIndex].timestamp
        
        try {
            val url = forecastTimelineManager.createRadarUrlForTime(layer, timestamp)
            _currentAnimationRadarUrl.value = url
            Log.d("RadarMapViewModel", "Animation frame updated: $url")
        } catch (e: Exception) {
            Log.e("RadarMapViewModel", "Error updating animation frame: ${e.message}")
        }
    }
    
    /**
     * Change the animation layer
     */
    fun changeAnimationLayer(layer: String) {
        Log.d("RadarMapViewModel", "Changing animation layer to: $layer")
        
        if (_forecastAnimationLayer.value == layer) return
        
        // Stop the animation and reinitialize with the new layer
        stopAnimation()
        _forecastAnimationLayer.value = layer
        _forecastAnimationEnabled.value = false
        _forecastTimeSteps.value = emptyList()
        
        // Reinitialize the animation with the new layer
        initForecastAnimation(layer)
    }
    
    /**
     * Toggle forecast animation mode
     */
    fun toggleForecastAnimation() {
        val isEnabled = _forecastAnimationEnabled.value ?: false
        
        if (isEnabled) {
            // Disable animation mode
            stopAnimation()
            _forecastAnimationEnabled.value = false
            _forecastTimeSteps.value = emptyList()
        } else {
            // Enable animation mode
            initForecastAnimation()
        }
    }
    
    /**
     * Clean up resources when the ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        stopAnimation()
        forecastTimelineManager.clearCache()
    }
}