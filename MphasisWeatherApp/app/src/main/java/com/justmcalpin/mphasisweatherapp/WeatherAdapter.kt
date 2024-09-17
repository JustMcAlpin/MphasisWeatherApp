package com.justmcalpin.mphasisweatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WeatherAdapter(private val weatherList: List<WeatherResponse>?, private val errorMessage: String?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Define two view types, one for weather data and one for the error message
    private val VIEW_TYPE_WEATHER = 1
    private val VIEW_TYPE_ERROR = 2

    class WeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cityName: TextView = itemView.findViewById(R.id.city_name)
        val temperature: TextView = itemView.findViewById(R.id.temperature)
        val humidity: TextView = itemView.findViewById(R.id.humidity)
    }

    class ErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val errorTextView: TextView = itemView.findViewById(R.id.error_message)
    }

    override fun getItemViewType(position: Int): Int {
        return if (weatherList != null && weatherList.isNotEmpty()) VIEW_TYPE_WEATHER else VIEW_TYPE_ERROR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_WEATHER) {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.weather_item, parent, false)
            WeatherViewHolder(itemView)
        } else {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.error_item, parent, false)
            ErrorViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is WeatherViewHolder && weatherList != null) {
            val weather = weatherList[position]

            // Set city name
            holder.cityName.text = weather.name

            // Convert temperature from Kelvin to Celsius (K - 273.15 = °C)
            val tempInCelsius = weather.main.temp - 273.15
            holder.temperature.text = "Temperature: %.2f°C".format(tempInCelsius)

            // Set humidity
            holder.humidity.text = "Humidity: ${weather.main.humidity}%"
        } else if (holder is ErrorViewHolder) {
            // Display the error message
            holder.errorTextView.text = errorMessage ?: "Unknown error occurred"
        }
    }

    override fun getItemCount(): Int {
        return if (weatherList != null && weatherList.isNotEmpty()) {
            weatherList.size
        } else {
            1 // To show the error message
        }
    }
}