package com.example

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneSelectionScreen(
    currentRingtone: String,
    onRingtoneSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTone by remember { mutableStateOf(currentRingtone) }
    var playingToneName by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var customFileUri by remember { mutableStateOf<Uri?>(null) }
    var customFileName by remember { mutableStateOf<String?>(null) }

    // Init custom file details if previously stored as Uri
    LaunchedEffect(currentRingtone) {
        if (currentRingtone.startsWith("content://")) {
            try {
                val uri = Uri.parse(currentRingtone)
                customFileUri = uri
                customFileName = getFileNameFromUri(context, uri) ?: "Custom Audio File"
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Media Player Cleanup helper
    fun stopAndReleasePlayer() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        playingToneName = null
    }

    fun playPreview(name: String, resourceName: String? = null, customUri: Uri? = null) {
        stopAndReleasePlayer()
        try {
            val mp = MediaPlayer().apply {
                if (customUri != null) {
                    setDataSource(context, customUri)
                } else {
                    // Fallback system standard notification URI
                    val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                    setDataSource(context, defaultUri)
                }
                setAudioStreamType(android.media.AudioManager.STREAM_ALARM)
                isLooping = false
                prepare()
                start()
            }
            mediaPlayer = mp
            playingToneName = name
            mp.setOnCompletionListener {
                playingToneName = null
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not play preview sound", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        }
    }

    // SAF Document Picker launcher for custom files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Request persistable permission
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                customFileUri = it
                val name = getFileNameFromUri(context, it) ?: "Custom Sound"
                customFileName = name
                selectedTone = it.toString()
                onRingtoneSelected(it.toString())
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load custom ringtone", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseBorderWidth by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_width"
    )

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
                    stopAndReleasePlayer()
                    onBack()
                }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Ringtone",
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // SECTION 1: Defaults (No label header)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = BorderStroke(1.dp, VoltBorder)
            ) {
                Column {
                    // Default Row
                    RingtoneRow(
                        title = "Default",
                        subtitle = "Classic",
                        isSelected = selectedTone == "Default" || selectedTone.isBlank(),
                        isPlaying = playingToneName == "Default",
                        onPlay = { playPreview("Default") },
                        onSelect = {
                            selectedTone = "Default"
                            onRingtoneSelected("Default")
                        }
                    )
                    Divider(color = VoltDivider)
                    // None Row
                    RingtoneRow(
                        title = "None",
                        subtitle = "Silent",
                        isSelected = selectedTone == "None",
                        isPlaying = false,
                        onPlay = null,
                        onSelect = {
                            selectedTone = "None"
                            onRingtoneSelected("None")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: System Tones
            Text(
                text = "SYSTEM",
                color = VoltTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = BorderStroke(1.dp, VoltBorder)
            ) {
                val systemTones = listOf("Classic", "Nature", "Electronic", "Gentle", "Morning")
                Column {
                    systemTones.forEachIndexed { index, toneName ->
                        RingtoneRow(
                            title = toneName,
                            subtitle = "Ambient theme",
                            isSelected = selectedTone == toneName,
                            isPlaying = playingToneName == toneName,
                            onPlay = { playPreview(toneName) },
                            onSelect = {
                                selectedTone = toneName
                                onRingtoneSelected(toneName)
                            }
                        )
                        if (index < systemTones.size - 1) {
                            Divider(color = VoltDivider)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: Custom Device File
            Text(
                text = "CUSTOM",
                color = VoltTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_ringtone_section"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = if (playingToneName == "CustomFile") BorderStroke(pulseBorderWidth.dp, VoltAccentGreen) else BorderStroke(1.dp, VoltBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (customFileUri == null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    filePickerLauncher.launch(arrayOf("audio/*"))
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = VoltAccentCyan)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("On this device", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VoltTextSecondary)
                        }
                    } else {
                        // Styled selected custom tone item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = VoltAccentGreen)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = customFileName ?: "Custom Sound File",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Custom ringtone",
                                        color = VoltTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isThisPlaying = playingToneName == "CustomFile"
                                IconButton(onClick = {
                                    if (isThisPlaying) {
                                        stopAndReleasePlayer()
                                    } else {
                                        customFileUri?.let { uri ->
                                            playPreview("CustomFile", customUri = uri)
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (isThisPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                        contentDescription = "Preview",
                                        tint = VoltAccentGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                IconButton(onClick = {
                                    stopAndReleasePlayer()
                                    customFileUri = null
                                    customFileName = null
                                    selectedTone = "Default"
                                    onRingtoneSelected("Default")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Clear",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(22.dp)
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
fun RingtoneRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isPlaying: Boolean,
    onPlay: (() -> Unit)?,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (isSelected) VoltAccentCyan else VoltTextSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onPlay != null) {
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "Play Preview",
                        tint = if (isPlaying) VoltAccentGreen else VoltTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Radio Button selection
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = VoltAccentCyan,
                    unselectedColor = VoltTextSecondary
                )
            )
        }
    }
}

// Helper to query file name
fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@Preview(
    name = "Ringtone Selection Screen Preview",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF121820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.PIXEL_6
)
@Composable
fun RingtoneSelectionScreenPreview() {
    VoltAlarmTheme {
        RingtoneSelectionScreen(
            currentRingtone = "Default",
            onRingtoneSelected = {},
            onBack = {}
        )
    }
}
