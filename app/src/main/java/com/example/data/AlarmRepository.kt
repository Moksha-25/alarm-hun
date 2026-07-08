package com.example.data

import android.content.Context
import com.example.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val scheduler: AlarmScheduler
) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun getAlarmById(id: Int): Alarm? {
        return alarmDao.getAlarmById(id)
    }

    suspend fun insert(alarm: Alarm): Long {
        val id = alarmDao.insertAlarm(alarm)
        val insertedAlarm = alarm.copy(id = id.toInt())
        scheduler.schedule(insertedAlarm)
        return id
    }

    suspend fun update(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
        scheduler.schedule(alarm)
    }

    suspend fun delete(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
        scheduler.cancel(alarm)
    }

    suspend fun toggleEnabled(alarm: Alarm) {
        val updated = alarm.copy(isEnabled = !alarm.isEnabled)
        alarmDao.updateAlarm(updated)
        if (updated.isEnabled) {
            scheduler.schedule(updated)
        } else {
            scheduler.cancel(updated)
        }
    }
}
