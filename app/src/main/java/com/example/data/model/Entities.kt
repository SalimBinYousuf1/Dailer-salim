package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_dial")
data class SpeedDialEntry(
    @PrimaryKey val digitKey: Int, // 2..9
    val contactName: String,
    val phoneNumber: String,
    val avatarUri: String? = null
)

@Entity(tableName = "canned_messages")
data class CannedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "contact_sim_preference")
data class ContactSimPreference(
    @PrimaryKey val phoneNumber: String,
    val subscriptionId: Int
)

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    BLOCKED,
    REJECTED
}

data class CallLogEntry(
    val id: Long,
    val number: String,
    val name: String?,
    val type: CallType,
    val timestamp: Long,
    val durationSeconds: Long,
    val simSlot: Int?,
    val count: Int = 1,
    val rawIds: List<Long> = listOf(id)
)

data class ContactItem(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val phoneNumbers: List<String>,
    val isStarred: Boolean,
    val photoUri: String? = null,
    val letterHeader: Char = displayName.firstOrNull()?.uppercaseChar() ?: '#'
)

data class SimSubscriptionInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val iccId: String? = null
)
