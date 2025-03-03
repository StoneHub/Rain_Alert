package com.stoneCode.rain_alert.viewmodel

import android.app.Application
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stoneCode.rain_alert.api.WeatherStation
import com.stoneCode.rain_alert.api.WeatherStationFinder
import com.stoneCode.rain_alert.data.StationObservation
import com.stoneCode.rain_alert.data.UserPreferences
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.repository.AlertHistoryRepository
import com.stoneCode.rain_alert.repository.WeatherRepository
import com.stoneCode.rain_alert.service.RainService
import com.stoneCode.rain_alert.service.ServiceStatusListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherViewModel(application: Application) : AndroidViewModel(application),
    ServiceStatusListener {

    val isServiceRunning = MutableLiveData(false)
    val lastUpdateTime = MutableLiveData("")
    val weatherData = MutableLiveData("Loading...")
    private val weatherRepository = WeatherRepository(application.applicationContext)
    private val alertHistoryRepository = AlertHistoryRepository(application.applicationContext)
    private val firebaseLogger = FirebaseLogger.getInstance()
    private val weatherStationFinder = WeatherStationFinder()
    
    // User preferences
    private val userPreferences = UserPreferences(application.applicationContext)
    
    // Custom location and selected stations
    val customLocationZip = MutableLiveData<String?>(null)
    val useCustomLocation = MutableLiveData(false)
    val selectedStationIds = MutableLiveData<List<String>>(emptyList())
    val availableStations = MutableLiveData<List<WeatherStation>>(emptyList())

    // Station data for display
    val stationData = MutableLiveData<List<StationObservation>>(emptyList())
    
    private var serviceCheckJob: Job? = null
    
    // Current location for display
    val currentLocation = MutableLiveData<String>(null)
    
    // Raw API data for debugging
    val rawApiData = MutableLiveData<String>(null)
    
    // Alert history as LiveData
    val alertHistory = alertHistoryRepository.getAlertHistory()
        .distinctUntilChanged()
        .asLiveData()

    // Data loading status
    val isDataReady = MutableLiveData(false)
    
    init {
        // Load user preferences
        viewModelScope.launch {
            // Load location preferences
            customLocationZip.value = userPreferences.customLocationZip.first()
            useCustomLocation.value = userPreferences.useCustomLocation.first()
            
            // Load selected stations
            val savedStationIds = userPreferences.selectedStationIds.first().toList()
            selectedStationIds.value = savedStationIds
            
            // Make initial weather update after loading preferences
            updateWeatherStatus()
            
            // If no stations are selected, automatically select the 3 closest ones
            if (savedStationIds.isEmpty()) {
                selectDefaultStations()
            }
        }
    }

    private fun setIsDataReady(ready: Boolean) {
        isDataReady.postValue(ready)
    }

    // Called when the service status changes
    override fun onServiceStatusChanged(isRunning: Boolean) {
        isServiceRunning.postValue(isRunning)
    }

    // Register as a listener with RainService
    fun registerServiceStatusListener() {
        RainService.setServiceStatusListener(this)
    }

    // Unregister as a listener
    fun unregisterServiceStatusListener() {
        RainService.clearServiceStatusListener()
    }

    fun updateWeatherStatus() {
        Log.d("WeatherViewModel", "Updating weather status")
        setIsDataReady(false) // Set to false when starting to fetch data

        viewModelScope.launch {
            // Fetch available stations for selection
            fetchAvailableStations()
            val startTime = System.currentTimeMillis()
            delay(500) // Introduce a small delay

            // Check if the service is running and update the LiveData
            val serviceRunning = isRainServiceRunning()
            isServiceRunning.postValue(serviceRunning)
            Log.d("WeatherViewModel", "Service running status: $serviceRunning")

            // Update the last update time
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            lastUpdateTime.postValue(currentTime)
            Log.d("WeatherViewModel", "Last update time: $currentTime")

            try {
                // Get location information for display
                val location = weatherRepository.getLastKnownLocation()
                if (location != null) {
                    val geocoder = android.location.Geocoder(getApplication())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val locationString = if (address.postalCode != null) {
                            "${address.locality ?: ""}, ${address.adminArea ?: ""} ${address.postalCode}"
                        } else {
                            "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}"
                        }
                        currentLocation.postValue(locationString)
                    } else {
                        currentLocation.postValue("Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}")
                    }
                }
                
                // Fetch real weather data from the repository
                val currentWeather = weatherRepository.getCurrentWeather()
                weatherData.postValue(currentWeather)
                Log.d("WeatherViewModel", "Weather data updated: $currentWeather")
                
                // Update station data
                stationData.postValue(weatherRepository.getCurrentStations())
                
                // Get raw API data for debugging
                val rawData = weatherRepository.getRawApiResponse()
                rawApiData.postValue(rawData!!)
                
                // Log API success to Firebase
                firebaseLogger.logApiStatus(isSuccess = true, endpoint = "getCurrentWeather")
                
                setIsDataReady(true) // Set to true when data is fetched
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather data", e)
                
                // Log API failure to Firebase
                firebaseLogger.logApiStatus(
                    isSuccess = false, 
                    errorMessage = e.message, 
                    endpoint = "getCurrentWeather"
                )
                
                weatherData.postValue("Error fetching weather data: ${e.message}")
                setIsDataReady(true) // Set to true even on error to continue UI flow
            }
        }
    }

    private fun isRainServiceRunning(): Boolean {
        // Check if the service is running
        return RainService.isRunning
    }

    fun stopServiceChecker() {
        serviceCheckJob?.cancel()
        serviceCheckJob = null
    }
    
    /**
     * Refresh weather data manually
     */
    fun refreshWeatherData() {
        viewModelScope.launch {
            updateWeatherStatus()
        }
    }
    
    /**
     * Clear alert history
     */
    fun clearAlertHistory() {
        viewModelScope.launch {
            alertHistoryRepository.clearHistory()
        }
    }
    
    /**
     * Update custom location settings
     */
    fun updateCustomLocation(zipCode: String?, useCustomLocation: Boolean) {
        viewModelScope.launch {
            customLocationZip.value = zipCode
            this@WeatherViewModel.useCustomLocation.value = useCustomLocation
            
            userPreferences.updateCustomLocationZip(zipCode)
            userPreferences.updateUseCustomLocation(useCustomLocation)
            
            // Refresh weather data with new location
            refreshWeatherData()
        }
    }
    
    /**
     * Update selected station IDs
     */
    fun updateSelectedStations(stationIds: List<String>) {
        viewModelScope.launch {
            Log.d("WeatherViewModel", "Updating selected stations: $stationIds")
            
            // Update the internal value first
            selectedStationIds.value = stationIds
            
            // Save to preferences
            userPreferences.updateSelectedStationIds(stationIds.toSet())
            
            // Force reload station data from weather repository
            val updatedStations = weatherRepository.refreshStationData(stationIds)
            
            // Update UI with new stations
            stationData.postValue(updatedStations)
            Log.d("WeatherViewModel", "Updated station data with ${updatedStations.size} stations")
            
            // Refresh all weather data to ensure everything is in sync
            refreshWeatherData()
        }
    }
    
    /**
     * Fetch available stations around the current location
     * This is public so it can be explicitly called when needed
     */
    suspend fun fetchAvailableStations() {
        try {
            // Clear the loading state
            Log.d("WeatherViewModel", "Fetching available stations for the current location")
            
            val location = weatherRepository.getCurrentLocation() ?: return
            val stationsResult = weatherStationFinder.findNearestStations(
                latitude = location.latitude,
                longitude = location.longitude,
                limit = 10 // Fetch 10 stations for selection
            )
            
            if (stationsResult.isSuccess) {
                val stations = stationsResult.getOrNull() ?: emptyList()
                availableStations.postValue(stations)
                Log.d("WeatherViewModel", "Fetched ${stations.size} available stations for selection")
            } else {
                Log.e("WeatherViewModel", "Failed to fetch stations: ${stationsResult.exceptionOrNull()?.message}")
                availableStations.postValue(emptyList())
            }
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error fetching available stations", e)
            availableStations.postValue(emptyList())
        }
    }
    
    /**
     * Get location from zip code
     */
    fun getLocationFromZipCode(zipCode: String): Location? {
        try {
            val geocoder = Geocoder(getApplication())
            val addresses = geocoder.getFromLocationName(zipCode, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val location = Location("ZipCodeProvider")
                location.latitude = address.latitude
                location.longitude = address.longitude
                return location
            }
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error getting location from zip code", e)
        }
        
        return null
    }
    
    /**
     * Select the 3 closest stations as default
     */
    private fun selectDefaultStations() {
        viewModelScope.launch {
            Log.d("WeatherViewModel", "Selecting default stations")
            
            // Wait for available stations to be populated
            fetchAvailableStations()
            // Longer delay to ensure stations are properly loaded
            delay(2000) 
            
            // Try multiple times if needed
            var attempts = 0
            while ((availableStations.value.isNullOrEmpty()) && attempts < 3) {
                Log.d("WeatherViewModel", "Retry fetching available stations, attempt ${attempts + 1}")
                fetchAvailableStations()
                delay(1500)
                attempts++
            }
            
            // Sort stations by distance and take the 3 closest ones
            val closestStations = availableStations.value?.sortedBy { it.distance }?.take(3) ?: emptyList()
            
            if (closestStations.isNotEmpty()) {
                Log.d("WeatherViewModel", "Selected ${closestStations.size} default stations")
                val stationIds = closestStations.map { it.id }
                updateSelectedStations(stationIds)
            } else {
                Log.e("WeatherViewModel", "No stations available for default selection")
            }
        }
    }
}