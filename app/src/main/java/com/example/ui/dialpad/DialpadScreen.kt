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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ContactItem
import com.example.ui.components.DefaultDialerBanner
import com.example.ui.components.SalimKeypad
import com.example.ui.components.SimPickerBottomSheet
import com.example.ui.components.SquircleAvatarShape
import com.example.ui.components.SquircleSmallCardShape
import com.example.ui.theme.AccentGreen

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
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Default Dialer Warning Banner if not set
        DefaultDialerBanner(
            isDefault = uiState.isDefaultDialer,
            onRequestDefault = onRequestDefaultDialer
        )

        // Matched contact search preview or Number Display area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.matchedContacts.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    items(uiState.matchedContacts, key = { it.id }) { contact ->
                        MatchedContactRow(
                            contact = contact,
                            onClick = {
                                val number = contact.phoneNumbers.firstOrNull() ?: uiState.enteredNumber
                                viewModel.initiateCall(number)
                            }
                        )
                    }
                }
            }

            // Display formatted entered phone number
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                        letterSpacing = 0.5.sp
                    ),
                    fontSize = if (displayText.length > 12) 28.sp else 34.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Add to contacts affordance
            AnimatedVisibility(
                visible = uiState.enteredNumber.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .clip(SquircleSmallCardShape)
                        .clickable { onAddContactClicked(uiState.enteredNumber) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("add_number_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Contact",
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Number",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // The Keypad with squircle keys and single primary Call button
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
private fun MatchedContactRow(
    contact: ContactItem,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = SquircleSmallCardShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(SquircleSmallCardShape)
            .clickable { onClick() }
            .testTag("matched_contact_${contact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(SquircleAvatarShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.displayName.firstOrNull()?.uppercase() ?: "#",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = contact.phoneNumbers.firstOrNull() ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
