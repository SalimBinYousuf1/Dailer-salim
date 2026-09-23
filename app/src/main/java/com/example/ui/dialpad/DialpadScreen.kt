package com.example.ui.dialpad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DefaultDialerBanner
import com.example.ui.components.SalimKeypad
import com.example.ui.components.SimPickerBottomSheet
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleLightGray
import com.example.util.T9Search

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadScreen(
    viewModel: DialpadViewModel,
    onRequestDefaultDialer: () -> Unit,
    onAddContactClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Default Dialer Warning Banner if not set
        DefaultDialerBanner(
            isDefault = uiState.isDefaultDialer,
            onRequestDefault = onRequestDefaultDialer
        )

        // Upper Section: T9 Matches, Number Display, Add Number button
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // T9 Matches Strip (Apple sleek pill row)
            AnimatedVisibility(
                visible = uiState.t9Matches.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(uiState.t9Matches, key = { it.contact.id }) { match ->
                        T9MatchPill(
                            match = match,
                            onClick = {
                                val number = match.matchedNumber ?: match.contact.phoneNumbers.firstOrNull() ?: uiState.enteredNumber
                                viewModel.initiateCall(number)
                            }
                        )
                    }
                }
            }

            // Display entered phone number with Apple typography
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            if (uiState.enteredNumber.isNotBlank()) {
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Phone Number", uiState.enteredNumber))
                            } else {
                                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrBlank()) {
                                    viewModel.onPasteNumber(clip)
                                }
                            }
                        }
                    )
                    .testTag("dialpad_number_display"),
                contentAlignment = Alignment.Center
            ) {
                val displayText = if (uiState.formattedNumber.isNotBlank()) {
                    uiState.formattedNumber
                } else {
                    ""
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Default,
                        letterSpacing = (-0.5).sp
                    ),
                    fontSize = if (displayText.length > 13) 26.sp else if (displayText.length > 9) 32.sp else 38.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // "Add Number" prompt (Apple Blue)
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.enteredNumber.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAddContactClicked(uiState.enteredNumber) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("add_number_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Number",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppleBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Apple 76dp circular keypad
        SalimKeypad(
            onDigitClicked = { viewModel.onDigitPressed(it) },
            onSpeedDialTriggered = { viewModel.onSpeedDialTriggered(it) },
            onVoicemailTriggered = { viewModel.onVoicemailTriggered() },
            onCallClicked = { viewModel.onCallButtonClicked() },
            onBackspaceClicked = { viewModel.onBackspace() },
            onBackspaceLongClicked = { viewModel.onBackspaceLong() },
            showActionRow = true,
            hasDigits = uiState.enteredNumber.isNotEmpty(),
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }

    // Dual-SIM Bottom Sheet
    uiState.showSimPickerForNumber?.let { number ->
        SimPickerBottomSheet(
            subscriptions = uiState.activeSubscriptions,
            targetNumber = number,
            onSimSelected = { viewModel.onSimSelected(it) },
            onDismiss = { viewModel.dismissSimPicker() }
        )
    }
}

@Composable
private fun T9MatchPill(
    match: T9Search.T9MatchResult,
    onClick: () -> Unit
) {
    val contact = match.contact
    val name = contact.displayName

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clickable { onClick() }
            .testTag("t9_match_${contact.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AppleLightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "#",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val annotatedName = buildAnnotatedString {
                if (match.matchedInName && match.matchStartIndex in 0..name.length && match.matchEndIndex in match.matchStartIndex..name.length) {
                    append(name.substring(0, match.matchStartIndex))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = AppleBlue)) {
                        append(name.substring(match.matchStartIndex, match.matchEndIndex))
                    }
                    append(name.substring(match.matchEndIndex))
                } else {
                    append(name)
                }
            }

            Text(
                text = annotatedName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call",
                tint = AppleGreen,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
