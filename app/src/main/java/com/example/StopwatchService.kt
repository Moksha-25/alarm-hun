package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class StopwatchService : Service() {

    private val binder = StopwatchBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null

    private var startTime = 0L
    private var baseTime = 0L
    private var isRunning = false

    private val _timeElapsedFlow = MutableStateFlow(0L)
    val timeElapsedFlow = _timeElapsedFlow.asStateFlow()

    private val _isRunningFlow = MutableStateFlow(false)
    val isRunningFlow = _isRunningFlow.asStateFlow()

    inner class StopwatchBinder : Binder() {
        fun getService(): StopwatchService = this@StopwatchService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> pause()
            ACTION_RESET -> reset()
        }
        return START_NOT_STICKY
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        _isRunningFlow.value = true
        startTime = SystemClock.elapsedRealtime()
        
        startForeground(NOTIFICATION_ID, buildNotification())

        tickerJob = serviceScope.launch {
            while (isRunning) {
                val currentElapsed = SystemClock.elapsedRealtime() - startTime + baseTime
                _timeElapsedFlow.value = currentElapsed
                delay(10) // Update every 10ms for centisecond resolution
            }
        }
    }

    fun pause() {
        if (!isRunning) return
        isRunning = false
        _isRunningFlow.value = false
        baseTime += SystemClock.elapsedRealtime() - startTime
        tickerJob?.cancel()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    fun forceSetElapsed(elapsed: Long) {
        _timeElapsedFlow.value = elapsed
        baseTime = elapsed
    }

    fun reset() {
        pause()
        baseTime = 0L
        _timeElapsedFlow.value = 0L
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSubText("VoltAlarm by INJAM MOKSHAGNA")
            .setContentTitle("VoltAlarm Stopwatch")
            .setContentText("Stopwatch is active and running")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Stopwatch Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 4001
        const val CHANNEL_ID = "stopwatch_channel"

        const val ACTION_START = "com.example.STOPWATCH_START"
        const val ACTION_STOP = "com.example.STOPWATCH_STOP"
        const val ACTION_RESET = "com.example.STOPWATCH_RESET"
    }
}
