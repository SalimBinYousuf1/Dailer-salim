package com.example.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.SalimApplication
import com.example.data.model.CannedMessage
import com.example.ui.components.SquircleCardShape
import com.example.ui.components.SquircleSmallCardShape
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DestructiveRed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SalimApplication
    val preferences = app.preferences
    val db = app.database
    val telephonyRepo = app.telephonyRepository
    val scope = rememberCoroutineScope()

    val vibrationEnabled by preferences.vibrationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val cannedMessages by db.cannedMessageDao().getAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val activeSubscriptions = remember { telephonyRepo.getActiveSubscriptions() }

    var showAddCannedDialog by remember { mutableStateOf(false) }
    var newCannedText by remember { mutableStateOf("") }
    var backupNotice by remember { mutableStateOf<String?>(null) }
    var recordingDisclaimerNotice by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Haptics & Feedback Section
            SettingsSectionHeader(title = "HAPTICS & FEEDBACK")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = SquircleCardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Tactile Haptics",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Keypad press and gesture feedback",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { scope.launch { preferences.setVibrationEnabled(it) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            checkedTrackColor = AccentGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dual-SIM Section (if applicable)
            if (activeSubscriptions.size > 1) {
                SettingsSectionHeader(title = "DUAL SIM PREFERENCES")
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = SquircleCardShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SimCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Active SIMs Detected (${activeSubscriptions.size})",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A SIM selection prompt will be shown for outgoing calls when no contact preference is set.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Quick-Decline Canned SMS Messages
            SettingsSectionHeader(title = "RESPOND WITH TEXT (QUICK DECLINE)")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = SquircleCardShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    cannedMessages.forEach { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\"${msg.text}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { scope.launch { db.cannedMessageDao().deleteById(msg.id) } },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SquircleSmallCardShape)
                            .clickable { showAddCannedDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add Custom Message",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Call Screening & Blocking
            SettingsSectionHeader(title = "CALL SCREENING & BLOCKING")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = SquircleCardShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBlocked() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Blocked Numbers",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Manage rejected callers and private numbers",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Call Recording (honest disclaimer)
            SettingsSectionHeader(title = "CALL RECORDING")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = SquircleCardShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { recordingDisclaimerNotice = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Call Recording Availability",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Subject to device OEM and regional telecom laws",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Backup & Restore
            SettingsSectionHeader(title = "DATA & BACKUP")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = SquircleCardShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            try {
                                val speedDials = db.speedDialDao().getAllSpeedDials().first()
                                val msgs = db.cannedMessageDao().getAll().first()
                                val json = buildString {
                                    append("{\n")
                                    append("  \"speedDials\": [")
                                    append(speedDials.joinToString(",") { "{\"key\":${it.digitKey},\"name\":\"${it.contactName}\",\"number\":\"${it.phoneNumber}\"}" })
                                    append("],\n")
                                    append("  \"cannedMessages\": [")
                                    append(msgs.joinToString(",") { "\"${it.text.replace("\"", "\\\"")}\"" })
                                    append("]\n")
                                    append("}")
                                }
                                val file = File(context.cacheDir, "salim_settings_backup.json")
                                file.writeText(json)
                                backupNotice = "Settings and Speed Dials backed up locally to ${file.name}"
                            } catch (e: Exception) {
                                backupNotice = "Backup failed: ${e.message}"
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Export Settings Backup",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Exports speed dials and quick decline messages as JSON",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Add Canned Message Dialog
    if (showAddCannedDialog) {
        AlertDialog(
            onDismissRequest = { showAddCannedDialog = false },
            title = { Text("Add Quick-Decline Message") },
            text = {
                OutlinedTextField(
                    value = newCannedText,
                    onValueChange = { newCannedText = it },
                    label = { Text("Message Text") },
                    shape = SquircleSmallCardShape,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCannedText.isNotBlank()) {
                            scope.launch {
                                db.cannedMessageDao().insert(CannedMessage(text = newCannedText.trim()))
                                newCannedText = ""
                                showAddCannedDialog = false
                            }
                        }
                    }
                ) {
                    Text("Add", color = AccentGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCannedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Recording Disclaimer Dialog
    if (recordingDisclaimerNotice) {
        AlertDialog(
            onDismissRequest = { recordingDisclaimerNotice = false },
            title = { Text("Call Recording Notice") },
            text = {
                Text(
                    "Call recording APIs on modern Android require system-level OEM privileges and compliance with local wiretapping and consent regulations. Salim Phone does not attempt unauthorized audio interception. Where your carrier and Android OS provide hardware recording integration, it will appear directly in the active call screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { recordingDisclaimerNotice = false }) {
                    Text("Understood", color = AccentGreen)
                }
            }
        )
    }

    // Backup Notice Dialog
    backupNotice?.let { msg ->
        AlertDialog(
            onDismissRequest = { backupNotice = null },
            title = { Text("Backup Status") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { backupNotice = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}
