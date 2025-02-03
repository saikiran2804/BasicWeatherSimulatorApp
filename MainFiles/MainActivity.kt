package com.example.task_3

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.task_3.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherAlertReceiver: WeatherAlertReceiver
    private lateinit var airplaneModeReceiver: AirplaneModeChangeReceiver

    private val handler = Handler(Looper.getMainLooper()) // Handler for periodic tasks
    private var isTaskRunning = false

    private val weatherUpdates = listOf("Sunny", "Rainy", "Cloudy", "Windy", "Stormy") // Hardcoded weather updates

    companion object {
        const val TAG = "MainActivity"

        // WeatherData object to store the current weather condition
        object WeatherData {
            var currentCondition: String = "Sunny" // Default condition
        }
    }

    private val weatherUpdateTask = object : Runnable {
        override fun run() {
            val weatherUpdate = WeatherData.currentCondition // Fetch from shared data
            Log.d(TAG, "Periodic Weather Update: $weatherUpdate")
            binding.tvWeatherUpdate.text = "Latest Weather: $weatherUpdate"
            handler.postDelayed(this, 5000) // Repeat every 5 seconds
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val serviceIntent = Intent(this, WeatherService::class.java)

        // Start and stop foreground service
        binding.btnStartService.setOnClickListener {
            Log.d(TAG, "Start Service button clicked")
            startService(serviceIntent)
            binding.tvServiceStatus.text = "Service Status: Running"
        }

        binding.btnStopService.setOnClickListener {
            Log.d(TAG, "Stop Service button clicked")
            stopService(serviceIntent)
            binding.tvServiceStatus.text = "Service Status: Stopped"
        }

        // Send weather alert
        binding.btnSendAlert.setOnClickListener {
            val customAlertMessage = binding.etCustomAlert.text.toString()
            if (customAlertMessage.isNotEmpty()) {
                sendWeatherAlert(customAlertMessage)
            } else {
                sendWeatherAlert("Severe Thunderstorm Warning!")
            }
        }

        // Start periodic task on button click
        binding.btnStartPeriodicTask.setOnClickListener {
            if (!isTaskRunning) {
                isTaskRunning = true
                startPeriodicTask()
                binding.tvWeatherUpdate.text = "Fetching weather updates..."
            }
        }

        // Stop periodic task on button click
        binding.btnStopPeriodicTask.setOnClickListener {
            isTaskRunning = false
            handler.removeCallbacksAndMessages(null) // Stop all scheduled tasks
            binding.tvWeatherUpdate.text = "Weather updates stopped."
        }

        // Check for notification permission (Android 13+)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // Initialize airplane mode receiver
        airplaneModeReceiver = AirplaneModeChangeReceiver()
    }

    private fun sendWeatherAlert(message: String) {
        val intent = Intent(WeatherAlertReceiver.ACTION_WEATHER_ALERT).apply {
            putExtra("alert_message", message)
        }
        sendBroadcast(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()

        // Register receiver for weather alert broadcast
        weatherAlertReceiver = WeatherAlertReceiver()
        val weatherAlertIntentFilter = IntentFilter(WeatherAlertReceiver.ACTION_WEATHER_ALERT)
        registerReceiver(weatherAlertReceiver, weatherAlertIntentFilter, RECEIVER_EXPORTED)

        // Register receiver for airplane mode broadcast
        val airplaneModeIntentFilter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        registerReceiver(airplaneModeReceiver, airplaneModeIntentFilter)

        // Start periodic task when activity starts
        handler.post(weatherUpdateTask)
    }

    override fun onStop() {
        super.onStop()

        // Unregister receivers when the activity stops
        unregisterReceiver(weatherAlertReceiver)
        unregisterReceiver(airplaneModeReceiver)

        // Stop periodic task when activity stops
        handler.removeCallbacks(weatherUpdateTask)
    }

    private fun startPeriodicTask() {
        if (isTaskRunning) {
            val weatherUpdate = weatherUpdates.random() // Random weather update
            WeatherData.currentCondition = weatherUpdate // Update WeatherData object
            Log.d(TAG, "Weather Update: $weatherUpdate")

            // Save the update to a file
            saveWeatherUpdateToFile(weatherUpdate)

            binding.tvWeatherUpdate.text = "Latest Weather: $weatherUpdate"

            // Schedule the next task
            handler.postDelayed({ startPeriodicTask() }, 5000) // 5 seconds delay
        }
    }

    private fun saveWeatherUpdateToFile(weatherCondition: String) {
        val file = File(filesDir, "weather_updates.txt")

        try {
            FileOutputStream(file, true).use { fos ->
                fos.write("Weather Condition: $weatherCondition\n".toByteArray())
            }
            Log.d(TAG, "Weather update saved to file.")
        } catch (e: IOException) {
            Log.e(TAG, "Error writing weather update to file: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Clean up handler to avoid memory leaks
    }
}
