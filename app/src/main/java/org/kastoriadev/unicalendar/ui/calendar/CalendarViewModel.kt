package org.kastoriadev.unicalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kastoriadev.unicalendar.data.sync.ScheduleSyncManager
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventType
import org.kastoriadev.unicalendar.domain.model.SyncResult
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class CalendarViewModel(
    private val repository: EventRepository,
    private val syncManager: ScheduleSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            repository.getAllEvents()
                .catch { emit(emptyList()) }
                .collect { events ->
                    val grouped = events.groupBy { it.startTime.toLocalDate() }
                    _uiState.update { current ->
                        current.copy(
                            monthEvents = grouped,
                            selectedDateEvents = grouped[current.selectedDate] ?: emptyList()
                        )
                    }
                }
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { current ->
            current.copy(
                selectedDate = date,
                currentMonth = YearMonth.from(date),
                selectedDateEvents = current.monthEvents[date] ?: emptyList()
            )
        }
    }

    fun onPreviousMonth() {
        _uiState.update { current ->
            current.copy(currentMonth = current.currentMonth.minusMonths(1))
        }
    }

    fun onNextMonth() {
        _uiState.update { current ->
            current.copy(currentMonth = current.currentMonth.plusMonths(1))
        }
    }

    fun onTodaySelected() {
        val today = LocalDate.now()
        onDateSelected(today)
    }

    fun setAddDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddDialog = visible) }
    }

    fun setSyncDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSyncDialog = visible) }
    }

    fun dismissSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }

    fun addEvent(
        title: String,
        description: String,
        location: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        type: EventType
    ) {
        viewModelScope.launch {
            val event = CalendarEvent(
                title = title.trim(),
                description = description.trim(),
                location = location.trim(),
                startTime = startTime,
                endTime = endTime,
                type = type,
                isSynced = false
            )
            repository.insertEvent(event)
            setAddDialogVisible(false)
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun syncSchedule(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, showSyncDialog = false) }
            val result = syncManager.syncFromUrl(url.trim())
            val message = when (result) {
                is SyncResult.Success -> "Successfully imported ${result.importedCount} scheduled events"
                is SyncResult.Failure -> "Sync failed: ${result.message}"
            }
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = message
                )
            }
        }
    }

    class Factory(
        private val repository: EventRepository,
        private val syncManager: ScheduleSyncManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                return CalendarViewModel(repository, syncManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
