// file: C:/Users/monro/AndroidStudioProjects/Rain_Alert/app/src/main/java/com/stoneCode/rain_alert/viewmodel/WeatherViewModel.kt
package com.stoneCode.rain_alert.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stoneCode.rain_alert.firebase.FirebaseLogger
import com.stoneCode.rain_alert.service.RainService
import com.stoneCode.rain_alert.service.ServiceStatusListener
import com.stoneCode.rain_alert.repository.WeatherRepository
import com.stoneCode.rain_alert.ui.ApiStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class WeatherViewModel(application: Application) : AndroidViewModel(application),
    ServiceStatusListener {

    val isServiceRunning = MutableLiveData(false)
    val lastUpdateTime = MutableLiveData("")
    val weatherData = MutableLiveData("Loading...")
    private val weatherRepository = WeatherRepository(application.applicationContext)
    private val firebaseLogger = FirebaseLogger.getInstance()

    private var serviceCheckJob: Job? = null

    // API status tracking
    val apiStatus = MutableLiveData<ApiStatus>(ApiStatus(
        isConnected = false,
        lastUpdated = System.currentTimeMillis(),
        errorMessage = "Not connected yet"
    ))

    // Data loading status
    val isDataReady = MutableLiveData(false)

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
                // Fetch real weather data from the repository
                val currentWeather = weatherRepository.getCurrentWeather()
                weatherData.postValue(currentWeather)
                Log.d("WeatherViewModel", "Weather data updated: $currentWeather")
                
                // Update API status with success
                val responseTime = System.currentTimeMillis() - startTime
                apiStatus.postValue(ApiStatus(
                    isConnected = true,
                    lastUpdated = System.currentTimeMillis(),
                    responseTime = responseTime,
                    rainProbability = weatherRepository.getPrecipitationChance(),
                    temperature = null
                ))
                
                // Log API success to Firebase
                firebaseLogger.logApiStatus(isSuccess = true, endpoint = "getCurrentWeather")
                
                setIsDataReady(true) // Set to true when data is fetched
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather data", e)
                
                // Update API status with error
                apiStatus.postValue(ApiStatus(
                    isConnected = false,
                    lastUpdated = System.currentTimeMillis(),
                    errorMessage = e.message ?: "Unknown error"
                ))
                
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
}