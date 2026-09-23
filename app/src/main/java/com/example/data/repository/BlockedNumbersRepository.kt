package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.BlockedNumberContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BlockedNumberItem(
    val id: Long,
    val number: String,
    val e164Number: String?
)

class BlockedNumbersRepository(private val context: Context) {

    private val contentResolver: ContentResolver get() = context.contentResolver

    fun canBlock(): Boolean {
        return try {
            BlockedNumberContract.canCurrentUserBlockNumbers(context)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getBlockedNumbers(): List<BlockedNumberItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<BlockedNumberItem>()
        if (!canBlock()) return@withContext list

        val projection = arrayOf(
            BlockedNumberContract.BlockedNumbers.COLUMN_ID,
            BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
            BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER
        )

        try {
            val cursor: Cursor? = contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                projection,
                null,
                null,
                "${BlockedNumberContract.BlockedNumbers.COLUMN_ID} DESC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ID)
                val numCol = c.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
                val e164Col = c.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val number = c.getString(numCol) ?: ""
                    val e164 = c.getString(e164Col)
                    list.add(BlockedNumberItem(id, number, e164))
                }
            }
        } catch (e: SecurityException) {
            Log.w("BlockedNumbersRepo", "Not default dialer or missing permission to read blocked numbers", e)
        } catch (e: Exception) {
            Log.e("BlockedNumbersRepo", "Error querying blocked numbers", e)
        }

        list
    }

    suspend fun blockNumber(number: String): Boolean = withContext(Dispatchers.IO) {
        if (number.isBlank()) return@withContext false
        try {
            val values = ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            }
            val uri = contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
            uri != null
        } catch (e: Exception) {
            Log.e("BlockedNumbersRepo", "Error inserting blocked number", e)
            false
        }
    }

    suspend fun unblockNumber(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI.buildUpon().appendPath(id.toString()).build()
            contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            Log.e("BlockedNumbersRepo", "Error deleting blocked number", e)
            false
        }
    }
}
