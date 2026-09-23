package com.example.ui.recents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.model.CallLogEntry
import com.example.data.model.CallType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class RecentsUiState(
    val callLogs: List<CallLogEntry> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: CallType? = null,
    val isMultiSelectMode: Boolean = false,
    val selectedEntryIds: Set<Long> = emptySet(),
    val missedCallsCount: Int = 0,
    val isLoading: Boolean = false,
    val exportedFile: File? = null,
    val selectedEntryForSheet: CallLogEntry? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val showBulkDeleteConfirmDialog: Boolean = false
)

class RecentsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalimApplication
    private val callLogRepo = app.callLogRepository
    private val telephonyRepo = app.telephonyRepository
    private val blockedRepo = app.blockedNumbersRepository
    private val dtmfHaptic = app.dtmfAndHaptic

    private val _uiState = MutableStateFlow(RecentsUiState())
    val uiState: StateFlow<RecentsUiState> = _uiState.asStateFlow()

    init {
        loadRecents()
    }

    fun loadRecents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val logs = callLogRepo.getCallLogs(
                searchQuery = _uiState.value.searchQuery,
                filterType = _uiState.value.activeFilter
            )
            val missed = callLogRepo.getMissedCallCount()
            _uiState.value = _uiState.value.copy(
                callLogs = logs,
                missedCallsCount = missed,
                isLoading = false
            )
        }
    }

    fun setFilter(filter: CallType?) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
        loadRecents()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadRecents()
    }

    fun onEntryClicked(entry: CallLogEntry) {
        if (_uiState.value.isMultiSelectMode) {
            toggleSelectEntry(entry.id)
        } else {
            dtmfHaptic.triggerCommitHaptic()
            telephonyRepo.placeCall(entry.number)
        }
    }

    fun onEntryLongPressed(entry: CallLogEntry) {
        dtmfHaptic.triggerCommitHaptic()
        _uiState.value = _uiState.value.copy(selectedEntryForSheet = entry)
    }

    fun dismissSheet() {
        _uiState.value = _uiState.value.copy(selectedEntryForSheet = null)
    }

    fun toggleMultiSelectMode() {
        val current = _uiState.value.isMultiSelectMode
        _uiState.value = _uiState.value.copy(
            isMultiSelectMode = !current,
            selectedEntryIds = emptySet()
        )
    }

    fun toggleSelectEntry(id: Long) {
        val current = _uiState.value.selectedEntryIds.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _uiState.value = _uiState.value.copy(selectedEntryIds = current)
    }

    fun selectAll() {
        val allIds = _uiState.value.callLogs.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedEntryIds = allIds)
    }

    fun requestDeleteSingle(entry: CallLogEntry) {
        _uiState.value = _uiState.value.copy(
            selectedEntryForSheet = entry,
            showDeleteConfirmDialog = true
        )
    }

    fun confirmDeleteSingle() {
        val entry = _uiState.value.selectedEntryForSheet ?: return
        viewModelScope.launch {
            dtmfHaptic.triggerDestructiveHaptic()
            callLogRepo.deleteEntries(entry.rawIds)
            _uiState.value = _uiState.value.copy(
                selectedEntryForSheet = null,
                showDeleteConfirmDialog = false
            )
            loadRecents()
        }
    }

    fun cancelDeleteSingle() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = false)
    }

    fun requestBulkDelete() {
        if (_uiState.value.selectedEntryIds.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(showBulkDeleteConfirmDialog = true)
        }
    }

    fun confirmBulkDelete() {
        viewModelScope.launch {
            val selected = _uiState.value.selectedEntryIds
            val allRawIds = _uiState.value.callLogs
                .filter { selected.contains(it.id) }
                .flatMap { it.rawIds }

            dtmfHaptic.triggerDestructiveHaptic()
            callLogRepo.deleteEntries(allRawIds)
            _uiState.value = _uiState.value.copy(
                isMultiSelectMode = false,
                selectedEntryIds = emptySet(),
                showBulkDeleteConfirmDialog = false
            )
            loadRecents()
        }
    }

    fun cancelBulkDelete() {
        _uiState.value = _uiState.value.copy(showBulkDeleteConfirmDialog = false)
    }

    fun blockNumber(number: String) {
        viewModelScope.launch {
            blockedRepo.blockNumber(number)
            dismissSheet()
        }
    }

    fun exportToCsv() {
        viewModelScope.launch {
            val file = callLogRepo.exportCallLogsToCsv()
            _uiState.value = _uiState.value.copy(exportedFile = file)
        }
    }

    fun clearExportNotice() {
        _uiState.value = _uiState.value.copy(exportedFile = null)
    }
}
