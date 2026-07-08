package com.example

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Alarm
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AddEditAlarmRoute(
    viewModel: AlarmViewModel,
    alarmId: Int?,
    onBack: () -> Unit,
    onNavigateToRingtone: () -> Unit,
    selectedRingtoneName: String
) {
    val alarms by viewModel.alarmsList.collectAsState()

    AddEditAlarmScreen(
        alarms = alarms,
        alarmId = alarmId,
        selectedRingtoneName = selectedRingtoneName,
        onBack = onBack,
        onNavigateToRingtone = onNavigateToRingtone,
        onAddAlarm = { hour, minute, label, isRecurring, days, vibrate, soundUri, snoozeDuration, maxSnoozeCount ->
            viewModel.addAlarm(
                hour = hour,
                minute = minute,
                label = label,
                isRecurring = isRecurring,
                days = days,
                vibrate = vibrate,
                soundUri = soundUri,
                snoozeDuration = snoozeDuration,
                maxSnoozeCount = maxSnoozeCount
            )
        },
        onUpdateAlarm = { alarm ->
            viewModel.updateAlarm(alarm)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    alarms: List<Alarm>,
    alarmId: Int?, // null if adding, otherwise editing
    selectedRingtoneName: String,
    onBack: () -> Unit,
    onNavigateToRingtone: () -> Unit,
    onAddAlarm: (Int, Int, String, Boolean, Set<Int>, Boolean, String, Int, Int) -> Unit,
    onUpdateAlarm: (Alarm) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Find current editing alarm
    val editingAlarm = remember(alarms, alarmId) {
        alarms.find { it.id == alarmId }
    }

    // Input States
    var alarmName by remember { mutableStateOf("") }
    var hour24 by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(30) }
    var isAm by remember { mutableStateOf(true) }

    // Convert hour to AM/PM initially
    LaunchedEffect(editingAlarm) {
        editingAlarm?.let {
            alarmName = it.label
            val h = it.hour
            minute = it.minute
            if (h >= 12) {
                isAm = false
                hour24 = h
            } else {
                isAm = true
                hour24 = if (h == 0) 12 else h
            }
        }
    }

    var selectedRepeatOption by remember { mutableStateOf("Ring once") }
    var selectedDays by remember { mutableStateOf(emptySet<Int>()) }
    var vibrateEnabled by remember { mutableStateOf(true) }
    var snoozeMinutes by remember { mutableStateOf(5) }
    var snoozeTimes by remember { mutableStateOf(3) }
    var showSnoozeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(editingAlarm) {
        editingAlarm?.let {
            selectedDays = it.getDaysSet()
            vibrateEnabled = it.vibrate
            snoozeMinutes = it.snoozeDuration
            snoozeTimes = it.maxSnoozeCount
            selectedRepeatOption = when {
                selectedDays.isEmpty() -> "Ring once"
                selectedDays.size == 5 && !selectedDays.contains(1) && !selectedDays.contains(7) -> "Weekdays"
                selectedDays.size == 2 && selectedDays.contains(1) && selectedDays.contains(7) -> "Weekends"
                else -> "Custom"
            }
        }
    }

    val onSaveAction = {
        val targetHour = if (isAm) {
            if (hour24 == 12) 0 else hour24
        } else {
            if (hour24 == 12) 12 else hour24 + 12
        }

        if (alarmId == null) {
            onAddAlarm(
                targetHour,
                minute,
                alarmName,
                selectedRepeatOption != "Ring once",
                selectedDays,
                vibrateEnabled,
                selectedRingtoneName,
                snoozeMinutes,
                snoozeTimes
            )
        } else {
            editingAlarm?.let {
                val daysString = selectedDays.sorted().joinToString(",")
                onUpdateAlarm(
                    it.copy(
                        hour = targetHour,
                        minute = minute,
                        label = alarmName,
                        isRecurring = selectedRepeatOption != "Ring once",
                        daysOfWeek = daysString,
                        vibrate = vibrateEnabled,
                        soundUri = selectedRingtoneName,
                        snoozeDuration = snoozeMinutes,
                        maxSnoozeCount = snoozeTimes
                    )
                )
            }
        }
        onBack()
    }

    Scaffold(
        containerColor = VoltBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    color = VoltTextSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { onBack() }
                        .testTag("cancel_alarm_button")
                )
                Text(
                    text = if (alarmId == null) "New Alarm" else "Edit Alarm",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Save",
                    color = VoltAccentViolet,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onSaveAction() }
                        .testTag("save_alarm_button")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Time Picker: iOS momentum drum rollers with Violet accents
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VoltSurface, RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.dp, VoltBorder), RoundedCornerShape(20.dp))
                    .padding(vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours Wheel (1..12)
                    Box(modifier = Modifier.weight(1f)) {
                        DrumRollerWheel(
                            items = (1..12).toList(),
                            initialValue = if (hour24 % 12 == 0) 12 else hour24 % 12,
                            onValueChange = { hour24 = it }
                        )
                    }

                    Text(
                        text = ":",
                        fontSize = 36.sp,
                        color = VoltAccentViolet,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minutes Wheel (0..59)
                    Box(modifier = Modifier.weight(1f)) {
                        DrumRollerWheel(
                            items = (0..59).toList(),
                            initialValue = minute,
                            onValueChange = { minute = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AM/PM custom Tab Row switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TabRow(
                    selectedTabIndex = if (isAm) 0 else 1,
                    modifier = Modifier
                        .width(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = VoltSurface,
                    contentColor = VoltAccentViolet,
                    indicator = @Composable { Box {} },
                    divider = @Composable {}
                ) {
                    Tab(
                        selected = isAm,
                        onClick = {
                            HapticUtils.triggerTick(haptic)
                            isAm = true
                        },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(
                            "AM",
                            fontWeight = FontWeight.Bold,
                            color = if (isAm) VoltAccentViolet else VoltTextSecondary
                        )
                    }
                    Tab(
                        selected = !isAm,
                        onClick = {
                            HapticUtils.triggerTick(haptic)
                            isAm = false
                        },
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(
                            "PM",
                            fontWeight = FontWeight.Bold,
                            color = if (!isAm) VoltAccentViolet else VoltTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Day Selector Chips (M, T, W, T, F, S, S) circular design
            Text(
                text = "REPEAT DAYS",
                color = VoltTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                val dayValues = listOf(2, 3, 4, 5, 6, 7, 1) // Mon..Sun
                dayNames.forEachIndexed { idx, name ->
                    val dayVal = dayValues[idx]
                    val isSelected = selectedDays.contains(dayVal)
                    val scale = animateFloatAsState(
                        targetValue = if (isSelected) 1.12f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "scale"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(scale.value)
                            .size(38.dp)
                            .background(
                                color = if (isSelected) VoltAccentViolet else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else VoltBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                HapticUtils.triggerTick(haptic)
                                selectedDays = if (isSelected) {
                                    selectedDays - dayVal
                                } else {
                                    selectedDays + dayVal
                                }
                                selectedRepeatOption = if (selectedDays.isEmpty()) "Ring once" else "Custom"
                            }
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) Color.White else VoltTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Options Card Group
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = BorderStroke(1.dp, VoltBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Alarm label textfield
                    TextField(
                        value = alarmName,
                        onValueChange = { alarmName = it },
                        placeholder = { Text("Alarm label", color = VoltTextSecondary) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = VoltAccentViolet,
                            unfocusedIndicatorColor = VoltDivider
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ringtone Picker Action Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToRingtone() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ringtone", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(selectedRingtoneName, color = VoltTextSecondary, fontSize = 13.sp)
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VoltTextSecondary)
                    }

                    Divider(color = VoltDivider)

                    // Vibrate Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Vibrate", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = vibrateEnabled,
                            onCheckedChange = {
                                vibrateEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = VoltAccentViolet,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = VoltToggleOff
                            )
                        )
                    }

                    Divider(color = VoltDivider)

                    // Snooze Configurations
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSnoozeSheet = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Snooze Options", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("$snoozeMinutes min, $snoozeTimes times", color = VoltTextSecondary, fontSize = 13.sp)
                        }
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VoltTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Wide Save Button
            Button(
                onClick = {
                    HapticUtils.triggerFabTap(haptic)
                    onSaveAction()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VoltAccentViolet),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("wide_save_alarm_button")
            ) {
                Text(
                    text = "Save Alarm",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Configurable Snooze Bottom Sheet Overlay
    if (showSnoozeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSnoozeSheet = false },
            containerColor = VoltSurface,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Snooze Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Duration: $snoozeMinutes minutes", color = VoltTextSecondary, fontSize = 14.sp)
                Slider(
                    value = snoozeMinutes.toFloat(),
                    onValueChange = { snoozeMinutes = it.toInt() },
                    valueRange = 1f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = VoltAccentViolet,
                        inactiveTrackColor = VoltBorder
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Max Snooze Count: $snoozeTimes times", color = VoltTextSecondary, fontSize = 14.sp)
                Slider(
                    value = snoozeTimes.toFloat(),
                    onValueChange = { snoozeTimes = it.toInt() },
                    valueRange = 1f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = VoltAccentViolet,
                        inactiveTrackColor = VoltBorder
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showSnoozeSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltAccentViolet),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// DrumRoller custom wheel roller
@Composable
fun DrumRollerWheel(
    items: List<Int>,
    initialValue: Int,
    onValueChange: (Int) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = items.indexOf(initialValue).coerceAtLeast(0)
    )
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex in items.indices) {
            onValueChange(items[listState.firstVisibleItemIndex])
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color.White.copy(alpha = 0.04f))
        ) {
            Divider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.align(Alignment.TopCenter))
            Divider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.align(Alignment.BottomCenter))
        }

        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentPadding = PaddingValues(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val isSelected = listState.firstVisibleItemIndex == index
                val item = items[index]
                val formattedText = if (item < 10) String.format("%02d", item) else item.toString()

                Text(
                    text = formattedText,
                    fontSize = if (isSelected) 36.sp else 28.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .alpha(if (isSelected) 1.0f else 0.40f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(
    name = "Add Edit Alarm Screen Preview",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF121820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.PIXEL_6
)
@Composable
fun AddEditAlarmScreenPreview() {
    VoltAlarmTheme {
        AddEditAlarmScreen(
            alarms = emptyList(),
            alarmId = null,
            selectedRingtoneName = "Default (Radar)",
            onBack = {},
            onNavigateToRingtone = {},
            onAddAlarm = { _, _, _, _, _, _, _, _, _ -> },
            onUpdateAlarm = {}
        )
    }
}
