package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device reboot completed. Rescheduling all active alarms.")
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AlarmDatabase.getDatabase(context)
                    val dao = db.alarmDao()
                    val alarms = dao.getAllAlarms().first() // Get a snapshot of current alarms
                    
                    val scheduler = AlarmScheduler(context)
                    for (alarm in alarms) {
                        if (alarm.isEnabled) {
                            scheduler.schedule(alarm)
                        }
                    }
                    Log.d("BootReceiver", "Successfully rescheduled ${alarms.count { it.isEnabled }} active alarms.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
