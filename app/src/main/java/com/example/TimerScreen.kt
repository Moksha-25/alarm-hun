package com.example

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TimerRoute(viewModel: TimerViewModel) {
    val totalTimeMs by viewModel.totalTimeMs.collectAsState()
    val remainingTimeMs by viewModel.remainingTimeMs.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()

    TimerScreen(
        totalTimeMs = totalTimeMs,
        remainingTimeMs = remainingTimeMs,
        isRunning = isRunning,
        isFinished = isFinished,
        onSetDuration = { h, m, s -> viewModel.setDuration(h, m, s) },
        onSetDurationFromPreset = { viewModel.setDurationFromPreset(it) },
        onStart = { viewModel.start() },
        onPause = { viewModel.pause() },
        onReset = { viewModel.reset() },
        onAddOneMinute = { viewModel.addOneMinute() },
        onStopFinishedAlert = { viewModel.stopFinishedAlert() }
    )
}

@Composable
fun TimerScreen(
    totalTimeMs: Long,
    remainingTimeMs: Long,
    isRunning: Boolean,
    isFinished: Boolean,
    onSetDuration: (Int, Int, Int) -> Unit,
    onSetDurationFromPreset: (Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onAddOneMinute: () -> Unit,
    onStopFinishedAlert: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Drum Roller Selection States
    var selectedHours by remember { mutableStateOf(0) }
    var selectedMinutes by remember { mutableStateOf(5) }
    var selectedSeconds by remember { mutableStateOf(0) }

    // Synchronize selector if timer is idle
    LaunchedEffect(selectedHours, selectedMinutes, selectedSeconds, isRunning) {
        if (!isRunning && remainingTimeMs == totalTimeMs) {
            onSetDuration(selectedHours, selectedMinutes, selectedSeconds)
        }
    }

    val fraction = if (totalTimeMs > 0) remainingTimeMs.toFloat() / totalTimeMs.toFloat() else 0f
    val isUnderOneMinute = remainingTimeMs in 1..59999

    // Pulsing alpha state when warning level is met under 1 min
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val warningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Format remaining duration into major count down + milliseconds fractional part
    val formattedParts = remember(remainingTimeMs) {
        val hrs = (remainingTimeMs / 3600000)
        val mins = (remainingTimeMs % 3600000) / 60000
        val secs = (remainingTimeMs % 60000) / 1000
        val ms = (remainingTimeMs % 1000) / 10 // Two-digit precision
        val mainTimeStr = if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
        val msStr = String.format(".%02d", ms)
        Pair(mainTimeStr, msStr)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoltBg)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Timer",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Precise countdown controller",
                    fontSize = 12.sp,
                    color = VoltTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Timer Circle Indicator or Time Setup Drum Roller
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isRunning || remainingTimeMs < totalTimeMs) {
                // Circular progress ring countdown
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val ringBrush = Brush.linearGradient(
                        colors = if (isUnderOneMinute) {
                            listOf(Color(0xFFFF5252), Color(0xFFFFB703))
                        } else {
                            listOf(VoltAccentViolet, Color(0xFFC583FF))
                        }
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 10.dp.toPx()
                        // Track ring
                        drawCircle(
                            color = VoltBorder,
                            style = Stroke(width = strokeWidth)
                        )
                        // Active animated progress ring arc
                        drawArc(
                            brush = ringBrush,
                            startAngle = -90f,
                            sweepAngle = 360f * fraction,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Large countdown digits with millisecond precision nested below
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(if (isUnderOneMinute) warningAlpha else 1.0f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = formattedParts.first,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = formattedParts.second,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltAccentViolet,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Remaining",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VoltTextSecondary,
                            letterSpacing = 0.1.sp
                        )
                    }
                }
            } else {
                // Setup Time Selector via customized Drum Roller
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VoltSurface, RoundedCornerShape(20.dp))
                        .border(BorderStroke(1.dp, VoltBorder), RoundedCornerShape(20.dp))
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours Picker
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hours", color = VoltTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        DrumRollerWheel(
                            items = (0..23).toList(),
                            initialValue = selectedHours,
                            onValueChange = { selectedHours = it }
                        )
                    }

                    Text(":", color = VoltAccentViolet, fontSize = 28.sp, fontWeight = FontWeight.Black)

                    // Minutes Picker
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mins", color = VoltTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        DrumRollerWheel(
                            items = (0..59).toList(),
                            initialValue = selectedMinutes,
                            onValueChange = { selectedMinutes = it }
                        )
                    }

                    Text(":", color = VoltAccentViolet, fontSize = 28.sp, fontWeight = FontWeight.Black)

                    // Seconds Picker
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Secs", color = VoltTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        DrumRollerWheel(
                            items = (0..59).toList(),
                            initialValue = selectedSeconds,
                            onValueChange = { selectedSeconds = it }
                        )
                    }
                }
            }
        }

        // Horizontal list of preset duration chips
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "QUICK PRESETS",
                color = VoltTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.12.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(1, 3, 5, 10, 15, 30, 45, 60)
                items(presets) { minutes ->
                    Box(
                        modifier = Modifier
                            .background(VoltSurface, RoundedCornerShape(50.dp))
                            .border(BorderStroke(1.dp, VoltBorder), RoundedCornerShape(50.dp))
                            .clickable {
                                HapticUtils.triggerTick(haptic)
                                onSetDurationFromPreset(minutes)
                                onStart()
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (minutes >= 60) "${minutes / 60}h" else "${minutes}m",
                            color = VoltAccentViolet,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.7f))

        // Actions and controllers group
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 90.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(VoltSurface, CircleShape)
                    .border(BorderStroke(1.dp, VoltBorder), CircleShape)
                    .pressScale {
                        HapticUtils.triggerTick(haptic)
                        onReset()
                    }
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
            }

            // Play / Pause central button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .background(VoltAccentViolet, CircleShape)
                    .testTag("timer_action_button")
                    .pressScale {
                        if (isRunning) {
                            HapticUtils.triggerToggleOff(haptic)
                            onPause()
                        } else {
                            HapticUtils.triggerToggleOn(haptic)
                            onStart()
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Add +1 minute button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(56.dp)
                    .width(100.dp)
                    .background(VoltSurface, RoundedCornerShape(50.dp))
                    .border(BorderStroke(1.dp, VoltBorder), RoundedCornerShape(50.dp))
                    .pressScale {
                        HapticUtils.triggerTick(haptic)
                        onAddOneMinute()
                    }
            ) {
                Text("+1 Min", color = VoltAccentViolet, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // Modal Sheet Overlay when finished
    if (isFinished) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = BorderStroke(2.dp, VoltAccentViolet)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⏰ Time's Up!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VoltAccentViolet,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your custom configured countdown has elapsed",
                        color = VoltTextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                HapticUtils.triggerTick(haptic)
                                onStopFinishedAlert()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VoltBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Stop", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                HapticUtils.triggerTick(haptic)
                                onAddOneMinute()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VoltAccentViolet),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("+1 min", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Timer Screen Preview",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF121820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.PIXEL_6
)
@Composable
fun TimerScreenPreview() {
    VoltAlarmTheme {
        TimerScreen(
            totalTimeMs = 300000L,
            remainingTimeMs = 125000L,
            isRunning = true,
            isFinished = false,
            onSetDuration = { _, _, _ -> },
            onSetDurationFromPreset = {},
            onStart = {},
            onPause = {},
            onReset = {},
            onAddOneMinute = {},
            onStopFinishedAlert = {}
        )
    }
}
