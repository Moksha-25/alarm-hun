package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.example.ACTION_DISMISS_ALARM"
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 999
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val vibrate = intent.getBooleanExtra("ALARM_VIBRATE", true)

        Log.d("AlarmReceiver", "onReceive action=$action, alarmId=$alarmId")

        if (action == ACTION_DISMISS) {
            // Dismiss action clicked from notification or dismiss activity
            dismissAlarm(context, alarmId)
            return
        }

        // Trigger alarm - run lookup in IO coroutine
        CoroutineScope(Dispatchers.IO).launch {
            var useVibrate = vibrate
            var useGradual = false
            var finalSoundUri = ""
            var challenge = "NONE"
            var snoozeDuration = 5
            var maxSnoozeCount = 3
            var flashlight = false
            var weather = false
            var voice = false

            if (alarmId != -1) {
                try {
                    val db = AlarmDatabase.getDatabase(context)
                    val alarm = db.alarmDao().getAlarmById(alarmId)
                    if (alarm != null) {
                        useVibrate = alarm.vibrate
                        useGradual = alarm.volumeGradual
                        finalSoundUri = alarm.soundUri
                        challenge = alarm.challengeType
                        snoozeDuration = alarm.snoozeDuration
                        maxSnoozeCount = alarm.maxSnoozeCount
                        flashlight = alarm.flashlightBlink
                        weather = alarm.weatherInfoEnabled
                        voice = alarm.voiceAnnouncementEnabled
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Failed to read alarm from DB", e)
                }
            }

            // Save active settings in shared preferences so they can be read by other parts of app
            val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("active_sound_uri", finalSoundUri)
                putBoolean("active_gradual", useGradual)
                putBoolean("active_vibrate", useVibrate)
                putString("active_challenge", challenge)
                putInt("active_snooze_duration", snoozeDuration)
                putInt("active_max_snooze_count", maxSnoozeCount)
                putBoolean("active_flashlight", flashlight)
                putBoolean("active_weather", weather)
                putBoolean("active_voice", voice)
                apply()
            }

            // Update preferred sound dynamically if specified
            if (finalSoundUri.isNotEmpty()) {
                prefs.edit().putString("preferred_alarm_sound", finalSoundUri).apply()
            }

            CoroutineScope(Dispatchers.Main).launch {
                // 1. Play the alarm sound and vibrate
                AlarmSoundManager.start(context, useVibrate, useGradual)

                // 2. Create high priority notification channel
                createNotificationChannel(context)

                // 3. Prepare intent for full screen overlay activity
                val dismissIntent = Intent(context, AlarmDismissActivity::class.java).apply {
                    putExtra("ALARM_ID", alarmId)
                    putExtra("ALARM_LABEL", alarmLabel)
                    putExtra("CHALLENGE_TYPE", challenge)
                    putExtra("SNOOZE_DURATION", snoozeDuration)
                    putExtra("MAX_SNOOZE_COUNT", maxSnoozeCount)
                    putExtra("FLASHLIGHT_BLINK", flashlight)
                    putExtra("WEATHER_INFO", weather)
                    putExtra("VOICE_ANNOUNCEMENT", voice)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    alarmId,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Prepare intent for Notification Dismiss Button
                val dismissBroadcastIntent = Intent(context, AlarmReceiver::class.java).apply {
                    this.action = ACTION_DISMISS
                    putExtra("ALARM_ID", alarmId)
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarmId,
                    dismissBroadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 4. Build Notification
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSubText("VoltAlarm by INJAM MOKSHAGNA")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Alarm Ringing!")
                    .setContentText(alarmLabel)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setContentIntent(fullScreenPendingIntent)
                    .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Dismiss",
                        dismissPendingIntent
                    )
                    .build()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)

                // Also try to start the full-screen activity directly
                try {
                    context.startActivity(dismissIntent)
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Failed to start AlarmDismissActivity directly", e)
                }
            }
        }
    }

    private fun dismissAlarm(context: Context, alarmId: Int) {
        // Stop Sound
        AlarmSoundManager.stop()

        // Cancel Notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        // Update database in background thread
        if (alarmId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AlarmDatabase.getDatabase(context)
                val dao = db.alarmDao()
                val alarm = dao.getAlarmById(alarmId)
                if (alarm != null) {
                    if (alarm.isRecurring) {
                        // Re-schedule recurring alarm for the next occurrence
                        val scheduler = AlarmScheduler(context)
                        scheduler.schedule(alarm)
                    } else {
                        // Disable one-off alarm
                        dao.updateAlarm(alarm.copy(isEnabled = false))
                    }
                }
            }
        }

        // Close system dialogs if permitted (might throw on Android 12+)
        try {
            @Suppress("DEPRECATION")
            val closeIntent = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            context.sendBroadcast(closeIntent)
        } catch (e: Exception) {
            Log.w("AlarmReceiver", "Failed to send ACTION_CLOSE_SYSTEM_DIALOGS", e)
        }
        
        // Broadcast local broadcast to tell activity to close
        val localDismissIntent = Intent("com.example.ALARM_DISMISSED_LOCAL")
        context.sendBroadcast(localDismissIntent)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Ringing Screen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows overlay notifications when a scheduled alarm goes off."
                setBypassDnd(true)
                enableVibration(true)
                setSound(null, null) // Audio is handled by AlarmSoundManager
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
