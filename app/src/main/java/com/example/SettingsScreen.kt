package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun SettingsRoute(
    alarmViewModel: AlarmViewModel,
    batteryViewModel: BatteryViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val accentColorHex by settingsViewModel.accentColorHex.collectAsState()
    val defaultSnoozeDuration by settingsViewModel.snoozeDuration.collectAsState()
    val defaultVolume by settingsViewModel.defaultVolume.collectAsState()
    val vibrateEnabled by settingsViewModel.defaultVibration.collectAsState()

    val batteryLevelTrigger by settingsViewModel.batteryThreshold.collectAsState()
    val notifyChargingComplete by settingsViewModel.notifyChargingFull.collectAsState()
    val respectSystemDnd by settingsViewModel.respectDnd.collectAsState()
    val overrideDndBattery by settingsViewModel.overrideDndBattery.collectAsState()
    val alarmNotifications by settingsViewModel.alarmNotifications.collectAsState()
    val batteryNotifications by settingsViewModel.batteryNotifications.collectAsState()
    val persistentBatteryStatus by settingsViewModel.persistentBattery.collectAsState()

    val batteryAlarmEnabled by batteryViewModel.isBatteryAlarmEnabled.collectAsState()

    SettingsScreen(
        alarmViewModel = alarmViewModel,
        themeMode = themeMode,
        accentColorHex = accentColorHex,
        defaultSnoozeDuration = defaultSnoozeDuration,
        defaultVolume = defaultVolume,
        vibrateEnabled = vibrateEnabled,
        batteryLevelTrigger = batteryLevelTrigger,
        notifyChargingComplete = notifyChargingComplete,
        respectSystemDnd = respectSystemDnd,
        overrideDndBattery = overrideDndBattery,
        alarmNotifications = alarmNotifications,
        batteryNotifications = batteryNotifications,
        persistentBatteryStatus = persistentBatteryStatus,
        batteryAlarmEnabled = batteryAlarmEnabled,
        onBack = onBack,
        onUpdateDefaultVolume = { settingsViewModel.updateDefaultVolume(it) },
        onUpdateDefaultVibration = { settingsViewModel.updateDefaultVibration(it) },
        onSetBatteryAlarmEnabled = { batteryViewModel.setBatteryAlarmEnabled(it) },
        onUpdateBatteryThreshold = { settingsViewModel.updateBatteryThreshold(it) },
        onUpdateNotifyChargingFull = { settingsViewModel.updateNotifyChargingFull(it) },
        onUpdateThemeMode = { settingsViewModel.updateThemeMode(it) },
        onUpdateAccentColorHex = { settingsViewModel.updateAccentColorHex(it) },
        onUpdateRespectDnd = { settingsViewModel.updateRespectDnd(it) },
        onUpdateOverrideDndBattery = { settingsViewModel.updateOverrideDndBattery(it) },
        onUpdateAlarmNotifications = { settingsViewModel.updateAlarmNotifications(it) },
        onUpdateBatteryNotifications = { settingsViewModel.updateBatteryNotifications(it) },
        onUpdatePersistentBattery = { settingsViewModel.updatePersistentBattery(it) },
        onUpdateSnoozeDuration = { settingsViewModel.updateSnoozeDuration(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    alarmViewModel: AlarmViewModel?,
    themeMode: String,
    accentColorHex: String,
    defaultSnoozeDuration: Int,
    defaultVolume: Int,
    vibrateEnabled: Boolean,
    batteryLevelTrigger: Int,
    notifyChargingComplete: Boolean,
    respectSystemDnd: Boolean,
    overrideDndBattery: Boolean,
    alarmNotifications: Boolean,
    batteryNotifications: Boolean,
    persistentBatteryStatus: Boolean,
    batteryAlarmEnabled: Boolean,
    onBack: () -> Unit,
    onUpdateDefaultVolume: (Int) -> Unit,
    onUpdateDefaultVibration: (Boolean) -> Unit,
    onSetBatteryAlarmEnabled: (Boolean) -> Unit,
    onUpdateBatteryThreshold: (Int) -> Unit,
    onUpdateNotifyChargingFull: (Boolean) -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateAccentColorHex: (String) -> Unit,
    onUpdateRespectDnd: (Boolean) -> Unit,
    onUpdateOverrideDndBattery: (Boolean) -> Unit,
    onUpdateAlarmNotifications: (Boolean) -> Unit,
    onUpdateBatteryNotifications: (Boolean) -> Unit,
    onUpdatePersistentBattery: (Boolean) -> Unit,
    onUpdateSnoozeDuration: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = VoltBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    HapticUtils.triggerTick(haptic)
                    onBack()
                }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 1. ALARM DEFAULTS SECTION
            Column {
                Text(
                    text = "ALARM DEFAULTS",
                    color = VoltTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VoltSurface),
                    border = BorderStroke(1.dp, VoltBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Snooze Duration Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticUtils.triggerTick(haptic)
                                    showSnoozeDialog = true
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Snooze, contentDescription = null, tint = VoltAccentViolet)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Default Snooze", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Tap to change snooze duration", color = VoltTextSecondary, fontSize = 12.sp)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$defaultSnoozeDuration min", color = VoltAccentViolet, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = VoltTextSecondary)
                            }
                        }

                        Divider(color = VoltDivider)

                        // Alarm Volume Slider
                        var localVolume by remember(defaultVolume) { mutableStateOf(defaultVolume) }
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = VoltAccentViolet)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Alarm Volume", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$localVolume%", color = VoltAccentViolet, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = localVolume.toFloat(),
                                onValueChange = { localVolume = it.toInt() },
                                onValueChangeFinished = { onUpdateDefaultVolume(localVolume) },
                                valueRange = 10f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = VoltAccentViolet,
                                    inactiveTrackColor = VoltBorder
                                )
                            )
                        }

                        Divider(color = VoltDivider)

                        // Vibration Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = VoltAccentViolet)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Dynamic Vibration", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Physical motor vibrations feedback", color = VoltTextSecondary, fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = vibrateEnabled,
                                onCheckedChange = {
                                    HapticUtils.triggerTick(haptic)
                                    onUpdateDefaultVibration(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VoltAccentViolet,
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = VoltToggleOff
                                ),
                                modifier = Modifier.testTag("vibrate_switch")
                            )
                        }
                    }
                }
            }

            // 2. BATTERY ALARM SECTION
            Column {
                Text(
                    text = "BATTERY MONITOR",
                    color = VoltTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VoltSurface),
                    border = BorderStroke(1.dp, VoltBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Battery Alarm Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.BatteryAlert, contentDescription = null, tint = VoltAccentViolet)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Low Battery Alarm", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Trigger alert at custom thresholds", color = VoltTextSecondary, fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = batteryAlarmEnabled,
                                onCheckedChange = {
                                    HapticUtils.triggerTick(haptic)
                                    onSetBatteryAlarmEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VoltAccentViolet,
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = VoltToggleOff
                                ),
                                modifier = Modifier.testTag("battery_alarm_switch")
                            )
                        }

                        Divider(color = VoltDivider)

                        // Battery Level Slider
                        var localBatteryThreshold by remember(batteryLevelTrigger) { mutableStateOf(batteryLevelTrigger) }
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = VoltAccentViolet)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Battery Level Trigger", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "$localBatteryThreshold%${if (localBatteryThreshold == 20) " (Recommended)" else ""}",
                                    color = VoltAccentViolet,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = localBatteryThreshold.toFloat(),
                                onValueChange = { localBatteryThreshold = it.toInt() },
                                onValueChangeFinished = { onUpdateBatteryThreshold(localBatteryThreshold) },
                                valueRange = 10f..50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = VoltAccentViolet,
                                    inactiveTrackColor = VoltBorder
                                )
                            )
                        }

                        Divider(color = VoltDivider)

                        // Notify when charging complete
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, tint = VoltAccentViolet)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Charge Complete Alert", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Notify when battery hits 100%", color = VoltTextSecondary, fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = notifyChargingComplete,
                                onCheckedChange = {
                                    HapticUtils.triggerTick(haptic)
                                    onUpdateNotifyChargingFull(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VoltAccentViolet,
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = VoltToggleOff
                                ),
                                modifier = Modifier.testTag("charging_complete_switch")
                            )
                        }
                    }
                }
            }

            // 3. DO NOT DISTURB SECTION
            Column {
                Text(
                    text = "DO NOT DISTURB",
                    color = VoltTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VoltSurface),
                    border = BorderStroke(1.dp, VoltBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.DoNotDisturbOn, contentDescription = null, tint = VoltAccentViolet)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Respect System DND", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Mute notifications during silent modes", color = VoltTextSecondary, fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = respectSystemDnd,
                                onCheckedChange = {
                                    HapticUtils.triggerTick(haptic)
                                    onUpdateRespectDnd(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VoltAccentViolet,
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = VoltToggleOff
                                ),
                                modifier = Modifier.testTag("respect_dnd_switch")
                            )
                        }

                        Divider(color = VoltDivider)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PriorityHigh, contentDescription = null, tint = VoltAccentViolet)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Override Battery Alerts", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("Trigger battery alarms even during DND", color = VoltTextSecondary, fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = overrideDndBattery,
                                onCheckedChange = {
                                    HapticUtils.triggerTick(haptic)
                                    onUpdateOverrideDndBattery(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VoltAccentViolet,
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = VoltToggleOff
                                ),
                                modifier = Modifier.testTag("override_dnd_switch")
                            )
                        }
                    }
                }
            }

            // 4. BACKUP & RESTORE SECTION
            if (alarmViewModel != null) {
                Column {
                    Text(
                        text = "BACKUP & RESTORE",
                        color = VoltTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.12.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = VoltSurface),
                        border = BorderStroke(1.dp, VoltBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Backup Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticUtils.triggerTick(haptic)
                                        try {
                                            val alarms = alarmViewModel.alarmsList.value
                                            val jsonArray = JSONArray()
                                            alarms.forEach { alarm ->
                                                val obj = JSONObject().apply {
                                                    put("hour", alarm.hour)
                                                    put("minute", alarm.minute)
                                                    put("label", alarm.label)
                                                    put("isEnabled", alarm.isEnabled)
                                                    put("isRecurring", alarm.isRecurring)
                                                    put("daysOfWeek", alarm.daysOfWeek)
                                                    put("vibrate", alarm.vibrate)
                                                    put("soundUri", alarm.soundUri)
                                                    put("snoozeDuration", alarm.snoozeDuration)
                                                    put("maxSnoozeCount", alarm.maxSnoozeCount)
                                                    put("challengeType", alarm.challengeType)
                                                    put("alarmProfile", alarm.alarmProfile)
                                                    put("volumeGradual", alarm.volumeGradual)
                                                    put("flashlightBlink", alarm.flashlightBlink)
                                                    put("weatherInfoEnabled", alarm.weatherInfoEnabled)
                                                    put("voiceAnnouncementEnabled", alarm.voiceAnnouncementEnabled)
                                                }
                                                jsonArray.put(obj)
                                            }
                                            val backupJson = jsonArray.toString(4)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("VoltAlarm Backup", backupJson)
                                            clipboard.setPrimaryClip(clip)

                                            Toast.makeText(context, "Alarms backup JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Backup, contentDescription = null, tint = VoltAccentViolet)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Export Backup JSON", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("Copy all alarms configuration payload", color = VoltTextSecondary, fontSize = 12.sp)
                                    }
                                }
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VoltTextSecondary)
                            }

                            Divider(color = VoltDivider)

                            // Restore Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticUtils.triggerTick(haptic)
                                        restoreJsonText = ""
                                        showRestoreDialog = true
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = VoltAccentViolet)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Import Backup JSON", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("Restore alarms from text configuration", color = VoltTextSecondary, fontSize = 12.sp)
                                    }
                                }
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VoltTextSecondary)
                            }
                        }
                    }
                }
            }

            // 5. ABOUT DEVELOPER SECTION
            Column {
                Text(
                    text = "ABOUT DEVELOPER",
                    color = VoltTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VoltSurface),
                    border = BorderStroke(1.dp, VoltBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Solo Developer Name Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = VoltAccentViolet)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Developer & Designer", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("INJAM MOKSHAGNA", color = VoltTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }

                        Divider(color = VoltDivider)

                        // Contact Email Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticUtils.triggerTick(haptic)
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                            data = android.net.Uri.parse("mailto:injammokshagna@gmail.com")
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "VoltAlarm Feedback")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Copying developer email: injammokshagna@gmail.com", Toast.LENGTH_LONG).show()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Developer Email", "injammokshagna@gmail.com"))
                                    }
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = VoltAccentViolet)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Support & Contact", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("injammokshagna@gmail.com", color = VoltTextSecondary, fontSize = 12.sp)
                                }
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VoltTextSecondary)
                        }

                        Divider(color = VoltDivider)

                        // App Credits
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = VoltAccentViolet)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("VoltAlarm Version", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text("v1.4.2 PREMIUM", color = VoltTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Default Snooze Duration Selection Dialog
    if (showSnoozeDialog) {
        Dialog(onDismissRequest = { showSnoozeDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = BorderStroke(1.dp, VoltBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Default Snooze",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val snoozePresets = listOf(1, 2, 5, 10, 15, 20, 30)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        snoozePresets.forEach { mins ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticUtils.triggerTick(haptic)
                                        onUpdateSnoozeDuration(mins)
                                        showSnoozeDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = defaultSnoozeDuration == mins,
                                    onClick = {
                                        HapticUtils.triggerTick(haptic)
                                        onUpdateSnoozeDuration(mins)
                                        showSnoozeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = VoltAccentViolet,
                                        unselectedColor = VoltTextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("$mins minutes", color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // JSON Restore Import Dialog
    if (showRestoreDialog && alarmViewModel != null) {
        Dialog(onDismissRequest = { showRestoreDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = BorderStroke(1.dp, VoltBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Restore Alarm Payload",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        "Paste raw backup JSON text below:",
                        color = VoltTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        placeholder = { Text("Paste JSON payload...", color = VoltTextSecondary, fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VoltAccentViolet,
                            unfocusedBorderColor = VoltBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                HapticUtils.triggerTick(haptic)
                                showRestoreDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VoltBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                HapticUtils.triggerTick(haptic)
                                try {
                                    val jsonArray = JSONArray(restoreJsonText)
                                    var importedCount = 0
                                    for (i in 0 until jsonArray.length()) {
                                        val json = jsonArray.getJSONObject(i)
                                        val daysString = json.optString("daysOfWeek", "")
                                        val daysSet = if (daysString.isBlank()) {
                                            emptySet()
                                        } else {
                                            daysString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
                                        }

                                        alarmViewModel.addAlarm(
                                            hour = json.getInt("hour"),
                                            minute = json.getInt("minute"),
                                            label = json.optString("label", "Alarm"),
                                            isRecurring = json.optBoolean("isRecurring", false),
                                            days = daysSet,
                                            vibrate = json.optBoolean("vibrate", true),
                                            soundUri = json.optString("soundUri", ""),
                                            snoozeDuration = json.optInt("snoozeDuration", 5),
                                            maxSnoozeCount = json.optInt("maxSnoozeCount", 3),
                                            challengeType = json.optString("challengeType", "NONE"),
                                            alarmProfile = json.optString("alarmProfile", "WORK"),
                                            volumeGradual = json.optBoolean("volumeGradual", false),
                                            flashlightBlink = json.optBoolean("flashlightBlink", false),
                                            weatherInfoEnabled = json.optBoolean("weatherInfoEnabled", false),
                                            voiceAnnouncementEnabled = json.optBoolean("voiceAnnouncementEnabled", false)
                                        )
                                        importedCount++
                                    }

                                    Toast.makeText(context, "Successfully restored $importedCount alarms!", Toast.LENGTH_LONG).show()
                                    showRestoreDialog = false
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to restore backup: Invalid JSON", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VoltAccentViolet),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                        ) {
                            Text("Restore", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Settings Screen Preview",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF121820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.PIXEL_6
)
@Composable
fun SettingsScreenPreview() {
    VoltAlarmTheme {
        SettingsScreen(
            alarmViewModel = null,
            themeMode = "dark",
            accentColorHex = "#8B5CF6",
            defaultSnoozeDuration = 5,
            defaultVolume = 75,
            vibrateEnabled = true,
            batteryLevelTrigger = 20,
            notifyChargingComplete = true,
            respectSystemDnd = false,
            overrideDndBattery = true,
            alarmNotifications = true,
            batteryNotifications = true,
            persistentBatteryStatus = false,
            batteryAlarmEnabled = true,
            onBack = {},
            onUpdateDefaultVolume = {},
            onUpdateDefaultVibration = {},
            onSetBatteryAlarmEnabled = {},
            onUpdateBatteryThreshold = {},
            onUpdateNotifyChargingFull = {},
            onUpdateThemeMode = {},
            onUpdateAccentColorHex = {},
            onUpdateRespectDnd = {},
            onUpdateOverrideDndBattery = {},
            onUpdateAlarmNotifications = {},
            onUpdateBatteryNotifications = {},
            onUpdatePersistentBattery = {},
            onUpdateSnoozeDuration = {}
        )
    }
}
