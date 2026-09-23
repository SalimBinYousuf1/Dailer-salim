package com.example.ui.contacts

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ContactItem
import com.example.ui.components.SquircleAvatarShape
import com.example.ui.components.SquircleCardShape
import com.example.ui.components.SquirclePillShape
import com.example.ui.components.SquircleSheetShape
import com.example.ui.components.SquircleSmallCardShape
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.DestructiveRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Contacts",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Export VCF
                IconButton(
                    onClick = { viewModel.exportVcf() },
                    modifier = Modifier.testTag("export_vcf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export VCF",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Add Contact Button
                IconButton(
                    onClick = { viewModel.openAddContact() },
                    modifier = Modifier.testTag("add_contact_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Contact",
                        tint = AccentGreen
                    )
                }
            }
        }

        // Search Bar (Apple style minimal rounded pill)
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search contacts") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            shape = SquirclePillShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .testTag("contact_search_field")
        )

        // Contact List grouped by letter headers
        if (uiState.contacts.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Contacts Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap + to create a contact or grant contacts permission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val grouped = uiState.contacts.groupBy { it.letterHeader }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                grouped.forEach { (header, contactsInGroup) ->
                    item(key = "header_$header") {
                        Text(
                            text = header.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                        )
                    }

                    items(contactsInGroup, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            onClick = { viewModel.selectContact(contact) }
                        )
                    }
                }
            }
        }
    }

    // Contact Detail Bottom Sheet
    uiState.selectedContactForDetail?.let { contact ->
        ContactDetailSheet(
            contact = contact,
            onDismiss = { viewModel.dismissDetail() },
            onCall = { number -> viewModel.placeCall(number) },
            onMessage = { number ->
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                context.startActivity(intent)
            },
            onVideoCall = { number ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tel:$number"))
                context.startActivity(intent)
            },
            onToggleStar = { viewModel.toggleStarred(contact) },
            onDelete = { viewModel.deleteContact(contact) }
        )
    }

    // Add Contact Sheet
    if (uiState.showAddContactSheet) {
        AddContactSheet(
            onDismiss = { viewModel.closeAddContact() },
            onSave = { name, number -> viewModel.saveNewContact(name, number) }
        )
    }

    // VCF Export Notice
    uiState.exportedVcfFile?.let { file ->
        AlertDialog(
            onDismissRequest = { viewModel.clearVcfNotice() },
            title = { Text("Contacts Exported") },
            text = { Text("Exported ${file.name} to local storage cache successfully.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearVcfNotice() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ContactRow(
    contact: ContactItem,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("contact_item_${contact.id}")
            .semantics {
                contentDescription = "${contact.displayName}, ${contact.phoneNumbers.firstOrNull() ?: "No number"}"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(SquircleAvatarShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.displayName.firstOrNull()?.uppercase() ?: "#",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.phoneNumbers.isNotEmpty()) {
                    Text(
                        text = contact.phoneNumbers.first(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (contact.isStarred) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailSheet(
    contact: ContactItem,
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
    onMessage: (String) -> Unit,
    onVideoCall: (String) -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            // Big avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(SquircleAvatarShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.displayName.firstOrNull()?.uppercase() ?: "#",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val primaryNumber = contact.phoneNumbers.firstOrNull() ?: ""

            Spacer(modifier = Modifier.height(18.dp))

            // Action Pills Row: Call, Message, Video, Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionCircleButton(
                    icon = Icons.Default.Call,
                    label = "Call",
                    onClick = {
                        if (primaryNumber.isNotBlank()) {
                            onDismiss()
                            onCall(primaryNumber)
                        }
                    }
                )

                ActionCircleButton(
                    icon = Icons.Default.Message,
                    label = "Message",
                    onClick = {
                        if (primaryNumber.isNotBlank()) {
                            onDismiss()
                            onMessage(primaryNumber)
                        }
                    }
                )

                ActionCircleButton(
                    icon = Icons.Default.Videocam,
                    label = "Video",
                    onClick = {
                        if (primaryNumber.isNotBlank()) {
                            onDismiss()
                            onVideoCall(primaryNumber)
                        }
                    }
                )

                ActionCircleButton(
                    icon = if (contact.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                    label = if (contact.isStarred) "Starred" else "Favorite",
                    tint = if (contact.isStarred) AccentGreen else MaterialTheme.colorScheme.onSurface,
                    onClick = onToggleStar
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Phone numbers list
            contact.phoneNumbers.forEach { number ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = SquircleSmallCardShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onDismiss()
                            onCall(number)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "mobile",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = number,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delete contact action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SquircleSmallCardShape)
                    .clickable { onDelete() }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = DestructiveRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Delete Contact",
                    style = MaterialTheme.typography.bodyLarge,
                    color = DestructiveRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ActionCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(SquircleSmallCardShape)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
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
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, number: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "New Contact",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = { onSave(name, number) },
                    enabled = name.isNotBlank() && number.isNotBlank()
                ) {
                    Text("Done", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                shape = SquircleSmallCardShape,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_contact_name_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = SquircleSmallCardShape,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_contact_phone_field")
            )
        }
    }
}
