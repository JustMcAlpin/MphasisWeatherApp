package com.justmcalpin.mphasisweatherapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
    private val _weatherData = MutableLiveData<WeatherResponse>()
    val weatherData: LiveData<WeatherResponse> get() = _weatherData

    fun fetchWeatherByCoordinates(lat: Double, lon: Double, apiKey: String) {
        viewModelScope.launch {
            try {
                val cityName = repository.getCityName(lat, lon, apiKey)
                if (cityName != null) {
                    fetchWeatherByCity(cityName, apiKey)
                }
            } catch (e: Exception) {
                _weatherData.value = null // Handle the exception
            }
        }
    }

    fun fetchWeatherByCity(city: String, apiKey: String) {
        viewModelScope.launch {
            try {
                val response = repository.fetchWeatherByCity(city, apiKey)
                _weatherData.value = response
            } catch (e: Exception) {
                _weatherData.value = null // Handle the exception
            }
        }
    }
}

class WeatherViewModelFactory(private val repository: WeatherRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            WeatherViewModel(repository) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
