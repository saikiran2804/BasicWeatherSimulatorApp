package com.example.task_3

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class WeatherService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val weatherConditions = listOf("Sunny", "Rainy", "Cloudy", "Windy", "Stormy", "Snowfall")
    private var currentConditionIndex = 0
    private lateinit var connectivityManager: ConnectivityManager
    private var isOffline = false

    companion object {
        const val CHANNEL_ID = "WeatherServiceChannel"
        const val TAG = "WeatherService"
    }

    // Periodic weather update Runnable
    private val weatherUpdater = object : Runnable {
        override fun run() {
            if (!isOffline) {
                // Simulate fetching a random weather condition from the list
                val newCondition = weatherConditions.random()
                WeatherData.currentCondition = newCondition  // Update WeatherData
                Log.d(TAG, "Weather Condition Updated: $newCondition")

                // Save the update to a file
                saveWeatherUpdateToFile(newCondition)

                // Send a notification with the updated weather
                val notification = createNotification("Current Weather: $newCondition")
                val manager = getSystemService(NotificationManager::class.java)
                manager?.notify(1, notification)

                // Repeat the task every 5 seconds
                handler.postDelayed(this, 5000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()

        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        // Check network status before starting the service
        if (!isNetworkAvailable()) {
            showNotification("You are offline. Weather updates not started.")
            stopSelf()
            return
        }

        // Start foreground service with notification
        startForeground(1, createNotification("Weather Service Running"))

        // Start periodic weather updates
        startWeatherUpdates()

        // Register network callback to handle network status changes
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        handler.removeCallbacksAndMessages(null)
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Create notification to display service status and weather updates
    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Weather Updates")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_weather)
            .setOngoing(true)
            .build()
    }

    // Create notification channel (required for Android 8.0 and above)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weather Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    // Start the periodic weather updates
    private fun startWeatherUpdates() {
        handler.post(weatherUpdater) // Start periodic updates
    }

    // Register network callback to listen for network status changes (online/offline)
    private fun registerNetworkCallback() {
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    // Network callback to handle network status changes
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (isOffline) {
                isOffline = false
                startWeatherUpdates()
                showNotification("You are online. Weather updates resumed.")
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            if (!isOffline) {
                isOffline = true
                stopWeatherUpdates()
                showNotification("You are offline. Weather updates stopped.")
            }
        }
    }

    // Stop weather updates if offline
    private fun stopWeatherUpdates() {
        handler.removeCallbacksAndMessages(null)
    }

    // Show a notification (used when network status changes)
    private fun showNotification(message: String) {
        val notification = createNotification(message)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(2, notification)
    }

    // Check if the network is available
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Method to save the weather condition to a text file
    private fun saveWeatherUpdateToFile(weatherCondition: String) {
        val file = File(applicationContext.filesDir, "weather_updates.txt")

        try {
            // Use FileOutputStream to append the weather update to the file
            FileOutputStream(file, true).use { fos ->
                fos.write("Weather Condition: $weatherCondition\n".toByteArray())
            }
            Log.d(TAG, "Weather update saved to file.")
        } catch (e: IOException) {
            Log.e(TAG, "Error writing weather update to file: ${e.message}")
        }
    }
}
