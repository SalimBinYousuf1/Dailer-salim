package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SalimPreferences
import com.example.data.repository.BlockedNumbersRepository
import com.example.data.repository.CallLogRepository
import com.example.data.repository.ContactsRepository
import com.example.data.repository.TelephonyRepository
import com.example.telephony.DtmfAndHapticFeedback

class SalimApplication : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val preferences by lazy { SalimPreferences(this) }
    val telephonyRepository by lazy { TelephonyRepository(this) }
    val callLogRepository by lazy { CallLogRepository(this) }
    val contactsRepository by lazy { ContactsRepository(this) }
    val blockedNumbersRepository by lazy { BlockedNumbersRepository(this) }
    val dtmfAndHaptic by lazy { DtmfAndHapticFeedback(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SalimApplication
            private set
    }
}
