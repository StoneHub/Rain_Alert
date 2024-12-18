// file: app/src/main/java/com/stoneCode/rain_alert/viewmodel/WeatherViewModel.kt
package com.stoneCode.rain_alert.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stoneCode.rain_alert.service.RainService
import com.stoneCode.rain_alert.service.ServiceStatusListener
import com.stoneCode.rain_alert.repository.WeatherRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application),
    ServiceStatusListener {

    val isServiceRunning = MutableLiveData(false)
    val lastUpdateTime = MutableLiveData("")
    val weatherData = MutableLiveData("Loading...")
    private val weatherRepository = WeatherRepository(application.applicationContext)

    private var serviceCheckJob: Job? = null

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

        // Check if the service is running and update the LiveData
        val serviceRunning = isRainServiceRunning()
        isServiceRunning.postValue(serviceRunning)
        Log.d("WeatherViewModel", "Service running status: $serviceRunning")

        // Update the last update time
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        lastUpdateTime.postValue(currentTime)
        Log.d("WeatherViewModel", "Last update time: $currentTime")

        // Fetch real weather data from the repository
        viewModelScope.launch {
            val currentWeather = weatherRepository.getCurrentWeather()
            weatherData.postValue(currentWeather)
            Log.d("WeatherViewModel", "Weather data updated: $currentWeather")
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