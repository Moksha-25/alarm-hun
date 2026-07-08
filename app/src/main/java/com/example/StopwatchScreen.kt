package com.example

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StopwatchRoute(viewModel: StopwatchViewModel) {
    val timeElapsed by viewModel.timeElapsed.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val laps by viewModel.laps.collectAsState()

    StopwatchScreen(
        timeElapsed = timeElapsed,
        isRunning = isRunning,
        laps = laps,
        onAddLap = { viewModel.addLap() },
        onReset = { viewModel.reset() },
        onStart = { viewModel.start() },
        onPause = { viewModel.pause() }
    )
}

@Composable
fun StopwatchScreen(
    timeElapsed: Long,
    isRunning: Boolean,
    laps: List<StopwatchLap>,
    onAddLap: () -> Unit,
    onReset: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Infinite breathing/rotation for visual cue when running
    val infiniteTransition = rememberInfiniteTransition(label = "ticker")
    val tickerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Parse elapsed milliseconds to minutes, seconds, and centiseconds
    val minutes = (timeElapsed / 60000) % 60
    val seconds = (timeElapsed / 1000) % 60
    val centiseconds = (timeElapsed / 10) % 100

    val mainTimeStr = String.format("%02d:%02d", minutes, seconds)
    val centisStr = String.format(".%02d", centiseconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoltBg)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Stopwatch",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "High-precision split tracker",
                    fontSize = 12.sp,
                    color = VoltTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main elegant digital readout card
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Spinning outer glowing border
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = VoltBorder,
                    style = Stroke(width = 6.dp.toPx())
                )
                if (isRunning) {
                    drawArc(
                        color = VoltAccentViolet,
                        startAngle = tickerRotation - 90f,
                        sweepAngle = 60f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
            }

            // High-contrast primary duration
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = mainTimeStr,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = centisStr,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltAccentViolet,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ELAPSED TIME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltTextSecondary,
                    letterSpacing = 0.15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // High contrast Lap Split list
        if (laps.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = BorderStroke(1.dp, VoltBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Split headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("LAP", fontSize = 11.sp, color = VoltTextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                        Text("SPLIT TIME", fontSize = 11.sp, color = VoltTextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("TOTAL RECORD", fontSize = 11.sp, color = VoltTextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }

                    Divider(color = VoltDivider)

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(laps, key = { _, lap -> lap.lapIndex }) { index, lap ->
                            val isEven = lap.lapIndex % 2 == 0
                            val rowBg = if (isEven) Color.White.copy(alpha = 0.02f) else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .staggeredEntrance(index)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(rowBg)
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .background(VoltAccentViolet.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = String.format("#%02d", lap.lapIndex),
                                        fontSize = 11.sp,
                                        color = VoltAccentViolet,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = formatLapTime(lap.lapTimeMs),
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatLapTime(lap.overallTimeMs),
                                    fontSize = 13.sp,
                                    color = VoltTextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Pristine visual empty logs area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Press play to start recording split lap intervals",
                    color = VoltTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Control buttons deck
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 90.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lap / Reset button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(50.dp)
                    .width(110.dp)
                    .background(VoltSurface, RoundedCornerShape(50.dp))
                    .border(BorderStroke(1.dp, VoltBorder), RoundedCornerShape(50.dp))
                    .pressScale {
                        HapticUtils.triggerTick(haptic)
                        if (isRunning) {
                            onAddLap()
                        } else {
                            onReset()
                        }
                    }
            ) {
                Text(
                    text = if (isRunning) "Lap" else "Reset",
                    color = VoltAccentViolet,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Centered Play / Pause Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .background(VoltAccentViolet, CircleShape)
                    .testTag("stopwatch_action_button")
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
        }
    }
}

private fun formatLapTime(timeMs: Long): String {
    val mins = (timeMs / 60000) % 60
    val secs = (timeMs / 1000) % 60
    val centis = (timeMs / 10) % 100
    return String.format("%02d:%02d.%02d", mins, secs, centis)
}

@Preview(
    name = "Stopwatch Screen Preview",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF121820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.PIXEL_6
)
@Composable
fun StopwatchScreenPreview() {
    VoltAlarmTheme {
        StopwatchScreen(
            timeElapsed = 75320L,
            isRunning = true,
            laps = listOf(
                StopwatchLap(lapIndex = 1, lapTimeMs = 35000L, overallTimeMs = 35000L),
                StopwatchLap(lapIndex = 2, lapTimeMs = 40320L, overallTimeMs = 75320L)
            ),
            onAddLap = {},
            onReset = {},
            onStart = {},
            onPause = {}
        )
    }
}
