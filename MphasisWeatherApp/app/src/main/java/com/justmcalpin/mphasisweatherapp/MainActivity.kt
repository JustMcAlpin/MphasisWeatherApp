package com.justmcalpin.mphasisweatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var editText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WeatherAdapter
    private var searchJob: Job? = null
    private val apiKey = "5134d8601fd732143befa42b24c81d33"  // API Key declared here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("Foxhound23989", "MainActivity onCreate started")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val repository = WeatherRepository()
        val factory = WeatherViewModelFactory(repository)
        weatherViewModel = ViewModelProvider(this, factory)[WeatherViewModel::class.java]

        searchButton = findViewById(R.id.searchButton)
        editText = findViewById(R.id.searchBar)
        recyclerView = findViewById(R.id.weather_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Observe weather data
        weatherViewModel.weatherData.observe(this, Observer { weather ->
            if (weather != null) {
                Log.d("Foxhound23989", "Weather data received: $weather")
                val weatherList = listOf(weather)
                adapter = WeatherAdapter(weatherList, null) // No error message
                recyclerView.adapter = adapter
                Log.d("Foxhound23989", "Adapter set with data")
            } else {
                val errorMessage = "Failed to fetch weather data. Please try again later."
                Log.d("Foxhound23989", errorMessage)
                adapter = WeatherAdapter(null, errorMessage) // Pass the error message
                recyclerView.adapter = adapter
            }
        })


        // Check for the last search
        val lastSearch = getLastSearch()
        if (lastSearch != null) {
            Log.d("Foxhound23989", "Searching for last saved location: $lastSearch")
            searchJob = CoroutineScope(Dispatchers.Main).launch {
                weatherViewModel.fetchWeatherByCity(lastSearch, apiKey)
                editText.setText(lastSearch)  // Optionally set the last search query in the EditText
            }
        } else {
            // Ask for location permission and fetch weather for nearby cities immediately if granted
            askLocationPermissionAndFetchWeather()
        }

        // Add click listener to the search button
        searchButton.setOnClickListener {
            val searchQuery = editText.text.toString()
            Log.d("Foxhound23989", "Search button clicked with query: $searchQuery")

            if (searchQuery.isNotEmpty()) {
                searchJob?.cancel()

                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    Log.d("Foxhound23989", "Starting fetchWeather for: $searchQuery")

                    // Save the last searched location to SharedPreferences
                    saveLastSearch(searchQuery)

                    weatherViewModel.fetchWeatherByCity(searchQuery, apiKey)
                }
            } else {
                Log.d("Foxhound23989", "Search query is empty")
            }
        }
    }

    private fun askLocationPermissionAndFetchWeather() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val isFineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val isCoarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (isFineLocationGranted || isCoarseLocationGranted) {
                Log.d("Foxhound23989", "Location permission granted")
                getCurrentLocation()
            } else {
                Log.d("Foxhound23989", "Location permission denied, using default coordinates")
                weatherViewModel.fetchWeatherByCoordinates(47.8756477, -122.1713036, apiKey)
            }
        }

        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("Foxhound23989", "Getting current location")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val lat = it.latitude
                    val lon = it.longitude
                    Log.d("Foxhound23989", "Location received: lat=$lat, lon=$lon")
                    searchJob = CoroutineScope(Dispatchers.Main).launch {
                        weatherViewModel.fetchWeatherByCoordinates(lat, lon, apiKey)
                    }
                } ?: run {
                    Log.d("Foxhound23989", "Location is null")
                    // Use default location if location is null
                    weatherViewModel.fetchWeatherByCoordinates(47.8756477, -122.1713036, apiKey)
                }
            }.addOnFailureListener {
                Log.e("Foxhound23989", "Failed to get location", it)
                // Fallback to default location
                weatherViewModel.fetchWeatherByCoordinates(47.8756477, -122.1713036, apiKey)
            }
        }
    }


    fun saveLastSearch(query: String) {
        val sharedPreferences = getSharedPreferences("weather_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("last_search", query)
        editor.apply()  // Commit changes asynchronously
        Log.d("Foxhound23989", "Last search saved: $query")  // Add this log to verify
    }


    fun getLastSearch(): String? {
        val sharedPreferences = getSharedPreferences("weather_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("last_search", null)  // Default to null if no value found
    }
}


interface WeatherApiService {
    @GET("geo/1.0/reverse")
    suspend fun getCityNameByCoordinates(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String
    ): List<GeocodingResponse>

    @GET("data/2.5/weather")
    suspend fun getCurrentWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String
    ): WeatherResponse
}


data class GeocodingResponse(
    val name: String, // City name
    val lat: Double,
    val lon: Double
)

object RetrofitClient {
    private const val BASE_URL = "https://api.openweathermap.org/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }
}

data class WeatherResponse(
    val name: String, // City name
    val main: Main,
    val weather: List<WeatherCondition>,
    val wind: Wind
)


data class Main(
    val temp: Double,
    val humidity: Int
)

data class Wind(
    val speed: Double
)

data class WeatherCondition(
    val description: String,
    val icon: String
)


class WeatherRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun getCityName(lat: Double, lon: Double, apiKey: String): String? {
        val geocodingResult = apiService.getCityNameByCoordinates(lat, lon, apiKey = apiKey)
        return geocodingResult.firstOrNull()?.name  // Return the first city name found, or null
    }

    suspend fun fetchWeatherByCity(city: String, apiKey: String): WeatherResponse {
        return apiService.getCurrentWeatherByCity(city, apiKey)
    }
}


class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
    private val _weatherData = MutableLiveData<WeatherResponse>()
    val weatherData: LiveData<WeatherResponse> get() = _weatherData

    fun fetchWeatherByCoordinates(lat: Double, lon: Double, apiKey: String) {
        viewModelScope.launch {
            try {
                val cityName = repository.getCityName(lat, lon, apiKey)
                if (cityName != null) {
                    Log.d("Foxhound23989", "City name found: $cityName")
                    fetchWeatherByCity(cityName, apiKey)
                } else {
                    Log.e("Foxhound23989", "Could not find city name for coordinates: lat=$lat, lon=$lon")
                }
            } catch (e: Exception) {
                Log.e("Foxhound23989", "Error fetching city name for coordinates: lat=$lat, lon=$lon", e)
            }
        }
    }

    fun fetchWeatherByCity(city: String, apiKey: String) {
        viewModelScope.launch {
            try {
                val response = repository.fetchWeatherByCity(city, apiKey)
                _weatherData.value = response
            } catch (e: Exception) {
                Log.e("Foxhound23989", "Error fetching weather for city: $city", e)
                _weatherData.value = null // Set to null on error
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
