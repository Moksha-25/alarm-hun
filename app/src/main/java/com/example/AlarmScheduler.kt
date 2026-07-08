package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Alarm
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        val triggerTime = calculateNextTriggerTime(alarm) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_VIBRATE", alarm.vibrate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("AlarmScheduler", "Alarm schedule succeeded for ${alarm.label} (ID: ${alarm.id}) at $triggerTime")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Exact alarm permission missing, falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun calculateNextTriggerTime(alarm: Alarm): Long? {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!alarm.isRecurring) {
            // Once-off alarm
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        } else {
            // Recurring alarm
            val activeDays = alarm.getDaysSet()
            if (activeDays.isEmpty()) {
                // If recurring is toggled but no days are checked, treat as once-off
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                return target.timeInMillis
            }

            val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, ..., 7=Saturday
            
            // If the alarm time today is in the future and today is an active day
            if (target.after(now) && activeDays.contains(currentDayOfWeek)) {
                return target.timeInMillis
            }

            // Find the next active day (up to 7 days in the future)
            var daysToAdd = 1
            while (daysToAdd <= 7) {
                var nextDayOfWeek = currentDayOfWeek + daysToAdd
                if (nextDayOfWeek > 7) {
                    nextDayOfWeek -= 7
                }
                if (activeDays.contains(nextDayOfWeek)) {
                    target.add(Calendar.DAY_OF_YEAR, daysToAdd)
                    return target.timeInMillis
                }
                daysToAdd++
            }
            return null
        }
    }
}
