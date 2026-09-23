package com.example.telephony

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveCallUiState(
    val hasCall: Boolean = false,
    val callState: Int = Call.STATE_DISCONNECTED,
    val number: String = "",
    val displayName: String? = null,
    val isIncoming: Boolean = false,
    val isActive: Boolean = false,
    val isOnHold: Boolean = false,
    val isMuted: Boolean = false,
    val audioRoute: Int = CallAudioState.ROUTE_EARPIECE,
    val supportedAudioRoutes: Int = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER,
    val connectTimeMillis: Long = 0L,
    val canMerge: Boolean = false,
    val canSwap: Boolean = false,
    val secondaryCallNumber: String? = null
)

object CallManager {
    private val _callState = MutableStateFlow(ActiveCallUiState())
    val callState: StateFlow<ActiveCallUiState> = _callState.asStateFlow()

    private var currentInCallService: SalimInCallService? = null
    var primaryCall: Call? = null
        private set
    var secondaryCall: Call? = null
        private set

    fun registerService(service: SalimInCallService) {
        currentInCallService = service
    }

    fun unregisterService(service: SalimInCallService) {
        if (currentInCallService == service) {
            currentInCallService = null
        }
    }

    fun updateFromCall(primary: Call?, secondary: Call? = null, audioState: CallAudioState? = null) {
        primaryCall = primary
        secondaryCall = secondary

        if (primary == null) {
            _callState.value = ActiveCallUiState()
            return
        }

        val details = primary.details
        val rawHandle = details?.handle?.schemeSpecificPart ?: ""
        val callerName = details?.callerDisplayName

        val state = primary.state
        val isIncoming = state == Call.STATE_RINGING
        val isActive = state == Call.STATE_ACTIVE
        val isOnHold = state == Call.STATE_HOLDING

        val route = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
        val supportedRoutes = audioState?.supportedRouteMask ?: (CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
        val muted = audioState?.isMuted ?: false

        val canMerge = primary.conferenceableCalls.isNotEmpty() || secondary != null
        val canSwap = secondary != null

        _callState.value = ActiveCallUiState(
            hasCall = true,
            callState = state,
            number = rawHandle,
            displayName = callerName,
            isIncoming = isIncoming,
            isActive = isActive,
            isOnHold = isOnHold,
            isMuted = muted,
            audioRoute = route,
            supportedAudioRoutes = supportedRoutes,
            connectTimeMillis = details?.connectTimeMillis ?: 0L,
            canMerge = canMerge,
            canSwap = canSwap,
            secondaryCallNumber = secondary?.details?.handle?.schemeSpecificPart
        )
    }

    fun answer() {
        primaryCall?.answer(0)
    }

    fun answerCall() = answer()

    fun reject(rejectWithMessage: Boolean = false, message: String? = null) {
        if (rejectWithMessage && message != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            primaryCall?.reject(Call.REJECT_REASON_DECLINED)
        } else {
            @Suppress("DEPRECATION")
            primaryCall?.reject(false, null)
        }
    }

    fun disconnect() {
        primaryCall?.disconnect()
    }

    fun disconnectCall() = disconnect()

    fun setMuted(muted: Boolean) {
        currentInCallService?.setMuted(muted)
    }

    fun setAudioRoute(route: Int) {
        currentInCallService?.setAudioRoute(route)
    }

    fun setSpeakerphone(speaker: Boolean) {
        if (speaker) {
            setAudioRoute(CallAudioState.ROUTE_SPEAKER)
        } else {
            setAudioRoute(CallAudioState.ROUTE_EARPIECE)
        }
    }

    fun toggleSpeaker() {
        val currentRoute = _callState.value.audioRoute
        if (currentRoute == CallAudioState.ROUTE_SPEAKER) {
            setAudioRoute(CallAudioState.ROUTE_EARPIECE)
        } else {
            setAudioRoute(CallAudioState.ROUTE_SPEAKER)
        }
    }

    fun hold(hold: Boolean) {
        if (hold) {
            primaryCall?.hold()
        } else {
            primaryCall?.unhold()
        }
    }

    fun setHold(hold: Boolean) = hold(hold)

    fun playDtmf(digit: Char) {
        primaryCall?.playDtmfTone(digit)
    }

    fun stopDtmf() {
        primaryCall?.stopDtmfTone()
    }

    fun merge() {
        val confCalls = primaryCall?.conferenceableCalls
        if (!confCalls.isNullOrEmpty()) {
            primaryCall?.conference(confCalls.first())
        } else if (secondaryCall != null) {
            primaryCall?.conference(secondaryCall)
        }
    }

    fun swap() {
        val currentPrimary = primaryCall
        val currentSecondary = secondaryCall
        if (currentPrimary != null && currentSecondary != null) {
            currentPrimary.hold()
            currentSecondary.unhold()
            primaryCall = currentSecondary
            secondaryCall = currentPrimary
            updateFromCall(primaryCall, secondaryCall, currentInCallService?.callAudioState)
        }
    }
}

class SalimInCallService : InCallService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val activeCalls = mutableListOf<Call>()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            if (state == Call.STATE_DISCONNECTED) {
                activeCalls.remove(call)
            }
            refreshCalls()
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            super.onDetailsChanged(call, details)
            refreshCalls()
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        activeCalls.add(call)
        call.registerCallback(callCallback)
        refreshCalls()

        // Launch in-call UI activity
        val intent = Intent(this, Class.forName("com.example.ui.call.CallActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        activeCalls.remove(call)
        refreshCalls()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        refreshCalls(audioState)
    }

    override fun onCreate() {
        super.onCreate()
        CallManager.registerService(this)
    }

    override fun onDestroy() {
        CallManager.unregisterService(this)
        super.onDestroy()
    }

    private fun refreshCalls(audioState: CallAudioState? = callAudioState) {
        val primary = activeCalls.firstOrNull { it.state == Call.STATE_RINGING }
            ?: activeCalls.firstOrNull { it.state == Call.STATE_ACTIVE || it.state == Call.STATE_DIALING }
            ?: activeCalls.firstOrNull()

        val secondary = activeCalls.firstOrNull { it != primary }

        CallManager.updateFromCall(primary, secondary, audioState)
    }
}
