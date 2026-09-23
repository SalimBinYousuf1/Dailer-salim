package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.util.Log
import com.example.data.model.CallLogEntry
import com.example.data.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallLogRepository(private val context: Context) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    suspend fun getCallLogs(searchQuery: String = "", filterType: CallType? = null): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CallLogEntry>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) CallLog.Calls.PHONE_ACCOUNT_ID else CallLog.Calls._ID
        )

        val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT 500"

        try {
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(CallLog.Calls._ID)
                val numCol = c.getColumnIndex(CallLog.Calls.NUMBER)
                val nameCol = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeCol = c.getColumnIndex(CallLog.Calls.TYPE)
                val dateCol = c.getColumnIndex(CallLog.Calls.DATE)
                val durCol = c.getColumnIndex(CallLog.Calls.DURATION)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val number = c.getString(numCol) ?: ""
                    val name = c.getString(nameCol)
                    val rawType = c.getInt(typeCol)
                    val date = c.getLong(dateCol)
                    val duration = c.getLong(durCol)

                    val type = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
                        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                        else -> CallType.INCOMING
                    }

                    list.add(
                        CallLogEntry(
                            id = id,
                            number = number,
                            name = name,
                            type = type,
                            timestamp = date,
                            durationSeconds = duration,
                            simSlot = null,
                            count = 1,
                            rawIds = listOf(id)
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w("CallLogRepo", "Permission not granted for CallLog", e)
        } catch (e: Exception) {
            Log.e("CallLogRepo", "Error reading CallLog", e)
        }

        // Group consecutive entries with same number and type
        val grouped = mutableListOf<CallLogEntry>()
        for (entry in list) {
            val last = grouped.lastOrNull()
            if (last != null && last.number == entry.number && last.type == entry.type) {
                grouped[grouped.lastIndex] = last.copy(
                    count = last.count + 1,
                    rawIds = last.rawIds + entry.id
                )
            } else {
                grouped.add(entry)
            }
        }

        var filtered: List<CallLogEntry> = grouped
        if (filterType != null) {
            filtered = filtered.filter { it.type == filterType }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            filtered = filtered.filter {
                (it.name?.lowercase()?.contains(q) == true) || it.number.contains(q)
            }
        }

        filtered
    }

    suspend fun getMissedCallCount(): Int = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(CallLog.Calls._ID)
            val selection = "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.IS_READ} = 0"
            val selectionArgs = arrayOf(CallLog.Calls.MISSED_TYPE.toString())
            val cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            val count = cursor?.count ?: 0
            cursor?.close()
            count
        } catch (e: Exception) {
            0
        }
    }

    suspend fun deleteEntries(ids: List<Long>): Int = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext 0
        try {
            val selection = "${CallLog.Calls._ID} IN (${ids.joinToString(",")})"
            contentResolver.delete(CallLog.Calls.CONTENT_URI, selection, null)
        } catch (e: Exception) {
            Log.e("CallLogRepo", "Error deleting call log entries", e)
            0
        }
    }

    suspend fun exportCallLogsToCsv(): File? = withContext(Dispatchers.IO) {
        try {
            val entries = getCallLogs()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "salim_call_history_${System.currentTimeMillis()}.csv")

            file.bufferedWriter().use { writer ->
                writer.write("ID,Name,Number,Type,Date,Duration(Seconds)\n")
                for (e in entries) {
                    val formattedDate = dateFormat.format(Date(e.timestamp))
                    val safeName = e.name?.replace("\"", "\"\"") ?: "Unknown"
                    writer.write("${e.id},\"$safeName\",${e.number},${e.type.name},$formattedDate,${e.durationSeconds}\n")
                }
            }
            file
        } catch (e: Exception) {
            Log.e("CallLogRepo", "Error exporting call log to CSV", e)
            null
        }
    }
}
