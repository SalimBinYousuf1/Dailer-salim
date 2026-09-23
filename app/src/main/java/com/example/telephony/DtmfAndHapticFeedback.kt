package com.example.telephony

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class DtmfAndHapticFeedback(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 75)
        } catch (e: Exception) {
            Log.e("DtmfHaptic", "Could not initialize ToneGenerator", e)
        }
    }

    fun playTone(char: Char, durationMs: Int = 120) {
        val tone = when (char) {
            '0' -> ToneGenerator.TONE_DTMF_0
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> -1
        }
        if (tone != -1) {
            try {
                toneGenerator?.startTone(tone, durationMs)
            } catch (e: Exception) {
                Log.e("DtmfHaptic", "Error playing tone", e)
            }
        }
    }

    fun triggerKeyHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12L)
            }
        } catch (e: Exception) {
            // Ignored if permissions or hardware lack vibrator
        }
    }

    fun triggerCommitHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35L)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun triggerDestructiveHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 20, 40, 20), -1)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.e("DtmfHaptic", "Error releasing ToneGenerator", e)
        }
    }
}
