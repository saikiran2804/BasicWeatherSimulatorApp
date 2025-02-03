package com.example.task_3

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class WeatherAlertReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_WEATHER_ALERT = "com.example.weather.WEATHER_ALERT"
        const val CHANNEL_ID = "WeatherAlertChannel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Retrieve the alert message sent in the broadcast
        val alertMessage = intent.getStringExtra("alert_message") ?: "No message"
        Log.d("WeatherAlertReceiver", "Received alert: $alertMessage") // Log to check if the message is received
        // Display the weather alert as a notification
        showWeatherAlertNotification(context, alertMessage)
    }

    private fun showWeatherAlertNotification(context: Context, alertMessage: String) {
        // Get the NotificationManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a NotificationChannel for devices running Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create the notification
        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Weather Alert")
            .setContentText(alertMessage)
            .setSmallIcon(R.drawable.ic_weather) // Replace with your app's weather icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismiss the notification when clicked
            .build()

        // Display the notification
        notificationManager.notify(3, notification)
    }
}
