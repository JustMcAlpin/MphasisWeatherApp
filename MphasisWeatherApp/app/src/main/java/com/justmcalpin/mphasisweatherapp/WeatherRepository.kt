package com.justmcalpin.mphasisweatherapp

class WeatherRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun getCityName(lat: Double, lon: Double, apiKey: String): String? {
        val geocodingResult = apiService.getCityNameByCoordinates(lat, lon, limit = 1, apiKey = apiKey)  // Pass limit as an Int
        return geocodingResult.firstOrNull()?.name  // Return the first city name found, or null
    }


    suspend fun fetchWeatherByCity(city: String, apiKey: String): WeatherResponse {
        return apiService.getCurrentWeatherByCity(city, apiKey)
    }
}
