package com.example.data.repository

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.model.SimSubscriptionInfo

class TelephonyRepository(private val context: Context) {

    private val telecomManager: TelecomManager? by lazy {
        context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    }

    private val telephonyManager: TelephonyManager? by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    }

    private val roleManager: RoleManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
        } else null
    }

    fun isDefaultDialer(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
        } else {
            telecomManager?.defaultDialerPackage == context.packageName
        }
    }

    fun createRequestDefaultDialerIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleManager?.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            @Suppress("DEPRECATION")
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        }
    }

    fun getActiveSubscriptions(): List<SimSubscriptionInfo> {
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            @Suppress("MissingPermission")
            val list = subscriptionManager?.activeSubscriptionInfoList ?: emptyList()
            list.map { info: SubscriptionInfo ->
                SimSubscriptionInfo(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                    carrierName = info.carrierName?.toString() ?: "Carrier",
                    iccId = info.iccId
                )
            }
        } catch (e: Exception) {
            Log.e("TelephonyRepo", "Error getting subscriptions", e)
            emptyList()
        }
    }

    fun getVoicemailNumber(): String? {
        return try {
            @Suppress("MissingPermission")
            telephonyManager?.voiceMailNumber
        } catch (e: Exception) {
            null
        }
    }

    fun placeCall(number: String, preferredSubId: Int? = null): Boolean {
        val cleanNumber = number.filter { it.isDigit() || it == '+' || it == ',' || it == ';' || it == '*' || it == '#' }
        if (cleanNumber.isEmpty()) return false

        val uri = Uri.fromParts("tel", cleanNumber, null)

        if (isDefaultDialer() && telecomManager != null) {
            try {
                val extras = Bundle()
                if (preferredSubId != null) {
                    // Try to find matching phone account handle
                    @Suppress("MissingPermission")
                    val accounts = telecomManager?.callCapablePhoneAccounts ?: emptyList()
                    val targetAccount = accounts.firstOrNull { it.id.contains(preferredSubId.toString()) }
                    if (targetAccount != null) {
                        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, targetAccount)
                    }
                }
                @Suppress("MissingPermission")
                telecomManager?.placeCall(uri, extras)
                return true
            } catch (e: Exception) {
                Log.e("TelephonyRepo", "TelecomManager placeCall failed, using intent fallback", e)
            }
        }

        // Action call / dial intent fallback
        return try {
            val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
            true
        } catch (e: Exception) {
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                true
            } catch (err: Exception) {
                Log.e("TelephonyRepo", "Could not start call intent", err)
                false
            }
        }
    }

    fun sendCannedSms(recipientNumber: String, messageText: String): Boolean {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(recipientNumber, null, messageText, null, null)
            true
        } catch (e: Exception) {
            Log.e("TelephonyRepo", "Direct SMS send failed, attempting intent fallback", e)
            try {
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$recipientNumber")).apply {
                    putExtra("sms_body", messageText)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(smsIntent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }
}
