package com.example.ui.call

import android.app.KeyguardManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.SalimApplication
import com.example.telephony.CallManager
import com.example.ui.theme.SalimPhoneTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CallActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isNearEar by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure lock-screen display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Setup proximity sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager != null && powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "salim:proximity_wakelock"
            )
        }

        val app = application as SalimApplication
        val db = app.database
        val dtmfHaptic = app.dtmfAndHaptic
        val telephonyRepo = app.telephonyRepository

        setContent {
            SalimPhoneTheme {
                val callState by CallManager.callState.collectAsStateWithLifecycle()
                val cannedMessages by db.cannedMessageDao().getAll().collectAsStateWithLifecycle(initialValue = emptyList())

                // Auto-finish activity if call disconnected
                LaunchedEffect(callState.hasCall, callState.callState) {
                    if (!callState.hasCall || callState.callState == Call.STATE_DISCONNECTED) {
                        finish()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (callState.callState == Call.STATE_RINGING) {
                        IncomingCallView(
                            callState = callState,
                            cannedMessages = cannedMessages,
                            dtmfHaptic = dtmfHaptic,
                            onAnswer = { CallManager.answerCall() },
                            onDecline = { CallManager.disconnectCall() },
                            onQuickDeclineWithMessage = { text ->
                                telephonyRepo.sendCannedSms(callState.number, text)
                                CallManager.disconnectCall()
                            }
                        )
                    } else {
                        InCallView(
                            callState = callState,
                            dtmfHaptic = dtmfHaptic,
                            onMuteToggle = { CallManager.setMuted(it) },
                            onSpeakerToggle = { CallManager.setSpeakerphone(it) },
                            onHoldToggle = { CallManager.setHold(it) },
                            onDtmfDigit = { CallManager.playDtmf(it) },
                            onEndCall = { CallManager.disconnectCall() },
                            onAddCall = {
                                // Returns to main dialer to initiate second call
                                finish()
                            }
                        )
                    }

                    // Proximity blackout overlay if near ear
                    if (isNearEar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = proximitySensor?.maximumRange ?: 5f
            val near = distance < 4f && distance < maxRange
            isNearEar = near

            if (near) {
                if (wakeLock?.isHeld == false) {
                    wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes */)
                }
            } else {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Silence ringer on hardware volume press during incoming call
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val state = CallManager.callState.value
            if (state.callState == Call.STATE_RINGING) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
