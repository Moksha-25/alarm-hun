package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefs: SharedPreferences = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

    private val _batteryPercentage = MutableStateFlow(100)
    val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _isBatteryAlarmEnabled = MutableStateFlow(false)
    val isBatteryAlarmEnabled: StateFlow<Boolean> = _isBatteryAlarmEnabled.asStateFlow()

    private val _batteryAlarmLevel = MutableStateFlow(100)
    val batteryAlarmLevel: StateFlow<Int> = _batteryAlarmLevel.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                _batteryPercentage.value = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
    }

    init {
        _isBatteryAlarmEnabled.value = prefs.getBoolean("battery_alarm_enabled", false)
        _batteryAlarmLevel.value = prefs.getInt("battery_alarm_level", 100)

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(batteryReceiver, filter)
        }
    }

    fun setBatteryAlarmEnabled(enabled: Boolean) {
        _isBatteryAlarmEnabled.value = enabled
        prefs.edit().putBoolean("battery_alarm_enabled", enabled).apply()
        
        // Start or stop battery service accordingly
        val serviceIntent = Intent(context, BatteryMonitorService::class.java)
        if (enabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                // Ignore background limitations
            }
        } else {
            context.stopService(serviceIntent)
        }
    }

    fun setBatteryAlarmLevel(level: Int) {
        _batteryAlarmLevel.value = level
        prefs.edit().putInt("battery_alarm_level", level).apply()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
