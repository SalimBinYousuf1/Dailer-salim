package com.example.ui.call

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telephony.ActiveCallUiState
import com.example.telephony.DtmfAndHapticFeedback
import com.example.ui.components.SalimKeypad
import com.example.ui.components.SquircleAvatarShape
import com.example.ui.theme.DestructiveRed
import kotlinx.coroutines.delay

@Composable
fun InCallView(
    callState: ActiveCallUiState,
    dtmfHaptic: DtmfAndHapticFeedback,
    onMuteToggle: (Boolean) -> Unit,
    onSpeakerToggle: (Boolean) -> Unit,
    onHoldToggle: (Boolean) -> Unit,
    onDtmfDigit: (Char) -> Unit,
    onEndCall: () -> Unit,
    onAddCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showInCallKeypad by remember { mutableStateOf(false) }

    // Call duration timer (calculated from real connectTimeMillis)
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(callState.connectTimeMillis, callState.callState) {
        while (callState.callState == Call.STATE_ACTIVE && callState.connectTimeMillis > 0L) {
            val now = System.currentTimeMillis()
            elapsedSeconds = ((now - callState.connectTimeMillis) / 1000L).coerceAtLeast(0L)
            delay(1000)
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    val isSpeakerOn = (callState.audioRoute and CallAudioState.ROUTE_SPEAKER) != 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Caller identity + Call state / duration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(SquircleAvatarShape)
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = callState.displayName?.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = callState.displayName ?: (if (callState.number.isNotBlank()) callState.number else "Unknown"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (callState.displayName != null && callState.number.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = callState.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAAAAA)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val statusText = when {
                callState.isOnHold -> "On Hold"
                callState.callState == Call.STATE_DIALING -> "Calling..."
                callState.callState == Call.STATE_CONNECTING -> "Connecting..."
                callState.callState == Call.STATE_ACTIVE -> formattedDuration
                else -> "Active Call"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Default),
                color = if (callState.isOnHold) Color(0xFFFF9500) else Color.White,
                fontWeight = FontWeight.Normal
            )
        }

        // Center: Either 6-grid In-Call Controls OR In-Call Keypad (for IVR navigation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !showInCallKeypad,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // Apple 6-grid controls: Mute, Keypad, Audio/Speaker, Add Call, Hold, Contacts
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallControlButton(
                            icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (callState.isMuted) "unmute" else "mute",
                            isActive = callState.isMuted,
                            onClick = {
                                dtmfHaptic.triggerKeyHaptic()
                                onMuteToggle(!callState.isMuted)
                            }
                        )

                        InCallControlButton(
                            icon = Icons.Default.Dialpad,
                            label = "keypad",
                            isActive = false,
                            onClick = {
                                dtmfHaptic.triggerKeyHaptic()
                                showInCallKeypad = true
                            }
                        )

                        InCallControlButton(
                            icon = Icons.Default.VolumeUp,
                            label = "speaker",
                            isActive = isSpeakerOn,
                            onClick = {
                                dtmfHaptic.triggerKeyHaptic()
                                onSpeakerToggle(!isSpeakerOn)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallControlButton(
                            icon = Icons.Default.Add,
                            label = "add call",
                            isActive = false,
                            onClick = {
                                dtmfHaptic.triggerKeyHaptic()
                                onAddCall()
                            }
                        )

                        InCallControlButton(
                            icon = if (callState.isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                            label = if (callState.isOnHold) "unhold" else "hold",
                            isActive = callState.isOnHold,
                            onClick = {
                                dtmfHaptic.triggerKeyHaptic()
                                onHoldToggle(!callState.isOnHold)
                            }
                        )

                        // Placeholder for symmetry
                        Box(modifier = Modifier.size(72.dp))
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showInCallKeypad,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SalimKeypad(
                        onDigitClicked = { digit ->
                            onDtmfDigit(digit)
                        },
                        onSpeedDialTriggered = {},
                        onVoicemailTriggered = {},
                        onCallClicked = {},
                        onBackspaceClicked = {},
                        onBackspaceLongClicked = {},
                        showActionRow = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Hide Keypad",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showInCallKeypad = false }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Bottom singular focal point: End Call button (Destructive Red)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(DestructiveRed)
                    .clickable {
                        dtmfHaptic.triggerDestructiveHaptic()
                        onEndCall()
                    }
                    .testTag("in_call_end_button")
                    .semantics {
                        role = Role.Button
                        contentDescription = "End Call"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
private fun InCallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
            .testTag("in_call_control_$label")
            .semantics { contentDescription = label }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White else Color(0xFF242426)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFAAAAAA)
        )
    }
}
