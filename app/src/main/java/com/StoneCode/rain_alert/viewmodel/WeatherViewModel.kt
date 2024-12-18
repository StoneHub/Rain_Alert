package com.StoneCode.rain_alert.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.StoneCode.rain_alert.service.RainService
import com.StoneCode.rain_alert.service.ServiceStatusListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application),
    ServiceStatusListener { // Implement the interface

    val isServiceRunning = MutableLiveData(false)
    val lastUpdateTime = MutableLiveData("")
    val weatherData = MutableLiveData("Loading...")

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

    fun updateWeatherStatus(context: Context) {
        Log.d("WeatherViewModel", "Updating weather status")

        // Check if the service is running and update the LiveData
        val serviceRunning = isRainServiceRunning(context)
        isServiceRunning.postValue(serviceRunning)
        Log.d("WeatherViewModel", "Service running status: $serviceRunning")

        // Update the last update time
        val currentTime =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        lastUpdateTime.postValue(currentTime)
        Log.d("WeatherViewModel", "Last update time: $currentTime")

        // Simulate different weather conditions
        val isRaining = System.currentTimeMillis() % 2 == 0L // Simulate rain every other update
        val weatherStatus =
            if (isRaining) "Raining\nAPI: Simulated\n" else "Not Raining\nAPI: Simulated\n"
        weatherData.postValue(weatherStatus)
        Log.d("WeatherViewModel", "Weather data updated: $weatherStatus")
    }

    private fun isRainServiceRunning(context: Context): Boolean {
        // Check if the service is running
        return RainService.isRunning
    }

    fun startServiceChecker() {
        serviceCheckJob?.cancel() // Cancel any existing job
        serviceCheckJob = viewModelScope.launch {
            while (isActive) { // Run while the ViewModel is active
                isServiceRunning.postValue(isRainServiceRunning(getApplication()))
                delay(5000) // Check every 5 seconds (adjust as needed)
            }
        }
    }

    fun stopServiceChecker() {
        serviceCheckJob?.cancel()
        serviceCheckJob = null
    }
}