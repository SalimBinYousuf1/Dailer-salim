package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen

data class KeypadKey(
    val mainDigit: Char,
    val letters: String,
    val longPressChar: Char? = null,
    val speedDialKey: Int? = null
)

val KeypadMatrix = listOf(
    listOf(
        KeypadKey('1', " ", speedDialKey = 1),
        KeypadKey('2', "A B C", speedDialKey = 2),
        KeypadKey('3', "D E F", speedDialKey = 3)
    ),
    listOf(
        KeypadKey('4', "G H I", speedDialKey = 4),
        KeypadKey('5', "J K L", speedDialKey = 5),
        KeypadKey('6', "M N O", speedDialKey = 6)
    ),
    listOf(
        KeypadKey('7', "P Q R S", speedDialKey = 7),
        KeypadKey('8', "T U V", speedDialKey = 8),
        KeypadKey('9', "W X Y Z", speedDialKey = 9)
    ),
    listOf(
        KeypadKey('*', ",", longPressChar = ','),
        KeypadKey('0', "+", longPressChar = '+'),
        KeypadKey('#', ";", longPressChar = ';')
    )
)

@Composable
fun SalimKeypadButton(
    key: KeypadKey,
    onKeyPressed: (Char) -> Unit,
    onKeyLongPressed: ((KeypadKey) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "key_scale"
    )

    val keyBackground = if (isPressed) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(keyBackground)
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onKeyPressed(key.mainDigit)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onLongPress = {
                        onKeyLongPressed?.invoke(key)
                    }
                )
            }
            .testTag("key_${key.mainDigit}")
            .semantics {
                role = Role.Button
                contentDescription = if (key.letters.isNotBlank()) {
                    "${key.mainDigit}, ${key.letters}"
                } else {
                    "${key.mainDigit}"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = key.mainDigit.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (key.letters.isNotBlank()) {
                Text(
                    text = key.letters,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp
                )
            } else if (key.mainDigit == '1') {
                // Subtle symbol / icon space for voicemail key
                Text(
                    text = "⚲",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SalimKeypad(
    onDigitClicked: (Char) -> Unit,
    onSpeedDialTriggered: (Int) -> Unit,
    onVoicemailTriggered: () -> Unit,
    onCallClicked: () -> Unit,
    onBackspaceClicked: () -> Unit,
    onBackspaceLongClicked: () -> Unit,
    showActionRow: Boolean = true,
    hasDigits: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeypadMatrix.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    SalimKeypadButton(
                        key = key,
                        onKeyPressed = { onDigitClicked(it) },
                        onKeyLongPressed = {
                            when {
                                key.mainDigit == '1' -> onVoicemailTriggered()
                                key.speedDialKey != null && key.speedDialKey in 2..9 -> onSpeedDialTriggered(key.speedDialKey)
                                key.longPressChar != null -> onDigitClicked(key.longPressChar)
                            }
                        }
                    )
                }
            }
        }

        if (showActionRow) {
            Spacer(modifier = Modifier.height(10.dp))

            // Action row: Left spacer/action, Call button (singular focal point), Backspace
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left placeholder for symmetry and spacing discipline
                Box(modifier = Modifier.size(76.dp))

                // Primary Call Button (The singular focal point)
                var callPressed by remember { mutableStateOf(false) }
                val callScale by animateFloatAsState(
                    targetValue = if (callPressed) 0.90f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "call_btn_scale"
                )

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(callScale)
                        .clip(CircleShape)
                        .background(AccentGreen)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    callPressed = true
                                    tryAwaitRelease()
                                    callPressed = false
                                    onCallClicked()
                                }
                            )
                        }
                        .testTag("call_button")
                        .semantics {
                            role = Role.Button
                            contentDescription = "Place call"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Backspace button
                Box(
                    modifier = Modifier.size(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasDigits) {
                        IconButton(
                            onClick = onBackspaceClicked,
                            modifier = Modifier
                                .size(56.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onBackspaceClicked() },
                                        onLongPress = { onBackspaceLongClicked() }
                                    )
                                }
                                .testTag("backspace_button")
                                .semantics { contentDescription = "Delete digit" }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
