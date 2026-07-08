package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class BatteryMonitorService : Service(), android.content.SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        const val CHANNEL_ID = "battery_channel"
        const val NOTIFICATION_ID = 888
        const val ACTION_SILENCE = "com.example.ACTION_SILENCE_BATTERY"
        const val ACTION_STOP = "com.example.ACTION_STOP_SERVICE"
    }

    private var hasTriggeredAlarm = false
    private var isRinging = false
    private var lastPct = 0
    private var lastCharging = false

    override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences?, key: String?) {
        if (key == "battery_alarm_level") {
            Log.d("BatteryMonitorService", "battery_alarm_level changed in preferences, re-evaluating")
            evaluateBatteryLevel(lastPct, lastCharging)
            updateServiceNotification(lastPct, lastCharging)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                lastPct = pct
                lastCharging = isCharging

                Log.d("BatteryMonitorService", "Battery changed: pct=$pct, charging=$isCharging")

                evaluateBatteryLevel(pct, isCharging)
                updateServiceNotification(pct, isCharging)
            }
        }
    }

    private fun evaluateBatteryLevel(pct: Int, isCharging: Boolean) {
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val targetPct = prefs.getInt("battery_alarm_level", 100)

        if (pct >= targetPct && isCharging) {
            if (!hasTriggeredAlarm) {
                hasTriggeredAlarm = true
                isRinging = true
                triggerBatteryAlarm()
            }
        } else if (pct < targetPct || !isCharging) {
            hasTriggeredAlarm = false
            if (isRinging) {
                isRinging = false
                AlarmSoundManager.stop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("BatteryMonitorService", "onCreate")
        createNotificationChannel()
        
        // Register preference change listener
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(batteryReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("BatteryMonitorService", "onStartCommand action=$action")

        if (action == ACTION_SILENCE) {
            silenceBatteryAlarm()
        } else if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Get initial battery status to display in notification
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        
        lastPct = pct
        lastCharging = isCharging

        evaluateBatteryLevel(pct, isCharging)

        val notification = buildNotification(pct, isCharging)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    private fun triggerBatteryAlarm() {
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val targetPct = prefs.getInt("battery_alarm_level", 100)
        Log.d("BatteryMonitorService", "Triggering battery $targetPct% alarm sound")
        // Start alarm sound with vibration
        AlarmSoundManager.start(this, vibrate = true)
        
        // Broadcast local intent to refresh UI state if main screen is open
        val localIntent = Intent("com.example.BATTERY_ALARM_TRIGGERED")
        sendBroadcast(localIntent)
    }

    private fun silenceBatteryAlarm() {
        Log.d("BatteryMonitorService", "Silencing battery alarm")
        isRinging = false
        AlarmSoundManager.stop()
        updateServiceNotification(lastPct, lastCharging)
        
        // Broadcast local intent to refresh UI
        val localIntent = Intent("com.example.BATTERY_ALARM_SILENCED")
        sendBroadcast(localIntent)
    }

    private fun updateServiceNotification(pct: Int, isCharging: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(pct, isCharging))
    }

    private fun buildNotification(pct: Int, isCharging: Boolean): Notification {
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val targetPct = prefs.getInt("battery_alarm_level", 100)
        val title = if (isRinging) "🚨 Battery Alert ($targetPct%)" else "🔋 Battery Monitor Active"
        val text = if (isRinging) {
            "Battery reached your limit of $targetPct%! Please unplug your charger."
        } else {
            "Monitoring level: $pct% (Alert limit: $targetPct%)"
        }

        // Open main activity when notification clicked
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSubText("VoltAlarm by INJAM MOKSHAGNA")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)

        if (isRinging) {
            // Add a dynamic silence button when alarm rings
            val silenceIntent = Intent(this, BatteryMonitorService::class.java).apply {
                action = ACTION_SILENCE
            }
            val silencePendingIntent = PendingIntent.getService(
                this,
                1,
                silenceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "Silence",
                silencePendingIntent
            )
            builder.setPriority(NotificationCompat.PRIORITY_HIGH) // Elevate priority when ringing
        }

        // Add a "Stop Monitor" action button
        val stopIntent = Intent(this, BatteryMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop Monitor",
            stopPendingIntent
        )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Battery Charger Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors battery charging level and alerts you based on your preference."
                setSound(null, null) // Sound is manually played through AlarmSoundManager
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        Log.d("BatteryMonitorService", "onDestroy")
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        
        unregisterReceiver(batteryReceiver)
        if (isRinging) {
            AlarmSoundManager.stop()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
