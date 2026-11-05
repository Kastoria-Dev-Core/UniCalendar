package org.kastoriadev.unicalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kastoriadev.unicalendar.data.sync.ScheduleSyncManager
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventType
import org.kastoriadev.unicalendar.domain.model.SyncResult
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

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

    fun setViewMode(mode: CalendarViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun onDateSelected(date: LocalDate) {
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        _uiState.update { current ->
            current.copy(
                selectedDate = date,
                currentMonth = YearMonth.from(date),
                currentWeekStart = weekStart,
                selectedDateEvents = current.monthEvents[date] ?: emptyList()
            )
        }
    }

    fun onPreviousMonth() {
        _uiState.update { current ->
            val prevMonth = current.currentMonth.minusMonths(1)
            val newSelectedDate = prevMonth.atDay(current.selectedDate.dayOfMonth.coerceAtMost(prevMonth.lengthOfMonth()))
            current.copy(
                currentMonth = prevMonth,
                selectedDate = newSelectedDate,
                currentWeekStart = newSelectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                selectedDateEvents = current.monthEvents[newSelectedDate] ?: emptyList()
            )
        }
    }

    fun onNextMonth() {
        _uiState.update { current ->
            val nextMonth = current.currentMonth.plusMonths(1)
            val newSelectedDate = nextMonth.atDay(current.selectedDate.dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth()))
            current.copy(
                currentMonth = nextMonth,
                selectedDate = newSelectedDate,
                currentWeekStart = newSelectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                selectedDateEvents = current.monthEvents[newSelectedDate] ?: emptyList()
            )
        }
    }

    fun onPreviousWeek() {
        _uiState.update { current ->
            val prevWeekStart = current.currentWeekStart.minusWeeks(1)
            val dayOffset = current.selectedDate.dayOfWeek.value - DayOfWeek.MONDAY.value
            val newSelectedDate = prevWeekStart.plusDays(dayOffset.toLong())
            current.copy(
                currentWeekStart = prevWeekStart,
                selectedDate = newSelectedDate,
                currentMonth = YearMonth.from(newSelectedDate),
                selectedDateEvents = current.monthEvents[newSelectedDate] ?: emptyList()
            )
        }
    }

    fun onNextWeek() {
        _uiState.update { current ->
            val nextWeekStart = current.currentWeekStart.plusWeeks(1)
            val dayOffset = current.selectedDate.dayOfWeek.value - DayOfWeek.MONDAY.value
            val newSelectedDate = nextWeekStart.plusDays(dayOffset.toLong())
            current.copy(
                currentWeekStart = nextWeekStart,
                selectedDate = newSelectedDate,
                currentMonth = YearMonth.from(newSelectedDate),
                selectedDateEvents = current.monthEvents[newSelectedDate] ?: emptyList()
            )
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

    fun onEditNoteClick(event: CalendarEvent) {
        _uiState.update { it.copy(selectedEventForNotes = event) }
    }

    fun onDismissNoteDialog() {
        _uiState.update { it.copy(selectedEventForNotes = null) }
    }

    fun onSaveNotes(eventId: Long, notes: String) {
        viewModelScope.launch {
            repository.updateNotes(eventId, notes)
            onDismissNoteDialog()
        }
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
