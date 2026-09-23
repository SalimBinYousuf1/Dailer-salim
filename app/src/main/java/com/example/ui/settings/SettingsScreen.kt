package com.example.ui.settings

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.SalimApplication
import com.example.data.model.CannedMessage
import com.example.ui.components.AppleGroupedRow
import com.example.ui.components.AppleGroupedSection
import com.example.ui.components.AppleGroupedSwitchRow
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleRed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SalimApplication
    val prefs = app.preferences
    val db = app.database
    val telephonyRepo = app.telephonyRepository
    val scope = rememberCoroutineScope()

    var hapticsEnabled by remember { mutableStateOf(true) }
    var dialTonesEnabled by remember { mutableStateOf(true) }
    var blockUnknown by remember { mutableStateOf(false) }
    var isDefaultDialer by remember { mutableStateOf(false) }
    var blockedCount by remember { mutableStateOf(0) }

    val cannedMessages by db.cannedMessageDao().getAll().collectAsStateWithLifecycle(initialValue = emptyList())
    var editingMessage by remember { mutableStateOf<CannedMessage?>(null) }
    var editingText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        hapticsEnabled = prefs.vibrationEnabled.first()
        dialTonesEnabled = prefs.dialTonesEnabled.first()
        blockUnknown = prefs.blockUnknownNumbers.first()
        isDefaultDialer = telephonyRepo.isDefaultDialer()
        blockedCount = app.blockedNumbersRepository.getBlockedNumbers().size
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // iOS Navigation Bar: [ Back (chevron + "Settings") ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppleBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Apple Large Title: "Settings"
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 12.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // Section 1: General Telephony
            AppleGroupedSection(
                title = "Phone & System Integration",
                footer = "Salim handles native calls, screening, and system telecom intents."
            ) {
                AppleGroupedRow(
                    title = "Default Phone App",
                    subtitle = if (isDefaultDialer) "Salim is currently active" else "Not set as default",
                    value = if (isDefaultDialer) "Active" else "Set Default",
                    icon = Icons.Default.Phone,
                    iconTint = Color.White,
                    iconBackground = AppleGreen,
                    showChevron = !isDefaultDialer,
                    onClick = {
                        if (!isDefaultDialer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                            val intent = roleManager?.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                            if (intent != null) context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Salim is default dialer", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                AppleGroupedSwitchRow(
                    title = "Silence Unknown Callers",
                    subtitle = "Calls from numbers not in your contacts will be screened and silenced.",
                    checked = blockUnknown,
                    onCheckedChange = { checked ->
                        blockUnknown = checked
                        scope.launch { prefs.setBlockUnknownNumbers(checked) }
                    },
                    icon = Icons.Default.Block,
                    iconTint = Color.White,
                    iconBackground = AppleRed,
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 2: Haptics & Audio
            AppleGroupedSection(
                title = "Audio & Keypad Feedback"
            ) {
                AppleGroupedSwitchRow(
                    title = "Keypad Audio Tones",
                    subtitle = "Play DTMF dial tones while typing",
                    checked = dialTonesEnabled,
                    onCheckedChange = { checked ->
                        dialTonesEnabled = checked
                        scope.launch { prefs.setDialTonesEnabled(checked) }
                    },
                    icon = Icons.Default.VolumeUp,
                    iconTint = Color.White,
                    iconBackground = AppleBlue
                )

                AppleGroupedSwitchRow(
                    title = "Keypad Haptic Feedback",
                    subtitle = "Tactile vibration impulses on keypresses",
                    checked = hapticsEnabled,
                    onCheckedChange = { checked ->
                        hapticsEnabled = checked
                        scope.launch { prefs.setVibrationEnabled(checked) }
                    },
                    icon = Icons.Default.Vibration,
                    iconTint = Color.White,
                    iconBackground = Color(0xFFFF9500),
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Call Screening & Blocking
            AppleGroupedSection(
                title = "Calls & Identification"
            ) {
                AppleGroupedRow(
                    title = "Blocked Contacts",
                    subtitle = "Manage blocked phone numbers",
                    value = if (blockedCount > 0) "$blockedCount" else null,
                    icon = Icons.Default.Block,
                    iconTint = Color.White,
                    iconBackground = AppleRed,
                    showChevron = true,
                    showDivider = false,
                    onClick = onNavigateToBlocked
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 4: Respond With Text (Canned SMS)
            AppleGroupedSection(
                title = "Respond with Text",
                footer = "These quick decline messages appear during incoming calls."
            ) {
                cannedMessages.forEachIndexed { index, msg ->
                    AppleGroupedRow(
                        title = msg.text,
                        showChevron = true,
                        showDivider = index < cannedMessages.size - 1,
                        onClick = {
                            editingMessage = msg
                            editingText = msg.text
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Edit canned message dialog
    editingMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit Quick Message") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            db.cannedMessageDao().update(msg.copy(text = editingText.trim()))
                            editingMessage = null
                        }
                    }
                ) {
                    Text("Save", color = AppleBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMessage = null }) {
                    Text("Cancel", color = AppleBlue)
                }
            }
        )
    }
}
