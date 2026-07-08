package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "volt_alarm_preferences")

class AppPreferences(private val context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

    companion object {
        val SNOOZE_DURATION = intPreferencesKey("snooze_duration")
        val DEFAULT_VOLUME = intPreferencesKey("default_volume")
        val DEFAULT_VIBRATION = booleanPreferencesKey("default_vibration")
        val BATTERY_THRESHOLD = intPreferencesKey("battery_threshold")
        val NOTIFY_CHARGING_FULL = booleanPreferencesKey("notify_charging_full")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR_HEX = stringPreferencesKey("accent_color_hex")
        val RESPECT_DND = booleanPreferencesKey("respect_dnd")
        val OVERRIDE_DND_BATTERY = booleanPreferencesKey("override_dnd_battery")
        val ALARM_NOTIFICATIONS = booleanPreferencesKey("alarm_notifications")
        val BATTERY_NOTIFICATIONS = booleanPreferencesKey("battery_notifications")
        val PERSISTENT_BATTERY = booleanPreferencesKey("persistent_battery")
    }

    val snoozeDuration: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SNOOZE_DURATION] ?: sharedPrefs.getInt("snooze_duration", 5)
    }

    val defaultVolume: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_VOLUME] ?: sharedPrefs.getInt("default_volume", 70)
    }

    val defaultVibration: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DEFAULT_VIBRATION] ?: sharedPrefs.getBoolean("default_vibration", true)
    }

    val batteryThreshold: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[BATTERY_THRESHOLD] ?: sharedPrefs.getInt("battery_threshold", sharedPrefs.getInt("battery_alarm_level", 80))
    }

    val notifyChargingFull: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIFY_CHARGING_FULL] ?: sharedPrefs.getBoolean("notify_charging_full", true)
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: sharedPrefs.getString("theme_mode", "dark") ?: "dark"
    }

    val accentColorHex: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[ACCENT_COLOR_HEX] ?: sharedPrefs.getString("accent_color_hex", "#8B5CF6") ?: "#8B5CF6"
    }

    val respectDnd: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[RESPECT_DND] ?: sharedPrefs.getBoolean("respect_dnd", false)
    }

    val overrideDndBattery: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[OVERRIDE_DND_BATTERY] ?: sharedPrefs.getBoolean("override_dnd_battery", true)
    }

    val alarmNotifications: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ALARM_NOTIFICATIONS] ?: sharedPrefs.getBoolean("alarm_notifications", true)
    }

    val batteryNotifications: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BATTERY_NOTIFICATIONS] ?: sharedPrefs.getBoolean("battery_notifications", true)
    }

    val persistentBattery: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PERSISTENT_BATTERY] ?: sharedPrefs.getBoolean("persistent_battery", false)
    }

    suspend fun setSnoozeDuration(duration: Int) {
        context.dataStore.edit { prefs -> prefs[SNOOZE_DURATION] = duration }
        sharedPrefs.edit().putInt("snooze_duration", duration).apply()
    }

    suspend fun setDefaultVolume(volume: Int) {
        context.dataStore.edit { prefs -> prefs[DEFAULT_VOLUME] = volume }
        sharedPrefs.edit().putInt("default_volume", volume).apply()
    }

    suspend fun setDefaultVibration(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DEFAULT_VIBRATION] = enabled }
        sharedPrefs.edit().putBoolean("default_vibration", enabled).apply()
        // Also keep vibrate_enabled in shared preferences for legacy compatibility
        sharedPrefs.edit().putBoolean("vibrate_enabled", enabled).apply()
    }

    suspend fun setBatteryThreshold(threshold: Int) {
        context.dataStore.edit { prefs -> prefs[BATTERY_THRESHOLD] = threshold }
        sharedPrefs.edit().putInt("battery_threshold", threshold).apply()
        // Synchronize with battery_alarm_level so BatteryMonitorService receives the updates instantly
        sharedPrefs.edit().putInt("battery_alarm_level", threshold).apply()
    }

    suspend fun setNotifyChargingFull(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[NOTIFY_CHARGING_FULL] = enabled }
        sharedPrefs.edit().putBoolean("notify_charging_full", enabled).apply()
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    suspend fun setAccentColorHex(hex: String) {
        context.dataStore.edit { prefs -> prefs[ACCENT_COLOR_HEX] = hex }
        sharedPrefs.edit().putString("accent_color_hex", hex).apply()
    }

    suspend fun setRespectDnd(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[RESPECT_DND] = enabled }
        sharedPrefs.edit().putBoolean("respect_dnd", enabled).apply()
    }

    suspend fun setOverrideDndBattery(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[OVERRIDE_DND_BATTERY] = enabled }
        sharedPrefs.edit().putBoolean("override_dnd_battery", enabled).apply()
    }

    suspend fun setAlarmNotifications(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[ALARM_NOTIFICATIONS] = enabled }
        sharedPrefs.edit().putBoolean("alarm_notifications", enabled).apply()
    }

    suspend fun setBatteryNotifications(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[BATTERY_NOTIFICATIONS] = enabled }
        sharedPrefs.edit().putBoolean("battery_notifications", enabled).apply()
    }

    suspend fun setPersistentBattery(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PERSISTENT_BATTERY] = enabled }
        sharedPrefs.edit().putBoolean("persistent_battery", enabled).apply()
    }
}
