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

    // Member variables for UI components and utilities
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var editText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WeatherAdapter
    private var searchJob: Job? = null // For managing coroutines related to search functionality
    private val apiKey = "5134d8601fd732143befa42b24c81d33"  // OpenWeather API key (replace with your own)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("Foxhound23989", "MainActivity onCreate started")

        // Initialize the location client to access the device's GPS or network-based location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up repository and ViewModel for the weather data
        val repository = WeatherRepository()
        val factory = WeatherViewModelFactory(repository)
        weatherViewModel = ViewModelProvider(this, factory)[WeatherViewModel::class.java]

        // Bind UI components (EditText, Button, RecyclerView)
        searchButton = findViewById(R.id.searchButton)
        editText = findViewById(R.id.searchBar)
        recyclerView = findViewById(R.id.weather_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Observe weather data from ViewModel and update UI accordingly
        weatherViewModel.weatherData.observe(this, Observer { weather ->
            if (weather != null) {
                Log.d("Foxhound23989", "Weather data received: $weather")
                // Update RecyclerView with weather data
                val weatherList = listOf(weather)
                adapter = WeatherAdapter(weatherList, null) // Pass the weather data to the adapter
                recyclerView.adapter = adapter
                Log.d("Foxhound23989", "Adapter set with data")
            } else {
                // If the data fetch fails, display an error message in the list
                val errorMessage = "Failed to fetch weather data. Please try again later."
                Log.d("Foxhound23989", errorMessage)
                adapter = WeatherAdapter(null, errorMessage) // Pass the error message to the adapter
                recyclerView.adapter = adapter
            }
        })

        // Check for the user's last search (if available) and automatically search for that location
        val lastSearch = getLastSearch()
        if (lastSearch != null) {
            Log.d("Foxhound23989", "Searching for last saved location: $lastSearch")
            searchJob = CoroutineScope(Dispatchers.Main).launch {
                // Fetch weather for the last searched city
                weatherViewModel.fetchWeatherByCity(lastSearch, apiKey)
                editText.setText(lastSearch)  // Optionally set the last search query in the EditText
            }
        } else {
            // If no previous search exists, request location permission and fetch weather for current location
            askLocationPermissionAndFetchWeather()
        }

        // Set up the search button click listener to initiate a weather search based on user input
        searchButton.setOnClickListener {
            val searchQuery = editText.text.toString()
            Log.d("Foxhound23989", "Search button clicked with query: $searchQuery")

            if (searchQuery.isNotEmpty()) {
                searchJob?.cancel()

                // Launch a coroutine to fetch weather for the entered city
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    Log.d("Foxhound23989", "Starting fetchWeather for: $searchQuery")

                    // Save the current search query to SharedPreferences
                    saveLastSearch(searchQuery)

                    // Fetch weather data by city name
                    weatherViewModel.fetchWeatherByCity(searchQuery, apiKey)
                }
            } else {
                Log.d("Foxhound23989", "Search query is empty")
            }
        }
    }

    // Requests location permission and fetches weather data for the user's current location if granted
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
                // Fallback to default coordinates if location access is denied
                weatherViewModel.fetchWeatherByCoordinates(47.8756477, -122.1713036, apiKey)
            }
        }

        // Request location permissions (both fine and coarse)
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Fetches the user's current location and retrieves weather data for that location
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
                        // Fetch weather by coordinates (latitude and longitude)
                        weatherViewModel.fetchWeatherByCoordinates(lat, lon, apiKey)
                    }
                } ?: run {
                    Log.d("Foxhound23989", "Location is null, using default coordinates")
                    // Fallback to default coordinates if location is unavailable
                    weatherViewModel.fetchWeatherByCoordinates(47.8756477, -122.1713036, apiKey)
                }
            }.addOnFailureListener {
                Log.e("Foxhound23989", "Failed to get location", it)
                // Fallback to default location on failure
                weatherViewModel.fetchWeatherByCoordinates(47.8756477, -122.1713036, apiKey)
            }
        }
    }

    // Saves the last search query in SharedPreferences
    fun saveLastSearch(query: String) {
        val sharedPreferences = getSharedPreferences("weather_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("last_search", query)
        editor.apply()  // Commit changes asynchronously
        Log.d("Foxhound23989", "Last search saved: $query")
    }

    // Retrieves the last search query from SharedPreferences
    fun getLastSearch(): String? {
        val sharedPreferences = getSharedPreferences("weather_prefs", MODE_PRIVATE)
        return sharedPreferences.getString("last_search", null)  // Default to null if no value found
    }
}



//interface WeatherApiService {
//    @GET("geo/1.0/reverse")
//    suspend fun getCityNameByCoordinates(
//        @Query("lat") lat: Double,
//        @Query("lon") lon: Double,
//        @Query("limit") limit: Int = 1,
//        @Query("appid") apiKey: String
//    ): List<GeocodingResponse>
//
//    @GET("data/2.5/weather")
//    suspend fun getCurrentWeatherByCity(
//        @Query("q") city: String,
//        @Query("appid") apiKey: String
//    ): WeatherResponse
//}


data class GeocodingResponse(
    val name: String, // City name
    val lat: Double,
    val lon: Double
)

//object RetrofitClient {
//    private const val BASE_URL = "https://api.openweathermap.org/"
//
//    val retrofit: Retrofit by lazy {
//        Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    val apiService: WeatherApiService by lazy {
//        retrofit.create(WeatherApiService::class.java)
//    }
//}

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


//class WeatherRepository {
//    private val apiService = RetrofitClient.apiService
//
//    suspend fun getCityName(lat: Double, lon: Double, apiKey: String): String? {
//        val geocodingResult = apiService.getCityNameByCoordinates(lat, lon, apiKey = apiKey)
//        return geocodingResult.firstOrNull()?.name  // Return the first city name found, or null
//    }
//
//    suspend fun fetchWeatherByCity(city: String, apiKey: String): WeatherResponse {
//        return apiService.getCurrentWeatherByCity(city, apiKey)
//    }
//}


//class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
//    private val _weatherData = MutableLiveData<WeatherResponse>()
//    val weatherData: LiveData<WeatherResponse> get() = _weatherData
//
//    fun fetchWeatherByCoordinates(lat: Double, lon: Double, apiKey: String) {
//        viewModelScope.launch {
//            try {
//                val cityName = repository.getCityName(lat, lon, apiKey)
//                if (cityName != null) {
//                    Log.d("Foxhound23989", "City name found: $cityName")
//                    fetchWeatherByCity(cityName, apiKey)
//                } else {
//                    Log.e("Foxhound23989", "Could not find city name for coordinates: lat=$lat, lon=$lon")
//                }
//            } catch (e: Exception) {
//                Log.e("Foxhound23989", "Error fetching city name for coordinates: lat=$lat, lon=$lon", e)
//            }
//        }
//    }
//
//    fun fetchWeatherByCity(city: String, apiKey: String) {
//        viewModelScope.launch {
//            try {
//                val response = repository.fetchWeatherByCity(city, apiKey)
//                _weatherData.value = response
//            } catch (e: Exception) {
//                Log.e("Foxhound23989", "Error fetching weather for city: $city", e)
//                _weatherData.value = null // Set to null on error
//            }
//        }
//    }
//}



//class WeatherViewModelFactory(private val repository: WeatherRepository) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        return if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
//            WeatherViewModel(repository) as T
//        } else {
//            throw IllegalArgumentException("Unknown ViewModel class")
//        }
//    }
//}
