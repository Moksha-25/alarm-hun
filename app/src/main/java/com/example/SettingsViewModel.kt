package com.example

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appPreferences = AppPreferences(application)

    val snoozeDuration: StateFlow<Int> = appPreferences.snoozeDuration.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 5
    )

    val defaultVolume: StateFlow<Int> = appPreferences.defaultVolume.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 70
    )

    val defaultVibration: StateFlow<Boolean> = appPreferences.defaultVibration.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val batteryThreshold: StateFlow<Int> = appPreferences.batteryThreshold.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 80
    )

    val notifyChargingFull: StateFlow<Boolean> = appPreferences.notifyChargingFull.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val themeMode: StateFlow<String> = appPreferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "dark"
    )

    val accentColorHex: StateFlow<String> = appPreferences.accentColorHex.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "#8B5CF6"
    )

    // Derived Color flow for Compose theme
    val accentColor: StateFlow<Color> = appPreferences.accentColorHex.map { hex ->
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            Color(0xFF8B5CF6)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Color(0xFF8B5CF6)
    )

    val respectDnd: StateFlow<Boolean> = appPreferences.respectDnd.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val overrideDndBattery: StateFlow<Boolean> = appPreferences.overrideDndBattery.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val alarmNotifications: StateFlow<Boolean> = appPreferences.alarmNotifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val batteryNotifications: StateFlow<Boolean> = appPreferences.batteryNotifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val persistentBattery: StateFlow<Boolean> = appPreferences.persistentBattery.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun updateSnoozeDuration(duration: Int) {
        viewModelScope.launch {
            appPreferences.setSnoozeDuration(duration)
        }
    }

    fun updateDefaultVolume(volume: Int) {
        viewModelScope.launch {
            appPreferences.setDefaultVolume(volume)
        }
    }

    fun updateDefaultVibration(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setDefaultVibration(enabled)
        }
    }

    fun updateBatteryThreshold(threshold: Int) {
        viewModelScope.launch {
            appPreferences.setBatteryThreshold(threshold)
        }
    }

    fun updateNotifyChargingFull(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setNotifyChargingFull(enabled)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            appPreferences.setThemeMode(mode)
        }
    }

    fun updateAccentColorHex(hex: String) {
        viewModelScope.launch {
            appPreferences.setAccentColorHex(hex)
        }
    }

    fun updateRespectDnd(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setRespectDnd(enabled)
        }
    }

    fun updateOverrideDndBattery(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setOverrideDndBattery(enabled)
        }
    }

    fun updateAlarmNotifications(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setAlarmNotifications(enabled)
        }
    }

    fun updateBatteryNotifications(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setBatteryNotifications(enabled)
        }
    }

    fun updatePersistentBattery(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setPersistentBattery(enabled)
        }
    }
}
