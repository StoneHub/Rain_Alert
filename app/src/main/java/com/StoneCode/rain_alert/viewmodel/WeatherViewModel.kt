package com.StoneCode.rain_alert.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.StoneCode.rain_alert.service.RainService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    val isServiceRunning = MutableLiveData(false)
    val lastUpdateTime = MutableLiveData("")
    val weatherData = MutableLiveData("Loading...")

    fun updateWeatherStatus(context: Context) {
        Log.d("WeatherViewModel", "Updating weather status")

        // Check if the service is running and update the LiveData
        val serviceRunning = isRainServiceRunning(context)
        isServiceRunning.postValue(serviceRunning)
        Log.d("WeatherViewModel", "Service running status: $serviceRunning")

        // Update the last update time
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        lastUpdateTime.postValue(currentTime)
        Log.d("WeatherViewModel", "Last update time: $currentTime")

        // Simulate different weather conditions
        val isRaining = System.currentTimeMillis() % 2 == 0L // Simulate rain every other update
        val weatherStatus = if (isRaining) "Raining\nAPI: Simulated\n" else "Not Raining\nAPI: Simulated\n"
        weatherData.postValue(weatherStatus)
        Log.d("WeatherViewModel", "Weather data updated: $weatherStatus")
    }

    private fun isRainServiceRunning(context: Context): Boolean {
        // Check if the service is running
        return RainService.isRunning
    }
}