package com.example

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private var timerJob: Job? = null

    private val _totalTimeMs = MutableStateFlow(0L)
    val totalTimeMs: StateFlow<Long> = _totalTimeMs.asStateFlow()

    private val _remainingTimeMs = MutableStateFlow(0L)
    val remainingTimeMs: StateFlow<Long> = _remainingTimeMs.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    // Sound manager specific to Timer so it doesn't collide with main alarm sounds
    private var alarmSoundManager: AlarmSoundManager? = null

    fun setDuration(hours: Int, minutes: Int, seconds: Int) {
        val totalMs = (hours * 3600L + minutes * 60L + seconds) * 1000L
        _totalTimeMs.value = totalMs
        _remainingTimeMs.value = totalMs
        _isFinished.value = false
    }

    fun setDurationFromPreset(minutes: Int) {
        setDuration(0, minutes, 0)
    }

    fun start() {
        if (_remainingTimeMs.value <= 0) return
        if (_isRunning.value) return

        _isRunning.value = true
        _isFinished.value = false
        timerJob = viewModelScope.launch(Dispatchers.Main) {
            val tickInterval = 100L
            while (_remainingTimeMs.value > 0) {
                delay(tickInterval)
                val newRemaining = _remainingTimeMs.value - tickInterval
                if (newRemaining <= 0) {
                    _remainingTimeMs.value = 0
                    onTimerFinished()
                } else {
                    _remainingTimeMs.value = newRemaining
                }
            }
        }
    }

    fun pause() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun reset() {
        pause()
        _remainingTimeMs.value = _totalTimeMs.value
        _isFinished.value = false
        stopFinishedAlert()
    }

    fun addOneMinute() {
        val additionalMs = 60 * 1000L
        _totalTimeMs.value = _totalTimeMs.value + additionalMs
        _remainingTimeMs.value = _remainingTimeMs.value + additionalMs
        
        if (_isFinished.value) {
            stopFinishedAlert()
            _isFinished.value = false
            start()
        }
    }

    private fun onTimerFinished() {
        _isRunning.value = false
        _isFinished.value = true
        // Play selected ringtone
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val preferredSound = prefs.getString("preferred_alarm_sound", null)
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                AlarmSoundManager.start(context, true, false)
            } catch (e: Exception) {
                // Fallback direct play if needed
            }
        }
    }

    fun stopFinishedAlert() {
        _isFinished.value = false
        AlarmSoundManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
