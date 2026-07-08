package com.example

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VoltAlarmTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import android.speech.tts.TextToSpeech
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.AnimatedVisibility
import java.text.SimpleDateFormat
import java.util.*

class AlarmDismissActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var alarmId = -1
    private var alarmLabel = "Alarm"
    private var challengeType = "NONE"
    private var snoozeDuration = 5
    private var maxSnoozeCount = 3
    private var flashlightBlink = false
    private var weatherInfo = false
    private var voiceAnnouncement = false

    private var tts: TextToSpeech? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var isFlashlightOn = false
    private var flashlightJob: Job? = null

    private val localReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.ALARM_DISMISSED_LOCAL") {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Retrieve extras
        alarmId = intent.getIntExtra("ALARM_ID", -1)
        alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        challengeType = intent.getStringExtra("CHALLENGE_TYPE") ?: "NONE"
        snoozeDuration = intent.getIntExtra("SNOOZE_DURATION", 5)
        maxSnoozeCount = intent.getIntExtra("MAX_SNOOZE_COUNT", 3)
        flashlightBlink = intent.getBooleanExtra("FLASHLIGHT_BLINK", false)
        weatherInfo = intent.getBooleanExtra("WEATHER_INFO", false)
        voiceAnnouncement = intent.getBooleanExtra("VOICE_ANNOUNCEMENT", false)

        // Configure activity to show on lock screen
        setupLockScreenFlags()

        // Register local dismiss receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                localReceiver, 
                IntentFilter("com.example.ALARM_DISMISSED_LOCAL"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(localReceiver, IntentFilter("com.example.ALARM_DISMISSED_LOCAL"))
        }

        // Initialize TTS
        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) {
            // Ignore TTS failure
        }

        // Start flashlight blinking
        try {
            startFlashlightBlinking()
        } catch (e: Exception) {
            // Ignore flashlight error
        }

        setContent {
            MyApplicationTheme {
                AlarmRingingScreen(
                    label = alarmLabel,
                    challengeType = challengeType,
                    snoozeDuration = snoozeDuration,
                    maxSnoozeCount = maxSnoozeCount,
                    weatherInfoEnabled = weatherInfo,
                    onDismiss = { dismissAlarm() },
                    onSnooze = { snoozeAlarm() }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            if (voiceAnnouncement) {
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR)
                val minute = calendar.get(Calendar.MINUTE)
                val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                val dateDisplay = sdf.format(calendar.time)
                
                val text = "Good morning! The current time is $hour $minute $amPm on $dateDisplay. The weather is currently clear and 72 degrees. Have a wonderful day!"
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "alarm_announcement")
            }
        }
    }

    private fun startFlashlightBlinking() {
        if (!flashlightBlink) return
        try {
            cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null) {
                flashlightJob = CoroutineScope(Dispatchers.Default).launch {
                    while (isActive) {
                        try {
                            cameraManager?.setTorchMode(cameraId!!, true)
                            isFlashlightOn = true
                            delay(300)
                            cameraManager?.setTorchMode(cameraId!!, false)
                            isFlashlightOn = false
                            delay(300)
                        } catch (e: Exception) {
                            delay(1000)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmDismiss", "Flashlight blink error", e)
        }
    }

    private fun stopFlashlightBlinking() {
        flashlightJob?.cancel()
        flashlightJob = null
        if (isFlashlightOn && cameraId != null) {
            try {
                cameraManager?.setTorchMode(cameraId!!, false)
            } catch (e: Exception) {
                // Ignore
            }
            isFlashlightOn = false
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun dismissAlarm() {
        // Stop flashlight and TTS
        stopFlashlightBlinking()
        try { tts?.stop() } catch (e: Exception) {}
        
        // Broadcast the dismiss intent to the AlarmReceiver
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra("ALARM_ID", alarmId)
        }
        sendBroadcast(intent)
        finish()
    }

    private fun snoozeAlarm() {
        // Stop currently ringing sound and clear notification
        AlarmSoundManager.stop()
        stopFlashlightBlinking()
        try { tts?.stop() } catch (e: Exception) {}
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(AlarmReceiver.NOTIFICATION_ID)

        // Schedule snooze alarm
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", "$alarmLabel (Snoozed)")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId + 10000, // Safe offset for snooze request code
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + snoozeDuration * 60 * 1000L
        
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
            Toast.makeText(this, "Alarm snoozed for $snoozeDuration minutes", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFlashlightBlinking()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {}
        try {
            unregisterReceiver(localReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }
}

@Composable
fun AlarmRingingScreen(
    label: String,
    challengeType: String,
    snoozeDuration: Int,
    maxSnoozeCount: Int,
    weatherInfoEnabled: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var showChallenge by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
        val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        while (true) {
            val now = Calendar.getInstance().time
            currentTime = "${timeFormat.format(now)} ${amPmFormat.format(now).uppercase()}"
            currentDate = dateFormat.format(now)
            delay(1000)
        }
    }

    // Animation for pulsing dismiss button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0E17),
                        Color(0xFF1D172B),
                        Color(0xFF0C0914)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Weather Forecast Banner at the top (if enabled)
            if (weatherInfoEnabled) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.06f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB636).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = Color(0xFFFFB636),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "72°F • Clear Morning",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Beautiful day ahead! No rain in the forecast.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Clock Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = if (weatherInfoEnabled) 20.dp else 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 12.dp)
                )
                Text(
                    text = currentTime,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = currentDate,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Alarm Label Description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "ALARM RINGING",
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (challengeType != "NONE") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🔒 Challenge Active: $challengeType",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                // Large Pulsing Dismiss Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    // Pulsing Outer Wave
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
                    )
                    
                    // Main Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            )
                            .clickable {
                                if (challengeType != "NONE") {
                                    showChallenge = true
                                } else {
                                    onDismiss()
                                }
                            }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (challengeType != "NONE") Icons.Default.Lock else Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (challengeType != "NONE") "SOLVE" else "DISMISS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Snooze Button
                Button(
                    onClick = onSnooze,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .width(220.dp)
                        .height(50.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Snooze",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "SNOOZE ($snoozeDuration Min)",
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Challenge Dialog overlay
        if (showChallenge) {
            AlertDialog(
                onDismissRequest = { /* Force completion - cannot dismiss dialog */ },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Wake Up Challenge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                text = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        when (challengeType) {
                            "MATH" -> {
                                MathChallengeScreen(onSuccess = {
                                    showChallenge = false
                                    onDismiss()
                                })
                            }
                            "MEMORY" -> {
                                MemoryChallengeScreen(onSuccess = {
                                    showChallenge = false
                                    onDismiss()
                                })
                            }
                            "QR" -> {
                                QrChallengeScreen(onSuccess = {
                                    showChallenge = false
                                    onDismiss()
                                })
                            }
                            "STEPS" -> {
                                StepsChallengeScreen(onSuccess = {
                                    showChallenge = false
                                    onDismiss()
                                })
                            }
                            else -> {
                                // Fallback
                                Button(onClick = { onDismiss() }) {
                                    Text("Bypass Challenge")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChallenge = false }) {
                        Text("Cancel Challenge", color = Color.White.copy(alpha = 0.5f))
                    }
                },
                containerColor = Color(0xFF161520),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun MathChallengeScreen(onSuccess: () -> Unit) {
    var factor1 by remember { mutableStateOf((12..45).random()) }
    var factor2 by remember { mutableStateOf((11..39).random()) }
    val correct = factor1 + factor2
    
    val options = remember(factor1, factor2) {
        listOf(correct, correct + 10, correct - 5, correct + 7).shuffled()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Solve the equation to turn off the alarm",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "$factor1 + $factor2 = ?",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        options.forEach { option ->
            Button(
                onClick = {
                    if (option == correct) {
                        onSuccess()
                    } else {
                        // Reshuffle / regenerate new numbers on wrong answer
                        factor1 = (12..45).random()
                        factor2 = (11..39).random()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = option.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MemoryChallengeScreen(onSuccess: () -> Unit) {
    // 6 tiles (3 pairs of items)
    val tileContents = remember { listOf("🎨", "⭐", "🍕", "🎨", "⭐", "🍕").shuffled() }
    val flippedStates = remember { mutableStateListOf(false, false, false, false, false, false) }
    val matchedStates = remember { mutableStateListOf(false, false, false, false, false, false) }
    var firstSelectedIndex by remember { mutableStateOf<Int?>(null) }
    var secondSelectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(firstSelectedIndex, secondSelectedIndex) {
        if (firstSelectedIndex != null && secondSelectedIndex != null) {
            delay(800)
            val idx1 = firstSelectedIndex!!
            val idx2 = secondSelectedIndex!!
            if (tileContents[idx1] == tileContents[idx2]) {
                matchedStates[idx1] = true
                matchedStates[idx2] = true
            } else {
                flippedStates[idx1] = false
                flippedStates[idx2] = false
            }
            firstSelectedIndex = null
            secondSelectedIndex = null
        }
    }

    LaunchedEffect(matchedStates.toList()) {
        if (matchedStates.all { it }) {
            delay(300)
            onSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Match all 3 pairs to prove you are awake!",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        // 3x2 Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            for (row in 0 until 2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0 until 3) {
                        val index = row * 3 + col
                        val isFlipped = flippedStates[index] || matchedStates[index]

                        Card(
                            onClick = {
                                if (!isFlipped && secondSelectedIndex == null) {
                                    flippedStates[index] = true
                                    if (firstSelectedIndex == null) {
                                        firstSelectedIndex = index
                                    } else {
                                        secondSelectedIndex = index
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (matchedStates[index]) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else if (flippedStates[index]) {
                                    Color.White.copy(alpha = 0.15f)
                                } else {
                                    Color.White.copy(alpha = 0.05f)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFlipped) {
                                    Text(text = tileContents[index], fontSize = 24.sp)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.QuestionMark,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrChallengeScreen(onSuccess: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scannerBeamY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Aim your camera at your kitchen QR code",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        // Scanner view simulation box
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
        ) {
            // Simulated camera noise or texture
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Outer scanner corners
                val len = 20f
                val stroke = 4f
                val w = this.size.width
                val h = this.size.height
                
                // Top-Left
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(len, 0f), stroke)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, len), stroke)
                // Top-Right
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(w - len, 0f), stroke)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(w, len), stroke)
                // Bottom-Left
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(len, h), stroke)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(0f, h - len), stroke)
                // Bottom-Right
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(w, h), androidx.compose.ui.geometry.Offset(w - len, h), stroke)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(w, h), androidx.compose.ui.geometry.Offset(w, h - len), stroke)

                // Draw scanner laser beam
                val beamY = h * scannerBeamY
                drawLine(
                    color = Color.Red,
                    start = androidx.compose.ui.geometry.Offset(0f, beamY),
                    end = androidx.compose.ui.geometry.Offset(w, beamY),
                    strokeWidth = 3f
                )
            }

            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSuccess,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                Text("Simulate QR Code Scan Success", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StepsChallengeScreen(onSuccess: () -> Unit) {
    var stepCount by remember { mutableStateOf(0) }
    var activeFoot by remember { mutableStateOf("L") } // Alternate L and R feet taps!

    LaunchedEffect(stepCount) {
        if (stepCount >= 10) {
            delay(300)
            onSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Walk or alternate steps to silence",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        // Progress text and visual
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$stepCount / 10",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "STEPS COMPLETED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        }

        // Beautiful custom progress track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (stepCount / 10f).coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }

        // Footstep coordinator buttons (making them move Left and Right to wake up!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (activeFoot == "L") {
                        stepCount++
                        activeFoot = "R"
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeFoot == "L") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                    contentColor = if (activeFoot == "L") Color.White else Color.White.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null)
                    Text("Step Left", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Button(
                onClick = {
                    if (activeFoot == "R") {
                        stepCount++
                        activeFoot = "L"
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeFoot == "R") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                    contentColor = if (activeFoot == "R") Color.White else Color.White.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = null)
                    Text("Step Right", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Preview(
    name = "Alarm Ringing Screen Preview",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF121820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.PIXEL_6
)
@Composable
fun AlarmRingingScreenPreview() {
    VoltAlarmTheme {
        AlarmRingingScreen(
            label = "Morning Workout",
            challengeType = "MATH",
            snoozeDuration = 5,
            maxSnoozeCount = 3,
            weatherInfoEnabled = true,
            onDismiss = {},
            onSnooze = {}
        )
    }
}
