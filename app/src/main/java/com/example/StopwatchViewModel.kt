package com.example

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StopwatchViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private var stopwatchService: StopwatchService? = null
    private var isBound = false

    private val _timeElapsed = MutableStateFlow(0L)
    val timeElapsed: StateFlow<Long> = _timeElapsed.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _laps = MutableStateFlow<List<StopwatchLap>>(emptyList())
    val laps: StateFlow<List<StopwatchLap>> = _laps.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StopwatchService.StopwatchBinder
            val boundService = binder.getService()
            stopwatchService = boundService
            isBound = true

            // Sync flows from service
            viewModelScope.launch {
                boundService.timeElapsedFlow.collect {
                    _timeElapsed.value = it
                }
            }
            viewModelScope.launch {
                boundService.isRunningFlow.collect {
                    _isRunning.value = it
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            stopwatchService = null
            isBound = false
        }
    }

    init {
        bindStopwatchService()
    }

    private fun bindStopwatchService() {
        val intent = Intent(context, StopwatchService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun start() {
        val intent = Intent(context, StopwatchService::class.java).apply {
            action = StopwatchService.ACTION_START
        }
        context.startService(intent)
        stopwatchService?.start()
    }

    fun pause() {
        val intent = Intent(context, StopwatchService::class.java).apply {
            action = StopwatchService.ACTION_STOP
        }
        context.startService(intent)
        stopwatchService?.pause()
    }

    fun reset() {
        val intent = Intent(context, StopwatchService::class.java).apply {
            action = StopwatchService.ACTION_RESET
        }
        context.startService(intent)
        stopwatchService?.reset()
        _laps.value = emptyList()
        _timeElapsed.value = 0L
    }

    fun addLap() {
        val currentTotal = _timeElapsed.value
        val lapList = _laps.value
        
        val lapTime = if (lapList.isEmpty()) {
            currentTotal
        } else {
            currentTotal - lapList.first().overallTimeMs
        }

        val newLap = StopwatchLap(
            lapIndex = lapList.size + 1,
            lapTimeMs = lapTime,
            overallTimeMs = currentTotal
        )
        _laps.value = listOf(newLap) + lapList
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }
}
