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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleDarkKeypad
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleKeypadPressedDark
import com.example.ui.theme.AppleKeypadPressedLight
import com.example.ui.theme.AppleLightGray

data class KeypadKey(
    val mainDigit: Char,
    val letters: String,
    val longPressChar: Char? = null,
    val speedDialKey: Int? = null
)

val AppleKeypadMatrix = listOf(
    listOf(
        KeypadKey('1', "", speedDialKey = 1),
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
fun AppleKeypadButton(
    key: KeypadKey,
    onKeyPressed: (Char) -> Unit,
    onKeyLongPressed: ((KeypadKey) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "key_scale"
    )

    val isDark = MaterialTheme.colorScheme.background == Color.Black || MaterialTheme.colorScheme.surface == Color.Black

    val defaultBg = if (isDark) AppleDarkKeypad else AppleLightGray
    val pressedBg = if (isDark) AppleKeypadPressedDark else AppleKeypadPressedLight

    Box(
        modifier = modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isPressed) pressedBg else defaultBg)
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
                fontSize = 34.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 36.sp
            )

            if (key.letters.isNotBlank()) {
                Text(
                    text = key.letters,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.6.sp
                )
            } else if (key.mainDigit == '1') {
                // Apple voicemail glyph
                Text(
                    text = "⚲",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppleKeypadMatrix.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    AppleKeypadButton(
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

            // Action row: Left blank spacer (76dp), Center Apple Green Call Button (76dp), Right Backspace (76dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left spacer for perfect Apple 3-column symmetry
                Box(modifier = Modifier.size(76.dp))

                // Primary iOS Green Call Button
                var callPressed by remember { mutableStateOf(false) }
                val callScale by animateFloatAsState(
                    targetValue = if (callPressed) 0.90f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "call_btn_scale"
                )

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(callScale)
                        .clip(CircleShape)
                        .background(AppleGreen)
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
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Right Backspace Button
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
