package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.CallType

@Composable
fun CallTypeIcon(
    type: CallType,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    val description = when (type) {
        CallType.INCOMING -> "Incoming call"
        CallType.OUTGOING -> "Outgoing call"
        CallType.MISSED -> "Missed call"
        CallType.BLOCKED -> "Blocked call"
        CallType.REJECTED -> "Rejected call"
    }

    val icon = when (type) {
        CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
        CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
        CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed
        CallType.BLOCKED, CallType.REJECTED -> Icons.Default.Block
    }

    // Strictly monochrome: hierarchy comes from the icon shape and TalkBack accessibility
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description }
    )
}
