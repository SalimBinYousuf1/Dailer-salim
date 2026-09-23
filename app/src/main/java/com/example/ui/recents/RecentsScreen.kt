package com.example.ui.recents

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.SquirclePillShape
import com.example.ui.components.SquircleSheetShape
import com.example.ui.components.SquircleSmallCardShape
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DestructiveRed
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
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top Header Row with Apple segmented control & actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recents",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Export Button
                IconButton(
                    onClick = { viewModel.exportToCsv() },
                    modifier = Modifier.testTag("export_recents_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export CSV",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Multi-select Edit toggle
                Text(
                    text = if (uiState.isMultiSelectMode) "Done" else "Edit",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(SquirclePillShape)
                        .clickable { viewModel.toggleMultiSelectMode() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("recents_edit_button")
                )
            }
        }

        // Segmented Filter Tabs: All, Missed, Outgoing, Incoming
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(SquirclePillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val filters = listOf(
                "All" to null,
                "Missed" to CallType.MISSED,
                "Outgoing" to CallType.OUTGOING,
                "Incoming" to CallType.INCOMING
            )

            filters.forEach { (label, type) ->
                val isSelected = uiState.activeFilter == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(SquirclePillShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { viewModel.setFilter(type) }
                        .padding(vertical = 6.dp)
                        .testTag("filter_${label.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // Multi-select action bar
        AnimatedVisibility(visible = uiState.isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.selectAll() }) {
                    Text("Select All", color = AccentGreen)
                }

                Text(
                    text = "${uiState.selectedEntryIds.size} Selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = { viewModel.requestBulkDelete() },
                    enabled = uiState.selectedEntryIds.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(contentColor = DestructiveRed)
                ) {
                    Text("Delete")
                }
            }
        }

        // Call Log List or Typographic Empty State
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
                        text = "No Call History",
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
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                items(uiState.callLogs, key = { it.id }) { entry ->
                    val isSelected = uiState.selectedEntryIds.contains(entry.id)
                    CallLogRow(
                        entry = entry,
                        timeFormat = timeFormat,
                        dateFormat = dateFormat,
                        isMultiSelect = uiState.isMultiSelectMode,
                        isSelected = isSelected,
                        onClick = { viewModel.onEntryClicked(entry) },
                        onLongClick = { viewModel.onEntryLongPressed(entry) }
                    )
                }
            }
        }
    }

    // Long Press Bottom Sheet
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
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = entry.name ?: entry.number,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (entry.name != null) {
                    Text(
                        text = entry.number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Call
                BottomSheetAction(
                    icon = Icons.Default.Call,
                    text = "Call ${entry.number}",
                    onClick = {
                        viewModel.dismissSheet()
                        viewModel.onEntryClicked(entry)
                    }
                )

                // Message
                BottomSheetAction(
                    icon = Icons.Default.Message,
                    text = "Send Message",
                    onClick = {
                        viewModel.dismissSheet()
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${entry.number}"))
                        context.startActivity(intent)
                    }
                )

                // Add to contacts
                BottomSheetAction(
                    icon = Icons.Default.PersonAdd,
                    text = "Add / View Contact",
                    onClick = {
                        viewModel.dismissSheet()
                        onAddContactClicked(entry.number)
                    }
                )

                // Copy Number
                BottomSheetAction(
                    icon = Icons.Default.ContentCopy,
                    text = "Copy Number",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Number", entry.number))
                        viewModel.dismissSheet()
                    }
                )

                // Block Number
                BottomSheetAction(
                    icon = Icons.Default.Block,
                    text = "Block Number",
                    onClick = {
                        viewModel.blockNumber(entry.number)
                    }
                )

                // Delete Entry
                BottomSheetAction(
                    icon = Icons.Default.Delete,
                    text = "Delete From Call Log",
                    isDestructive = true,
                    onClick = {
                        viewModel.requestDeleteSingle(entry)
                    }
                )
            }
        }
    }

    // Single Delete Confirmation Dialog
    if (uiState.showDeleteConfirmDialog && uiState.selectedEntryForSheet != null) {
        val entry = uiState.selectedEntryForSheet!!
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteSingle() },
            title = { Text("Delete Call Log") },
            text = { Text("Are you sure you want to delete call history for ${entry.name ?: entry.number}?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteSingle() },
                    colors = ButtonDefaults.textButtonColors(contentColor = DestructiveRed)
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

    // Bulk Delete Confirmation Dialog
    if (uiState.showBulkDeleteConfirmDialog) {
        val count = uiState.selectedEntryIds.size
        AlertDialog(
            onDismissRequest = { viewModel.cancelBulkDelete() },
            title = { Text("Delete $count Entries") },
            text = { Text("Are you sure you want to permanently delete $count call log entries?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmBulkDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = DestructiveRed)
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

    // Exported file notice
    uiState.exportedFile?.let { file ->
        AlertDialog(
            onDismissRequest = { viewModel.clearExportNotice() },
            title = { Text("Call Log Exported") },
            text = { Text("Exported ${file.name} to local storage cache successfully.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearExportNotice() }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallLogRow(
    entry: CallLogEntry,
    timeFormat: SimpleDateFormat,
    dateFormat: SimpleDateFormat,
    isMultiSelect: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val date = Date(entry.timestamp)
    val formattedTime = timeFormat.format(date)
    val formattedDate = dateFormat.format(date)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("call_log_${entry.id}")
            .semantics {
                contentDescription = "${entry.type.name} call from ${entry.name ?: entry.number}, $formattedDate $formattedTime"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelect) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            CallTypeIcon(type = entry.type, size = 18.dp)

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.name ?: entry.number,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (entry.type == CallType.MISSED) DestructiveRed else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (entry.count > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${entry.count})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (entry.name != null) {
                    Text(
                        text = entry.number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tabular figures for timestamp to prevent jitter
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Default
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BottomSheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleSmallCardShape)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) DestructiveRed else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) DestructiveRed else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isDestructive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
