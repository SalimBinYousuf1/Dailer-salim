package com.example.data.repository

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.example.data.model.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ContactsRepository(private val context: Context) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    suspend fun getAllContacts(searchQuery: String = "", favoritesOnly: Boolean = false): List<ContactItem> = withContext(Dispatchers.IO) {
        val contactsMap = linkedMapOf<Long, ContactItem>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.STARRED,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        var selection: String? = null
        var selectionArgs: Array<String>? = null

        if (favoritesOnly) {
            selection = "${ContactsContract.CommonDataKinds.Phone.STARRED} = 1"
        }

        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"

        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val keyCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                val nameCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val starCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                val photoCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val key = c.getString(keyCol) ?: id.toString()
                    val name = c.getString(nameCol) ?: "Unnamed"
                    val number = c.getString(numCol) ?: ""
                    val isStarred = c.getInt(starCol) == 1
                    val photo = c.getString(photoCol)

                    val existing = contactsMap[id]
                    if (existing != null) {
                        if (number.isNotBlank() && !existing.phoneNumbers.contains(number)) {
                            contactsMap[id] = existing.copy(phoneNumbers = existing.phoneNumbers + number)
                        }
                    } else {
                        val header = name.firstOrNull()?.uppercaseChar()?.let {
                            if (it in 'A'..'Z') it else '#'
                        } ?: '#'

                        contactsMap[id] = ContactItem(
                            id = id,
                            lookupKey = key,
                            displayName = name,
                            phoneNumbers = if (number.isNotBlank()) listOf(number) else emptyList(),
                            isStarred = isStarred,
                            photoUri = photo,
                            letterHeader = header
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w("ContactsRepo", "READ_CONTACTS permission not granted", e)
        } catch (e: Exception) {
            Log.e("ContactsRepo", "Error reading contacts", e)
        }

        var result = contactsMap.values.toList()

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            result = result.filter { contact ->
                contact.displayName.lowercase().contains(q) ||
                        contact.phoneNumbers.any { it.replace("[^0-9+]".toRegex(), "").contains(q.replace("[^0-9+]".toRegex(), "")) }
            }
        }

        result
    }

    suspend fun lookupContactByNumber(number: String): ContactItem? = withContext(Dispatchers.IO) {
        if (number.isBlank()) return@withContext null
        val clean = number.filter { it.isDigit() || it == '+' }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(clean))
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.LOOKUP_KEY,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.NUMBER,
            ContactsContract.PhoneLookup.STARRED,
            ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
        )

        try {
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                    val key = c.getString(c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.LOOKUP_KEY)) ?: id.toString()
                    val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)) ?: number
                    val num = c.getString(c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER)) ?: number
                    val starred = c.getInt(c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.STARRED)) == 1
                    val photo = c.getString(c.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI))
                    return@withContext ContactItem(
                        id = id,
                        lookupKey = key,
                        displayName = name,
                        phoneNumbers = listOf(num),
                        isStarred = starred,
                        photoUri = photo
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ContactsRepo", "Error looking up contact by number", e)
        }
        null
    }

    suspend fun setStarred(contactId: Long, starred: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
            }
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            contentResolver.update(uri, values, null, null) > 0
        } catch (e: Exception) {
            Log.e("ContactsRepo", "Error updating starred status", e)
            false
        }
    }

    suspend fun createContact(displayName: String, phoneNumber: String): Long? = withContext(Dispatchers.IO) {
        try {
            val ops = ArrayList<ContentProviderOperation>()

            val rawContactInsertIndex = ops.size
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build()
            )

            // Phone
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )

            val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val rawContactUri = results[0].uri
            rawContactUri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            Log.e("ContactsRepo", "Error creating contact", e)
            null
        }
    }

    suspend fun deleteContact(contactId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            Log.e("ContactsRepo", "Error deleting contact", e)
            false
        }
    }

    suspend fun exportContactsToVcf(): File? = withContext(Dispatchers.IO) {
        try {
            val contacts = getAllContacts()
            val exportDir = File(context.cacheDir, "vcf_exports").apply { mkdirs() }
            val file = File(exportDir, "salim_contacts_${System.currentTimeMillis()}.vcf")

            file.bufferedWriter().use { writer ->
                for (contact in contacts) {
                    writer.write("BEGIN:VCARD\r\n")
                    writer.write("VERSION:3.0\r\n")
                    writer.write("FN:${contact.displayName}\r\n")
                    for (phone in contact.phoneNumbers) {
                        writer.write("TEL;TYPE=CELL:$phone\r\n")
                    }
                    writer.write("END:VCARD\r\n")
                }
            }
            file
        } catch (e: Exception) {
            Log.e("ContactsRepo", "Error exporting VCF", e)
            null
        }
    }
}
