package com.example.ui.contacts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.SalimApplication
import com.example.data.model.ContactItem
import com.example.ui.components.SquircleSheetShape
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as SalimApplication
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var newContactName by remember { mutableStateOf("") }
    var newContactNumber by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // iOS Navigation Bar: [ Export (left) | + Add (right) ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Export",
                style = MaterialTheme.typography.bodyLarge,
                color = AppleBlue,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { viewModel.exportVcf() }
                    .padding(4.dp)
            )

            IconButton(
                onClick = {
                    newContactName = ""
                    newContactNumber = ""
                    viewModel.openAddContact()
                },
                modifier = Modifier.testTag("add_contact_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Contact",
                    tint = AppleBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Apple Large Title: "Contacts"
        Text(
            text = "Contacts",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 6.dp)
        )

        // iOS Search Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .height(40.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.searchQuery.isEmpty()) {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_search_field")
                    )
                }

                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.setSearchQuery("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Contacts list grouped by letter
        val groupedContacts = uiState.contacts.groupBy {
            val first = it.displayName.firstOrNull()?.uppercaseChar() ?: '#'
            if (first in 'A'..'Z') first else '#'
        }.toSortedMap()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            groupedContacts.forEach { (char, contactsForChar) ->
                item(key = "header_$char") {
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                items(contactsForChar, key = { it.id }) { contact ->
                    AppleContactRow(
                        contact = contact,
                        onClick = { viewModel.selectContact(contact) }
                    )
                }
            }
        }
    }

    // iOS Contact Detail Bottom Sheet
    uiState.selectedContactForDetail?.let { contact ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissDetail() },
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
                // Large circular avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E5EA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.displayName.firstOrNull()?.uppercase() ?: "#",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Apple 4 Action Pills Row: message, call, favorite, copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AppleContactActionPill(
                        icon = Icons.Default.Message,
                        label = "message",
                        onClick = {
                            viewModel.dismissDetail()
                            contact.phoneNumbers.firstOrNull()?.let { num ->
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$num"))
                                context.startActivity(intent)
                            }
                        }
                    )
                    AppleContactActionPill(
                        icon = Icons.Default.Call,
                        label = "call",
                        onClick = {
                            viewModel.dismissDetail()
                            contact.phoneNumbers.firstOrNull()?.let { viewModel.placeCall(it) }
                        }
                    )
                    AppleContactActionPill(
                        icon = if (contact.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        label = if (contact.isStarred) "unfavorite" else "favorite",
                        tint = if (contact.isStarred) Color(0xFFFF9500) else AppleBlue,
                        onClick = {
                            viewModel.toggleStarred(contact)
                        }
                    )
                    AppleContactActionPill(
                        icon = Icons.Default.ContentCopy,
                        label = "copy",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            contact.phoneNumbers.firstOrNull()?.let { num ->
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Contact", num))
                            }
                            viewModel.dismissDetail()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Inset Group: Phone Numbers
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "phone",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        contact.phoneNumbers.forEach { num ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.dismissDetail()
                                        viewModel.placeCall(num)
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = num,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AppleBlue,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = AppleGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Inset Group: Block / Actions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                contact.phoneNumbers.firstOrNull()?.let { num ->
                                    scope.launch {
                                        app.blockedNumbersRepository.blockNumber(num)
                                    }
                                }
                                viewModel.dismissDetail()
                            }
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
                }
            }
        }
    }

    // Add Contact Dialog
    if (uiState.showAddContactSheet) {
        AlertDialog(
            onDismissRequest = { viewModel.closeAddContact() },
            title = { Text("New Contact") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_contact_name_field")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newContactNumber,
                        onValueChange = { newContactNumber = it },
                        label = { Text("Phone") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_contact_number_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveNewContact(newContactName, newContactNumber)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                    modifier = Modifier.testTag("save_contact_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddContact() }) {
                    Text("Cancel", color = AppleBlue)
                }
            }
        )
    }

    // Exported VCF confirmation
    uiState.exportedVcfFile?.let { file ->
        AlertDialog(
            onDismissRequest = { viewModel.clearVcfNotice() },
            title = { Text("Contacts Exported") },
            text = { Text("Saved ${file.name} to local device storage.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearVcfNotice() }) {
                    Text("Done", color = AppleBlue)
                }
            }
        )
    }
}

@Composable
private fun AppleContactRow(
    contact: ContactItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("contact_item_${contact.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (contact.isStarred) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Starred",
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

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
private fun AppleContactActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = AppleBlue,
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
                tint = tint,
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
