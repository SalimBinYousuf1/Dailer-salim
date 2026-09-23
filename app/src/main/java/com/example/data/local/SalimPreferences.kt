package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "salim_settings")

class SalimPreferences(private val context: Context) {

    private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    private val KEY_BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown_numbers")
    private val KEY_DEFAULT_SIM_ID = intPreferencesKey("default_sim_subscription_id")
    private val KEY_CALL_RECORDING_ENABLED = booleanPreferencesKey("call_recording_enabled")
    private val KEY_RINGTONE_URI = stringPreferencesKey("custom_ringtone_uri")

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VIBRATION_ENABLED] ?: true
    }

    val blockUnknownNumbers: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BLOCK_UNKNOWN] ?: false
    }

    val defaultSimId: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_SIM_ID]
    }

    val callRecordingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CALL_RECORDING_ENABLED] ?: false
    }

    val ringtoneUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_RINGTONE_URI]
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setBlockUnknownNumbers(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_BLOCK_UNKNOWN] = enabled }
    }

    suspend fun setDefaultSimId(subId: Int?) {
        context.dataStore.edit { prefs ->
            if (subId == null) {
                prefs.remove(KEY_DEFAULT_SIM_ID)
            } else {
                prefs[KEY_DEFAULT_SIM_ID] = subId
            }
        }
    }

    suspend fun setCallRecordingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_CALL_RECORDING_ENABLED] = enabled }
    }

    suspend fun setRingtoneUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(KEY_RINGTONE_URI)
            } else {
                prefs[KEY_RINGTONE_URI] = uri
            }
        }
    }
}
