package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log

class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            Log.d("BatteryReceiver", "Power connected. Starting BatteryMonitorService if enabled.")
            val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean("battery_alarm_enabled", false)
            if (isEnabled) {
                val serviceIntent = Intent(context, BatteryMonitorService::class.java)
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e("BatteryReceiver", "Failed to start BatteryMonitorService", e)
                }
            }
        }
    }
}
