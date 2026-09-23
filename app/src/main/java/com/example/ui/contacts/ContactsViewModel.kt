package com.example.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.model.ContactItem
import com.example.data.model.SpeedDialEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ContactsUiState(
    val contacts: List<ContactItem> = emptyList(),
    val favorites: List<ContactItem> = emptyList(),
    val speedDials: List<SpeedDialEntry> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedContactForDetail: ContactItem? = null,
    val showAddContactSheet: Boolean = false,
    val showSpeedDialAssignSheetForDigit: Int? = null,
    val exportedVcfFile: File? = null
)

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalimApplication
    private val contactsRepo = app.contactsRepository
    private val telephonyRepo = app.telephonyRepository
    private val db = app.database
    private val dtmfHaptic = app.dtmfAndHaptic

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
        observeSpeedDials()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val all = contactsRepo.getAllContacts(searchQuery = _uiState.value.searchQuery)
            val favs = contactsRepo.getAllContacts(favoritesOnly = true)
            _uiState.value = _uiState.value.copy(
                contacts = all,
                favorites = favs,
                isLoading = false
            )
        }
    }

    private fun observeSpeedDials() {
        viewModelScope.launch {
            db.speedDialDao().getAllSpeedDials().collect { list ->
                _uiState.value = _uiState.value.copy(speedDials = list)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadContacts()
    }

    fun toggleStarred(contact: ContactItem) {
        viewModelScope.launch {
            val newStatus = !contact.isStarred
            contactsRepo.setStarred(contact.id, newStatus)
            dtmfHaptic.triggerCommitHaptic()
            loadContacts()
            if (_uiState.value.selectedContactForDetail?.id == contact.id) {
                _uiState.value = _uiState.value.copy(
                    selectedContactForDetail = contact.copy(isStarred = newStatus)
                )
            }
        }
    }

    fun selectContact(contact: ContactItem) {
        _uiState.value = _uiState.value.copy(selectedContactForDetail = contact)
    }

    fun dismissDetail() {
        _uiState.value = _uiState.value.copy(selectedContactForDetail = null)
    }

    fun openAddContact(prefillNumber: String = "") {
        _uiState.value = _uiState.value.copy(showAddContactSheet = true)
    }

    fun closeAddContact() {
        _uiState.value = _uiState.value.copy(showAddContactSheet = false)
    }

    fun saveNewContact(name: String, number: String) {
        viewModelScope.launch {
            if (name.isNotBlank() && number.isNotBlank()) {
                contactsRepo.createContact(name.trim(), number.trim())
                dtmfHaptic.triggerCommitHaptic()
                closeAddContact()
                loadContacts()
            }
        }
    }

    fun deleteContact(contact: ContactItem) {
        viewModelScope.launch {
            dtmfHaptic.triggerDestructiveHaptic()
            contactsRepo.deleteContact(contact.id)
            dismissDetail()
            loadContacts()
        }
    }

    fun assignSpeedDial(digit: Int, contact: ContactItem, number: String) {
        viewModelScope.launch {
            db.speedDialDao().insert(
                SpeedDialEntry(
                    digitKey = digit,
                    contactName = contact.displayName,
                    phoneNumber = number
                )
            )
            _uiState.value = _uiState.value.copy(showSpeedDialAssignSheetForDigit = null)
        }
    }

    fun removeSpeedDial(digit: Int) {
        viewModelScope.launch {
            db.speedDialDao().deleteByDigit(digit)
        }
    }

    fun openSpeedDialAssign(digit: Int) {
        _uiState.value = _uiState.value.copy(showSpeedDialAssignSheetForDigit = digit)
    }

    fun closeSpeedDialAssign() {
        _uiState.value = _uiState.value.copy(showSpeedDialAssignSheetForDigit = null)
    }

    fun exportVcf() {
        viewModelScope.launch {
            val file = contactsRepo.exportContactsToVcf()
            _uiState.value = _uiState.value.copy(exportedVcfFile = file)
        }
    }

    fun clearVcfNotice() {
        _uiState.value = _uiState.value.copy(exportedVcfFile = null)
    }

    fun placeCall(number: String) {
        dtmfHaptic.triggerCommitHaptic()
        telephonyRepo.placeCall(number)
    }
}
