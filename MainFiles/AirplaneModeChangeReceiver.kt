package com.example.task_3


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AirplaneModeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == EVENT) {

            // Extract the state from the intent
            val state = intent.getBooleanExtra("state", false)
            if (state) {
                Log.d(TAG, "onReceive: Airplane Mode Enabled (Device Offline)")
            } else {
                Log.d(TAG, "onReceive: Airplane Mode Disabled (Device Online)")
            }

            Log.d(TAG, "onReceive: Intent Data = ${intent.extras?.keySet()?.toList()}")
        }
    }

    companion object {
        const val TAG = "AirplaneModeChangeReceiver"
        const val EVENT = Intent.ACTION_AIRPLANE_MODE_CHANGED
    }
}
