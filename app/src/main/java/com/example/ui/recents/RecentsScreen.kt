package com.example.ui.recents

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallLogEntry
import com.example.data.model.CallType
import com.example.ui.components.CallTypeIcon
import com.example.ui.components.SquircleSheetShape
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    viewModel: RecentsViewModel,
    onAddContactClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // iOS Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit Button (Apple Blue)
            Text(
                text = if (uiState.isMultiSelectMode) "Done" else "Edit",
                style = MaterialTheme.typography.bodyLarge,
                color = AppleBlue,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { viewModel.toggleMultiSelectMode() }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .testTag("recents_edit_button")
            )

            // Centered iOS Segmented Picker: [ All | Missed ]
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF767680).copy(alpha = 0.12f))
                    .padding(2.dp)
            ) {
                val isAllSelected = uiState.activeFilter == null
                val isMissedSelected = uiState.activeFilter == CallType.MISSED

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isAllSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { viewModel.setFilter(null) }
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .testTag("filter_all"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isMissedSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable { viewModel.setFilter(CallType.MISSED) }
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .testTag("filter_missed"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Missed",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isMissedSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Export CSV action
            IconButton(
                onClick = { viewModel.exportToCsv() },
                modifier = Modifier.size(32.dp).testTag("export_recents_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export CSV",
                    tint = AppleBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Apple Large Title: "Recents"
        Text(
            text = "Recents",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
        )

        // Multi-select bulk action bar
        AnimatedVisibility(visible = uiState.isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.selectAll() }) {
                    Text("Select All", color = AppleBlue)
                }

                Text(
                    text = "${uiState.selectedEntryIds.size} Selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = { viewModel.requestBulkDelete() },
                    enabled = uiState.selectedEntryIds.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleRed)
                ) {
                    Text("Delete")
                }
            }
        }

        // Call Log List
        if (uiState.callLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Recents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Calls made and received will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("M/d/yy", Locale.getDefault())

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(uiState.callLogs, key = { it.id }) { entry ->
                    val isSelected = uiState.selectedEntryIds.contains(entry.id)
                    AppleCallLogRow(
                        entry = entry,
                        timeFormat = timeFormat,
                        dateFormat = dateFormat,
                        isMultiSelect = uiState.isMultiSelectMode,
                        isSelected = isSelected,
                        onClick = { viewModel.onEntryClicked(entry) },
                        onInfoClick = { viewModel.onEntryLongPressed(entry) },
                        onDeleteClick = { viewModel.requestDeleteSingle(entry) }
                    )
                }
            }
        }
    }

    // iOS Contact / Call Detail Sheet
    uiState.selectedEntryForSheet?.let { entry ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSheet() },
            sheetState = sheetState,
            shape = SquircleSheetShape,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered large avatar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E5EA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (entry.name ?: entry.number).firstOrNull()?.uppercase() ?: "#",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = entry.name ?: entry.number,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (entry.name != null) {
                    Text(
                        text = entry.number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Apple 4 Action Pills Row: message, call, video, info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AppleActionPill(
                        icon = Icons.Default.Message,
                        label = "message",
                        onClick = {
                            viewModel.dismissSheet()
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${entry.number}"))
                            context.startActivity(intent)
                        }
                    )
                    AppleActionPill(
                        icon = Icons.Default.Call,
                        label = "call",
                        onClick = {
                            viewModel.dismissSheet()
                            viewModel.onEntryClicked(entry)
                        }
                    )
                    AppleActionPill(
                        icon = Icons.Default.PersonAdd,
                        label = "add",
                        onClick = {
                            viewModel.dismissSheet()
                            onAddContactClicked(entry.number)
                        }
                    )
                    AppleActionPill(
                        icon = Icons.Default.ContentCopy,
                        label = "copy",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Number", entry.number))
                            viewModel.dismissSheet()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Block Caller & Delete
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.blockNumber(entry.number) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = AppleRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Block this Caller",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppleRed,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 50.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.requestDeleteSingle(entry) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = AppleRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Delete from Recents",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppleRed,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialogs
    if (uiState.showDeleteConfirmDialog && uiState.selectedEntryForSheet != null) {
        val entry = uiState.selectedEntryForSheet!!
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteSingle() },
            title = { Text("Delete Call") },
            text = { Text("Delete call record for ${entry.name ?: entry.number}?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteSingle() },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteSingle() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showBulkDeleteConfirmDialog) {
        val count = uiState.selectedEntryIds.size
        AlertDialog(
            onDismissRequest = { viewModel.cancelBulkDelete() },
            title = { Text("Delete $count Calls") },
            text = { Text("Are you sure you want to permanently delete $count calls from history?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBulkDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleRed)
                ) {
                    Text("Delete $count")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelBulkDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }

    uiState.exportedFile?.let { file ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExportNotice() },
            title = { Text("History Exported") },
            text = { Text("Exported ${file.name} to local storage.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearExportNotice() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun AppleCallLogRow(
    entry: CallLogEntry,
    timeFormat: SimpleDateFormat,
    dateFormat: SimpleDateFormat,
    isMultiSelect: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val date = Date(entry.timestamp)
    val formattedTime = timeFormat.format(date)
    val isMissed = entry.type == CallType.MISSED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("call_log_${entry.id}")
            .semantics {
                contentDescription = "${entry.name ?: entry.number}, $formattedTime"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelect) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) AppleBlue else Color(0xFFC7C7CC),
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 4.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.name ?: entry.number,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isMissed) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isMissed) AppleRed else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (entry.count > 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${entry.count})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CallTypeIcon(type = entry.type, size = 13.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (entry.name != null) "mobile" else entry.type.name.lowercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timestamp (Apple Gray)
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Default),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(10.dp))

            // iOS ⓘ info button (Apple Blue)
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("info_button_${entry.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Details",
                    tint = AppleBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Apple inset divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )
    }
}

@Composable
private fun AppleActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AppleBlue,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
