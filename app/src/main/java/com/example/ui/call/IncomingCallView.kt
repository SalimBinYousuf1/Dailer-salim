package com.example.ui.call

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CannedMessage
import com.example.telephony.ActiveCallUiState
import com.example.telephony.DtmfAndHapticFeedback
import com.example.ui.components.SquircleAvatarShape
import com.example.ui.components.SquirclePillShape
import com.example.ui.components.SquircleSheetShape
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DestructiveRed
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingCallView(
    callState: ActiveCallUiState,
    cannedMessages: List<CannedMessage>,
    dtmfHaptic: DtmfAndHapticFeedback,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onQuickDeclineWithMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMessageSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 1:1 finger tracking slider with rubber band physics
    val density = LocalDensity.current
    val sliderWidthPx = with(density) { 260.dp.toPx() }
    val thumbSizePx = with(density) { 68.dp.toPx() }
    val maxDragPx = sliderWidthPx - thumbSizePx

    val offsetX = remember { Animatable(0f) }
    var hasTriggeredThreshold by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Caller Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(SquircleAvatarShape)
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = callState.displayName?.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = callState.displayName ?: (if (callState.number.isNotBlank()) callState.number else "Unknown Caller"),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (callState.displayName != null && callState.number.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = callState.number,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFAAAAAA)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Incoming Call",
                style = MaterialTheme.typography.labelMedium,
                color = AccentGreen,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        // Quick Decline with Message Option
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = { showMessageSheet = true },
                modifier = Modifier.testTag("incoming_message_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "Message",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = "Message",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFAAAAAA)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Slide to Answer Track (Apple 1:1 spring physics slider)
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(72.dp)
                    .clip(SquirclePillShape)
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Background Track Hint
                Text(
                    text = "slide to answer  »",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF888888),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Draggable Thumb Button
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                val target = (offsetX.value + delta).coerceIn(0f, maxDragPx)
                                scope.launch { offsetX.snapTo(target) }

                                if (target >= maxDragPx * 0.85f && !hasTriggeredThreshold) {
                                    hasTriggeredThreshold = true
                                    dtmfHaptic.triggerCommitHaptic()
                                }
                            },
                            onDragStopped = { velocity ->
                                if (offsetX.value >= maxDragPx * 0.85f) {
                                    onAnswer()
                                } else {
                                    hasTriggeredThreshold = false
                                    // Spring back to start
                                    scope.launch {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                                visibilityThreshold = 0.5f
                                            )
                                        )
                                    }
                                }
                            }
                        )
                        .testTag("slide_to_answer_thumb")
                        .semantics { contentDescription = "Slide to answer call" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Decline Button (Destructive Red)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        dtmfHaptic.triggerDestructiveHaptic()
                        onDecline()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DestructiveRed,
                        contentColor = Color.White
                    ),
                    shape = SquirclePillShape,
                    modifier = Modifier
                        .width(280.dp)
                        .height(52.dp)
                        .testTag("incoming_decline_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Decline",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Quick Message Bottom Sheet
    if (showMessageSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showMessageSheet = false },
            sheetState = sheetState,
            shape = SquircleSheetShape,
            containerColor = Color(0xFF1C1C1E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Respond with Text",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                cannedMessages.forEach { msg ->
                    Surface(
                        color = Color(0xFF2C2C2E),
                        shape = SquirclePillShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(SquirclePillShape)
                    ) {
                        Button(
                            onClick = {
                                showMessageSheet = false
                                onQuickDeclineWithMessage(msg.text)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
