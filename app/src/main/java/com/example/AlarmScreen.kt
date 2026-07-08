package com.example

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Alarm
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun AlarmRoute(
    alarmViewModel: AlarmViewModel,
    batteryViewModel: BatteryViewModel,
    onAddEditAlarm: (Int?) -> Unit,
    onSettingsShortcut: () -> Unit
) {
    val alarms by alarmViewModel.alarmsList.collectAsState()
    val batteryPercentage by batteryViewModel.batteryPercentage.collectAsState()
    val isCharging by batteryViewModel.isCharging.collectAsState()
    val isBatteryAlarmEnabled by batteryViewModel.isBatteryAlarmEnabled.collectAsState()
    val batteryAlarmLevel by batteryViewModel.batteryAlarmLevel.collectAsState()

    AlarmScreen(
        alarms = alarms,
        batteryPercentage = batteryPercentage,
        isCharging = isCharging,
        isBatteryAlarmEnabled = isBatteryAlarmEnabled,
        batteryAlarmLevel = batteryAlarmLevel,
        onToggleAlarm = { alarmViewModel.toggleAlarm(it) },
        onDeleteAlarm = { alarmViewModel.deleteAlarm(it) },
        onDeleteAllAlarms = { alarmViewModel.deleteAllAlarms() },
        onToggleBatteryAlarm = { batteryViewModel.setBatteryAlarmEnabled(it) },
        onBatteryAlarmLevelChange = { batteryViewModel.setBatteryAlarmLevel(it) },
        onAddEditAlarm = onAddEditAlarm,
        onSettingsShortcut = onSettingsShortcut
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    alarms: List<Alarm>,
    batteryPercentage: Int,
    isCharging: Boolean,
    isBatteryAlarmEnabled: Boolean,
    batteryAlarmLevel: Int,
    onToggleAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onDeleteAllAlarms: () -> Unit,
    onToggleBatteryAlarm: (Boolean) -> Unit,
    onBatteryAlarmLevelChange: (Int) -> Unit,
    onAddEditAlarm: (Int?) -> Unit,
    onSettingsShortcut: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var sortByTime by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Compute dynamic subtitle
    val nextAlarmText = remember(alarms) {
        val activeAlarms = alarms.filter { it.isEnabled }
        if (activeAlarms.isEmpty()) {
            "No active alarms"
        } else {
            val now = Calendar.getInstance()
            var minDiffMs = Long.MAX_VALUE
            for (alarm in activeAlarms) {
                val alarmCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (alarmCal.before(now)) {
                    alarmCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                val diff = alarmCal.timeInMillis - now.timeInMillis
                if (diff < minDiffMs) {
                    minDiffMs = diff
                }
            }
            if (minDiffMs == Long.MAX_VALUE) {
                "No active alarms"
            } else {
                val hours = minDiffMs / 3600000
                val minutes = (minDiffMs % 3600000) / 60000
                "Nearest alarm in ${hours}h ${minutes}m"
            }
        }
    }

    val sortedAlarms = remember(alarms, sortByTime) {
        if (sortByTime) {
            alarms.sortedWith(compareBy({ it.hour }, { it.minute }))
        } else {
            alarms
        }
    }

    Scaffold(
        containerColor = VoltBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    HapticUtils.triggerFabTap(haptic)
                    onAddEditAlarm(null)
                },
                containerColor = VoltAccentViolet,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("add_alarm_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Alarm",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Premium custom top bar with settings/sorting menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VoltAlarm",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Premium Alarm Suite",
                        fontSize = 12.sp,
                        color = VoltTextSecondary
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(VoltSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("⚙ Settings", color = VoltTextPrimary) },
                            onClick = {
                                onSettingsShortcut()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("⇅ Sort by time", color = VoltTextPrimary) },
                            onClick = {
                                sortByTime = !sortByTime
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🗑 Delete all alarms", color = VoltTextPrimary) },
                            onClick = {
                                onDeleteAllAlarms()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ℹ About VoltAlarm", color = VoltTextPrimary) },
                            onClick = {
                                showMenu = false
                                showAboutSheet = true
                            }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Time-aware greeting section with weather subtext
                item {
                    HeaderGreetingSection(nextAlarmText = nextAlarmText)
                }

                // Battery Status Section with sleek circular gauge
                item {
                    SectionHeader(title = "BATTERY MONITOR")
                }
                item {
                    BatteryRingStatusCard(
                        batteryPercentage = batteryPercentage,
                        isCharging = isCharging,
                        isEnabled = isBatteryAlarmEnabled,
                        triggerLevel = batteryAlarmLevel,
                        onToggle = onToggleBatteryAlarm,
                        onLevelChange = onBatteryAlarmLevelChange
                    )
                }

                // Alarms Section
                item {
                    SectionHeader(title = "SCHEDULED ALARMS")
                }

                if (sortedAlarms.isEmpty()) {
                    item {
                        EmptyStateView(
                            imageResId = R.drawable.ic_empty_alarm,
                            title = "No Alarms Set",
                            subtitle = "Tap + or add your first alarm to start waking smarter.",
                            actionText = "Add Alarm",
                            onActionClick = { onAddEditAlarm(null) }
                        )
                    }
                } else {
                    itemsIndexed(sortedAlarms, key = { _, alarm -> alarm.id }) { index, alarm ->
                        AlarmItemCard(
                            alarm = alarm,
                            index = index,
                            onToggle = { onToggleAlarm(alarm) },
                            onEdit = { onAddEditAlarm(alarm.id) },
                            onDelete = { onDeleteAlarm(alarm) }
                        )
                    }
                }
            }
        }
    }

    // Modal About Bottom Sheet containing "INJAM MOKSHAGNA" credit details
    if (showAboutSheet) {
        AboutBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showAboutSheet = false }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.15.sp,
        color = VoltTextSecondary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun HeaderGreetingSection(nextAlarmText: String) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (greeting, weatherIcon, weatherText) = when (hour) {
        in 5..11 -> Triple("Good Morning", Icons.Default.WbSunny, "72°F · Clear Sky")
        in 12..16 -> Triple("Good Afternoon", Icons.Default.Cloud, "78°F · Partly Cloudy")
        in 17..20 -> Triple("Good Evening", Icons.Default.NightsStay, "68°F · Clear")
        else -> Triple("Good Night", Icons.Default.NightsStay, "62°F · Clear")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = greeting,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = null,
                    tint = VoltAccentViolet,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = weatherText,
                    fontSize = 13.sp,
                    color = VoltAccentViolet,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "  •  $nextAlarmText",
                    fontSize = 13.sp,
                    color = VoltTextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Dynamic, real-time sweeping analog clock decoration
        SweepingAnalogClock(
            sizeDp = 86.dp
        )
    }
}

@Composable
fun BatteryRingStatusCard(
    batteryPercentage: Int,
    isCharging: Boolean,
    isEnabled: Boolean,
    triggerLevel: Int,
    onToggle: (Boolean) -> Unit,
    onLevelChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("battery_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VoltSurface),
        border = BorderStroke(1.dp, VoltBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main gauge in the center
            BatteryCircularGauge(percentage = batteryPercentage, isCharging = isCharging)

            Spacer(modifier = Modifier.height(20.dp))

            // Sub-controller card: toggle alert
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Battery Alert Alarm",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Alerts you at target while charging",
                        fontSize = 12.sp,
                        color = VoltTextSecondary
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = {
                        if (it) HapticUtils.triggerToggleOn(haptic) else HapticUtils.triggerToggleOff(haptic)
                        onToggle(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = VoltAccentViolet,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = VoltToggleOff
                    ),
                    modifier = Modifier.testTag("battery_toggle_switch")
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = VoltDivider)
                Spacer(modifier = Modifier.height(16.dp))

                var localTriggerLevel by remember(triggerLevel) { mutableStateOf(triggerLevel) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trigger Level",
                        fontSize = 13.sp,
                        color = VoltTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$localTriggerLevel%",
                        fontSize = 15.sp,
                        color = VoltAccentViolet,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = localTriggerLevel.toFloat(),
                    onValueChange = {
                        localTriggerLevel = it.toInt()
                    },
                    onValueChangeFinished = {
                        onLevelChange(localTriggerLevel)
                    },
                    valueRange = 20f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = VoltAccentViolet,
                        inactiveTrackColor = VoltBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("20%", fontSize = 11.sp, color = VoltTextSecondary)
                    Text("80% (Eco Limit)", fontSize = 11.sp, color = VoltAccentViolet, fontWeight = FontWeight.Bold)
                    Text("100%", fontSize = 11.sp, color = VoltTextSecondary)
                }
            }
        }
    }
}

@Composable
fun BatteryCircularGauge(
    percentage: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = percentage / 100f
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // Infinite breathing scale for flash icon when charging
    val infiniteTransition = rememberInfiniteTransition(label = "charging")
    val flashScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(130.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            // Track circle
            drawArc(
                color = VoltBorder,
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Progress arc
            drawArc(
                color = VoltAccentViolet,
                startAngle = -220f,
                sweepAngle = 260f * animatedProgress.value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Charging",
                        tint = VoltAccentViolet,
                        modifier = Modifier
                            .size(18.dp)
                            .scale(flashScale)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = "$percentage%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Text(
                text = if (isCharging) "CHARGING" else "DISCHARGING",
                fontSize = 10.sp,
                color = VoltTextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.1.sp
            )
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: Alarm,
    index: Int,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    val displayHour = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
    val displayMinute = String.format("%02d", alarm.minute)
    val amPm = if (alarm.hour >= 12) "PM" else "AM"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEntrance(index)
            .pressScale {
                expanded = !expanded
            }
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VoltSurface),
        border = BorderStroke(1.dp, VoltBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time & AM/PM
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$displayHour:$displayMinute",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (alarm.isEnabled) Color.White else VoltTextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = amPm,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = VoltTextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Label and Repeat Days
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = alarm.label.ifEmpty { "Alarm" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) VoltAccentViolet else VoltTextSecondary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = alarm.getDaysDisplay(),
                        fontSize = 11.sp,
                        color = VoltTextSecondary,
                        maxLines = 1
                    )
                }

                // Switch Toggle
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = {
                        if (it) HapticUtils.triggerToggleOn(haptic) else HapticUtils.triggerToggleOff(haptic)
                        onToggle()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = VoltAccentViolet,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = VoltToggleOff
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = VoltDivider)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onEdit()
                        },
                        border = BorderStroke(1.dp, VoltBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Edit", color = VoltTextPrimary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDelete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Delete", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Alarm Screen Preview",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF121820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.PIXEL_6
)
@Composable
fun AlarmScreenPreview() {
    VoltAlarmTheme {
        AlarmScreen(
            alarms = listOf(
                Alarm(id = 1, hour = 7, minute = 0, label = "Morning", isEnabled = true),
                Alarm(id = 2, hour = 8, minute = 30, label = "Work", isEnabled = false)
            ),
            batteryPercentage = 85,
            isCharging = true,
            isBatteryAlarmEnabled = true,
            batteryAlarmLevel = 90,
            onToggleAlarm = {},
            onDeleteAlarm = {},
            onDeleteAllAlarms = {},
            onToggleBatteryAlarm = {},
            onBatteryAlarmLevelChange = {},
            onAddEditAlarm = {},
            onSettingsShortcut = {}
        )
    }
}
