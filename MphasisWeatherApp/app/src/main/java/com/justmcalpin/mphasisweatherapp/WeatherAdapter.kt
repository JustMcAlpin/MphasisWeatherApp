package com.justmcalpin.mphasisweatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WeatherAdapter(private val weatherList: List<WeatherResponse>) : RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder>() {

    class WeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cityName: TextView = itemView.findViewById(R.id.city_name)
        val temperature: TextView = itemView.findViewById(R.id.temperature)
        val humidity: TextView = itemView.findViewById(R.id.humidity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.weather_item, parent, false)
        return WeatherViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        val weather = weatherList[position]

        // Set city name
        holder.cityName.text = weather.name

        // Convert temperature from Kelvin to Celsius (K - 273.15 = °C)
        val tempInCelsius = weather.main.temp - 273.15
        holder.temperature.text = "Temperature: %.2f°C".format(tempInCelsius)

        // Set humidity
        holder.humidity.text = "Humidity: ${weather.main.humidity}%"
    }

    override fun getItemCount(): Int {
        return weatherList.size
    }
}
