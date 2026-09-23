package com.example.ui.blocked

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SalimApplication
import com.example.data.repository.BlockedNumberItem
import com.example.ui.components.AppleGroupedSection
import com.example.ui.components.AppleGroupedSwitchRow
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleRed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun BlockedNumbersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as SalimApplication
    val blockedRepo = app.blockedNumbersRepository
    val prefs = app.preferences
    val scope = rememberCoroutineScope()

    val blockedList = remember { mutableStateListOf<BlockedNumberItem>() }
    var blockUnknown by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newNumberToBlock by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<BlockedNumberItem?>(null) }

    fun refresh() {
        scope.launch {
            blockedList.clear()
            blockedList.addAll(blockedRepo.getBlockedNumbers())
            blockUnknown = prefs.blockUnknownNumbers.first()
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // iOS Navigation Bar: [ Back (left) | + Add (right) ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("blocked_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppleBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            TextButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_blocked_button")
            ) {
                Text("Add New...", color = AppleBlue, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Apple Large Title: "Blocked"
        Text(
            text = "Blocked Contacts",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 12.dp)
        )

        // Switch section: Silence unknown callers
        AppleGroupedSection(
            footer = "You will not receive phone calls, messages, or FaceTime from people on the blocked list."
        ) {
            AppleGroupedSwitchRow(
                title = "Silence Unknown Callers",
                subtitle = "Screen calls from numbers not saved in contacts",
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

        Spacer(modifier = Modifier.height(16.dp))

        // Blocked Numbers List (Apple Inset Grouped)
        if (blockedList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Blocked Contacts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            AppleGroupedSection(
                title = "Blocked Numbers (${blockedList.size})"
            ) {
                blockedList.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.number,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = { itemToDelete = item },
                            colors = ButtonDefaults.textButtonColors(contentColor = AppleRed)
                        ) {
                            Text("Unblock")
                        }
                    }

                    if (index < blockedList.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }

    // Add Blocked Number Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newNumberToBlock = ""
            },
            title = { Text("Block Number") },
            text = {
                OutlinedTextField(
                    value = newNumberToBlock,
                    onValueChange = { newNumberToBlock = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("block_number_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val num = newNumberToBlock.trim()
                        if (num.isNotBlank()) {
                            scope.launch {
                                blockedRepo.blockNumber(num)
                                refresh()
                                showAddDialog = false
                                newNumberToBlock = ""
                            }
                        }
                    }
                ) {
                    Text("Block", color = AppleRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newNumberToBlock = ""
                    }
                ) {
                    Text("Cancel", color = AppleBlue)
                }
            }
        )
    }

    // Unblock confirmation
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Unblock Contact") },
            text = { Text("Unblock calls and messages from ${item.number}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            blockedRepo.unblockNumber(item.id)
                            refresh()
                            itemToDelete = null
                        }
                    }
                ) {
                    Text("Unblock", color = AppleBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = AppleBlue)
                }
            }
        )
    }
}
