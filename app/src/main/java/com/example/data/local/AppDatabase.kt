package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CannedMessage
import com.example.data.model.ContactSimPreference
import com.example.data.model.SpeedDialEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Dao
interface SpeedDialDao {
    @Query("SELECT * FROM speed_dial ORDER BY digitKey ASC")
    fun getAllSpeedDials(): Flow<List<SpeedDialEntry>>

    @Query("SELECT * FROM speed_dial WHERE digitKey = :digit LIMIT 1")
    suspend fun getByDigit(digit: Int): SpeedDialEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SpeedDialEntry)

    @Query("DELETE FROM speed_dial WHERE digitKey = :digit")
    suspend fun deleteByDigit(digit: Int)
}

@Dao
interface CannedMessageDao {
    @Query("SELECT * FROM canned_messages ORDER BY id ASC")
    fun getAll(): Flow<List<CannedMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: CannedMessage): Long

    @Update
    suspend fun update(message: CannedMessage)

    @Query("DELETE FROM canned_messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ContactSimPreferenceDao {
    @Query("SELECT * FROM contact_sim_preference WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getPreference(phoneNumber: String): ContactSimPreference?

    @Query("SELECT * FROM contact_sim_preference")
    fun getAllPreferences(): Flow<List<ContactSimPreference>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(pref: ContactSimPreference)

    @Query("DELETE FROM contact_sim_preference WHERE phoneNumber = :phoneNumber")
    suspend fun deletePreference(phoneNumber: String)
}

@Database(
    entities = [SpeedDialEntry::class, CannedMessage::class, ContactSimPreference::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun speedDialDao(): SpeedDialDao
    abstract fun cannedMessageDao(): CannedMessageDao
    abstract fun contactSimPreferenceDao(): ContactSimPreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salim_phone.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default canned quick-decline messages
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).cannedMessageDao()
                            dao.insert(CannedMessage(text = "Can't talk right now. What's up?", isDefault = true))
                            dao.insert(CannedMessage(text = "I'll call you right back.", isDefault = true))
                            dao.insert(CannedMessage(text = "On my way.", isDefault = true))
                            dao.insert(CannedMessage(text = "In a meeting, will text later.", isDefault = true))
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
