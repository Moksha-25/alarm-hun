package com.example

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AlarmRepository
    private val prefs: SharedPreferences = application.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

    val alarmsList: StateFlow<List<Alarm>>

    // World Clock Cities List
    private val _worldClockCities = MutableStateFlow<List<String>>(emptyList())
    val worldClockCities = _worldClockCities.asStateFlow()

    private val _worldClockFavorites = MutableStateFlow<List<String>>(emptyList())
    val worldClockFavorites = _worldClockFavorites.asStateFlow()

    // Preferred Alarm Sound State (Default Ringtone Uri or Name)
    private val _preferredAlarmSound = MutableStateFlow<String?>(null)
    val preferredAlarmSound = _preferredAlarmSound.asStateFlow()

    // --- App Preferences / Settings Flows ---
    private val appPreferences = AppPreferences(application)

    val themeMode: StateFlow<String> = appPreferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "dark"
    )

    val accentColorHex: StateFlow<String> = appPreferences.accentColorHex.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "#00BCD4"
    )

    val defaultSnoozeDuration: StateFlow<Int> = appPreferences.snoozeDuration.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 5
    )

    val defaultVolume: StateFlow<Int> = appPreferences.defaultVolume.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 70
    )

    val vibrateEnabled: StateFlow<Boolean> = appPreferences.defaultVibration.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    init {
        val database = AlarmDatabase.getDatabase(application)
        val scheduler = AlarmScheduler(application)
        repository = AlarmRepository(database.alarmDao(), scheduler)

        alarmsList = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        _preferredAlarmSound.value = prefs.getString("preferred_alarm_sound", "Default")
        val savedCities = prefs.getString("world_clock_cities", "Asia/Kolkata,Asia/Tokyo,Europe/London,America/New_York,Australia/Sydney")
        _worldClockCities.value = savedCities?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()

        val savedFavorites = prefs.getString("world_clock_favorites", "Asia/Kolkata,America/New_York")
        _worldClockFavorites.value = savedFavorites?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    fun addAlarm(
        hour: Int,
        minute: Int,
        label: String,
        isRecurring: Boolean,
        days: Set<Int>,
        vibrate: Boolean,
        soundUri: String = "",
        snoozeDuration: Int = 5,
        maxSnoozeCount: Int = 3,
        challengeType: String = "NONE",
        alarmProfile: String = "WORK",
        volumeGradual: Boolean = false,
        flashlightBlink: Boolean = false,
        weatherInfoEnabled: Boolean = false,
        voiceAnnouncementEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            val daysString = days.sorted().joinToString(",")
            val alarm = Alarm(
                hour = hour,
                minute = minute,
                label = label.ifBlank { "Alarm" },
                isRecurring = isRecurring,
                daysOfWeek = daysString,
                vibrate = vibrate,
                soundUri = soundUri,
                snoozeDuration = snoozeDuration,
                maxSnoozeCount = maxSnoozeCount,
                challengeType = challengeType,
                alarmProfile = alarmProfile,
                volumeGradual = volumeGradual,
                flashlightBlink = flashlightBlink,
                weatherInfoEnabled = weatherInfoEnabled,
                voiceAnnouncementEnabled = voiceAnnouncementEnabled
            )
            repository.insert(alarm)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.update(alarm)
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.toggleEnabled(alarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.delete(alarm)
        }
    }

    fun updatePreferredAlarmSound(uriString: String?) {
        _preferredAlarmSound.value = uriString
        prefs.edit().putString("preferred_alarm_sound", uriString).apply()
    }

    fun addWorldClockCity(timezoneId: String) {
        val current = _worldClockCities.value.toMutableList()
        if (!current.contains(timezoneId)) {
            current.add(timezoneId)
            _worldClockCities.value = current
            prefs.edit().putString("world_clock_cities", current.joinToString(",")).apply()
        }
    }

    fun removeWorldClockCity(timezoneId: String) {
        val current = _worldClockCities.value.toMutableList()
        current.remove(timezoneId)
        _worldClockCities.value = current
        prefs.edit().putString("world_clock_cities", current.joinToString(",")).apply()

        val favs = _worldClockFavorites.value.toMutableList()
        if (favs.contains(timezoneId)) {
            favs.remove(timezoneId)
            _worldClockFavorites.value = favs
            prefs.edit().putString("world_clock_favorites", favs.joinToString(",")).apply()
        }
    }

    fun toggleWorldClockFavorite(timezoneId: String) {
        val favs = _worldClockFavorites.value.toMutableList()
        if (favs.contains(timezoneId)) {
            favs.remove(timezoneId)
        } else {
            favs.add(timezoneId)
        }
        _worldClockFavorites.value = favs
        prefs.edit().putString("world_clock_favorites", favs.joinToString(",")).apply()
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

    fun updateDefaultSnoozeDuration(duration: Int) {
        viewModelScope.launch {
            appPreferences.setSnoozeDuration(duration)
        }
    }

    fun updateDefaultVolume(volume: Int) {
        viewModelScope.launch {
            appPreferences.setDefaultVolume(volume)
        }
    }

    fun updateVibrateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setDefaultVibration(enabled)
        }
    }

    fun deleteAllAlarms() {
        viewModelScope.launch {
            alarmsList.value.forEach {
                repository.delete(it)
            }
        }
    }
}
