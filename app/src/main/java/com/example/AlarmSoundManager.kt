package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*

object AlarmSoundManager {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volumeJob: Job? = null

    fun start(context: Context, vibrate: Boolean = true, gradual: Boolean = false) {
        if (mediaPlayer != null) return // Already ringing

        try {
            val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            val preferredSoundStr = prefs.getString("preferred_alarm_sound", null)
            var alert: Uri? = null
            
            if (!preferredSoundStr.isNullOrEmpty()) {
                try {
                    alert = Uri.parse(preferredSoundStr)
                } catch (e: Exception) {
                    Log.e("AlarmSoundManager", "Error parsing saved preferred sound URI: $preferredSoundStr", e)
                }
            }
            
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (alert == null) {
                    alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    if (alert == null) {
                        alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }
                }
            }

            if (alert != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, alert)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    if (gradual) {
                        setVolume(0f, 0f)
                    } else {
                        setVolume(1f, 1f)
                    }
                    start()
                }
                
                if (gradual) {
                    volumeJob?.cancel()
                    volumeJob = CoroutineScope(Dispatchers.Default).launch {
                        var vol = 0f
                        while (vol < 1.0f && mediaPlayer != null) {
                            delay(1000)
                            vol += 0.08f
                            if (vol > 1.0f) vol = 1.0f
                            try {
                                mediaPlayer?.setVolume(vol, vol)
                            } catch (e: Exception) {
                                break
                            }
                        }
                    }
                }
            } else {
                Log.e("AlarmSoundManager", "No default alarm, ringtone, or notification sound found!")
            }

            if (vibrate) {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                val pattern = longArrayOf(0, 600, 600)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
            Log.d("AlarmSoundManager", "Started alarm sound and vibration")
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "Error playing alarm sound", e)
        }
    }

    fun stop() {
        try {
            volumeJob?.cancel()
            volumeJob = null
            
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null

            vibrator?.cancel()
            vibrator = null
            Log.d("AlarmSoundManager", "Stopped alarm sound and vibration")
        } catch (e: Exception) {
            Log.e("AlarmSoundManager", "Error stopping alarm sound", e)
        }
    }

    fun isRinging(): Boolean {
        return mediaPlayer != null
    }
}
