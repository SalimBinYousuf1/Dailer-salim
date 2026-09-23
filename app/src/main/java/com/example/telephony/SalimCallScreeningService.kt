package com.example.telephony

import android.os.Build
import android.provider.BlockedNumberContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.data.local.SalimPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SalimCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences by lazy { SalimPreferences(applicationContext) }

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: ""

        serviceScope.launch {
            var shouldBlock = false

            // Check if user enabled blocking unknown/private numbers
            val blockUnknown = preferences.blockUnknownNumbers.first()
            if (blockUnknown && number.isBlank()) {
                shouldBlock = true
            } else if (number.isNotBlank()) {
                // Check against BlockedNumberContract
                try {
                    if (BlockedNumberContract.canCurrentUserBlockNumbers(applicationContext)) {
                        shouldBlock = BlockedNumberContract.isBlocked(applicationContext, number)
                    }
                } catch (e: Exception) {
                    Log.e("CallScreening", "Error checking blocked number provider", e)
                }
            }

            val response = CallResponse.Builder()
                .setDisallowCall(shouldBlock)
                .setRejectCall(shouldBlock)
                .setSkipCallLog(false)
                .setSkipNotification(shouldBlock)
                .build()

            respondToCall(callDetails, response)
        }
    }
}
