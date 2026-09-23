package com.example.ui.dialpad

import android.app.Application
import android.telephony.PhoneNumberUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.model.ContactItem
import com.example.data.model.SimSubscriptionInfo
import com.example.data.model.SpeedDialEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class DialpadUiState(
    val enteredNumber: String = "",
    val formattedNumber: String = "",
    val matchedContacts: List<ContactItem> = emptyList(),
    val isDefaultDialer: Boolean = true,
    val activeSubscriptions: List<SimSubscriptionInfo> = emptyList(),
    val showSimPickerForNumber: String? = null,
    val speedDials: List<SpeedDialEntry> = emptyList()
)

class DialpadViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalimApplication
    private val telephonyRepo = app.telephonyRepository
    private val contactsRepo = app.contactsRepository
    private val db = app.database
    private val dtmfHaptic = app.dtmfAndHaptic

    private val _uiState = MutableStateFlow(DialpadUiState())
    val uiState: StateFlow<DialpadUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        refreshState()
        observeSpeedDials()
    }

    fun refreshState() {
        _uiState.value = _uiState.value.copy(
            isDefaultDialer = telephonyRepo.isDefaultDialer(),
            activeSubscriptions = telephonyRepo.getActiveSubscriptions()
        )
    }

    private fun observeSpeedDials() {
        viewModelScope.launch {
            db.speedDialDao().getAllSpeedDials().collect { list ->
                _uiState.value = _uiState.value.copy(speedDials = list)
            }
        }
    }

    fun onDigitPressed(char: Char) {
        dtmfHaptic.playTone(char)
        dtmfHaptic.triggerKeyHaptic()

        val newNumber = _uiState.value.enteredNumber + char
        updateNumber(newNumber)
    }

    fun onBackspace() {
        val current = _uiState.value.enteredNumber
        if (current.isNotEmpty()) {
            dtmfHaptic.triggerKeyHaptic()
            val newNumber = current.dropLast(1)
            updateNumber(newNumber)
        }
    }

    fun onBackspaceLong() {
        if (_uiState.value.enteredNumber.isNotEmpty()) {
            dtmfHaptic.triggerDestructiveHaptic()
            updateNumber("")
        }
    }

    fun onPasteNumber(pasted: String) {
        val clean = pasted.filter { it.isDigit() || it == '+' || it == ',' || it == ';' || it == '*' || it == '#' }
        if (clean.isNotEmpty()) {
            updateNumber(clean)
        }
    }

    private fun updateNumber(newNumber: String) {
        val formatted = try {
            PhoneNumberUtils.formatNumber(newNumber, Locale.getDefault().country) ?: newNumber
        } catch (e: Exception) {
            newNumber
        }

        _uiState.value = _uiState.value.copy(
            enteredNumber = newNumber,
            formattedNumber = formatted
        )

        searchContacts(newNumber)
    }

    private fun searchContacts(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(matchedContacts = emptyList())
            return
        }

        searchJob = viewModelScope.launch {
            delay(100) // Debounce query
            val matches = contactsRepo.getAllContacts(searchQuery = query)
            _uiState.value = _uiState.value.copy(matchedContacts = matches.take(5))
        }
    }

    fun onSpeedDialTriggered(key: Int) {
        viewModelScope.launch {
            val entry = db.speedDialDao().getByDigit(key)
            if (entry != null && entry.phoneNumber.isNotBlank()) {
                dtmfHaptic.triggerCommitHaptic()
                initiateCall(entry.phoneNumber)
            }
        }
    }

    fun onVoicemailTriggered() {
        val vmNumber = telephonyRepo.getVoicemailNumber() ?: "*86"
        dtmfHaptic.triggerCommitHaptic()
        initiateCall(vmNumber)
    }

    fun onCallButtonClicked() {
        val number = _uiState.value.enteredNumber
        if (number.isNotBlank()) {
            initiateCall(number)
        }
    }

    fun initiateCall(number: String) {
        val subs = _uiState.value.activeSubscriptions
        if (subs.size > 1) {
            // Check if contact has preferred SIM in DB
            viewModelScope.launch {
                val pref = db.contactSimPreferenceDao().getPreference(number)
                if (pref != null) {
                    telephonyRepo.placeCall(number, pref.subscriptionId)
                } else {
                    _uiState.value = _uiState.value.copy(showSimPickerForNumber = number)
                }
            }
        } else {
            telephonyRepo.placeCall(number, subs.firstOrNull()?.subscriptionId)
        }
    }

    fun onSimSelected(sub: SimSubscriptionInfo) {
        val number = _uiState.value.showSimPickerForNumber ?: return
        _uiState.value = _uiState.value.copy(showSimPickerForNumber = null)
        telephonyRepo.placeCall(number, sub.subscriptionId)
    }

    fun dismissSimPicker() {
        _uiState.value = _uiState.value.copy(showSimPickerForNumber = null)
    }
}
