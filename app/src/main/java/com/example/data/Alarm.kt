package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = false,
    val daysOfWeek: String = "", // e.g., "2,3" for Mon, Tue (1=Sunday, 2=Monday, ..., 7=Saturday)
    val vibrate: Boolean = true,
    val soundUri: String = "", // Empty means default alarm sound
    
    // Premium fields
    val snoozeDuration: Int = 5,
    val maxSnoozeCount: Int = 3,
    val challengeType: String = "NONE", // NONE, MATH, MEMORY, QR, STEPS
    val alarmProfile: String = "WORK", // WORK, WEEKEND, STUDY, GYM
    val volumeGradual: Boolean = false,
    val flashlightBlink: Boolean = false,
    val weatherInfoEnabled: Boolean = false,
    val voiceAnnouncementEnabled: Boolean = false
) {
    // Helper to get selected days as integer set
    fun getDaysSet(): Set<Int> {
        if (daysOfWeek.isBlank()) return emptySet()
        return daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    // Helper to check if a specific day is active (1=Sunday, ..., 7=Saturday)
    fun isDayActive(day: Int): Boolean {
        return getDaysSet().contains(day)
    }

    // Friendly display of the scheduled days
    fun getDaysDisplay(): String {
        if (!isRecurring) return "Once"
        val days = getDaysSet()
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Every day"
        if (days.size == 5 && !days.contains(1) && !days.contains(7)) return "Weekdays"
        if (days.size == 2 && days.contains(1) && days.contains(7)) return "Weekends"
        
        val dayNames = mapOf(
            1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed", 
            5 to "Thu", 6 to "Fri", 7 to "Sat"
        )
        return days.sorted().map { dayNames[it] ?: "" }.filter { it.isNotEmpty() }.joinToString(", ")
    }
}
