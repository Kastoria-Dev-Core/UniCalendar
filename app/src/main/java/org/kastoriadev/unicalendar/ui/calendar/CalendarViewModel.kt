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
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.data.preferences.UserPreferences
import org.kastoriadev.unicalendar.data.sync.ScheduleSyncManager
import org.kastoriadev.unicalendar.domain.model.AppLanguage
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.DisplayLayoutMode
import org.kastoriadev.unicalendar.domain.model.EventType
import org.kastoriadev.unicalendar.domain.model.SyncResult
import org.kastoriadev.unicalendar.domain.model.ThemeMode
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import org.kastoriadev.unicalendar.util.UiText
import java.io.InputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

class CalendarViewModel(
    private val repository: EventRepository,
    private val syncManager: ScheduleSyncManager,
    private val userPreferences: UserPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CalendarUiState(
            themeMode = userPreferences?.themeMode ?: ThemeMode.SYSTEM,
            displayLayoutMode = userPreferences?.displayLayoutMode ?: DisplayLayoutMode.SPLIT,
            appLanguage = userPreferences?.appLanguage ?: AppLanguage.SYSTEM,
            selectedSemester = userPreferences?.selectedSemester,
            includePde = userPreferences?.includePde ?: false,
            showSemesterPrompt = userPreferences?.let { !it.hasCompletedOnboarding } ?: false,
            canDismissSemesterPrompt = userPreferences?.hasCompletedOnboarding ?: true
        )
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            repository.getAllEvents()
                .catch { emit(emptyList()) }
                .collect { events ->
                    _uiState.update { current ->
                        val filtered = filterEvents(events, current.selectedSemester, current.includePde)
                        val grouped = filtered.groupBy { it.startTime.toLocalDate() }
                        current.copy(
                            allEvents = events,
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

    fun setThemeMode(mode: ThemeMode) {
        userPreferences?.themeMode = mode
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setAppLanguage(language: AppLanguage) {
        userPreferences?.appLanguage = language
        _uiState.update { it.copy(appLanguage = language) }
    }

    fun setDisplayLayoutMode(mode: DisplayLayoutMode) {
        userPreferences?.displayLayoutMode = mode
        _uiState.update { it.copy(displayLayoutMode = mode) }
    }

    fun openSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun dismissSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun openDayDetailsSheet() {
        _uiState.update { it.copy(showDayDetailsSheet = true) }
    }

    fun dismissDayDetailsSheet() {
        _uiState.update { it.copy(showDayDetailsSheet = false) }
    }

    fun setSemesterFilter(semester: String?) {
        val target = if (semester.equals("ALL", ignoreCase = true)) null else semester
        userPreferences?.selectedSemester = target
        _uiState.update { current ->
            val filtered = filterEvents(current.allEvents, target, current.includePde)
            val grouped = filtered.groupBy { it.startTime.toLocalDate() }
            current.copy(
                selectedSemester = target,
                monthEvents = grouped,
                selectedDateEvents = grouped[current.selectedDate] ?: emptyList()
            )
        }
    }

    fun onSemesterConfirmed(semester: String, includePde: Boolean) {
        val targetSemester = if (semester.equals("ALL", ignoreCase = true)) null else semester
        userPreferences?.let { prefs ->
            prefs.selectedSemester = targetSemester
            prefs.includePde = includePde
            prefs.hasCompletedOnboarding = true
        }

        _uiState.update { current ->
            val filtered = filterEvents(current.allEvents, targetSemester, includePde)
            val grouped = filtered.groupBy { it.startTime.toLocalDate() }
            current.copy(
                selectedSemester = targetSemester,
                includePde = includePde,
                showSemesterPrompt = false,
                canDismissSemesterPrompt = true,
                monthEvents = grouped,
                selectedDateEvents = grouped[current.selectedDate] ?: emptyList()
            )
        }
    }

    fun openSemesterPrompt() {
        _uiState.update { it.copy(showSemesterPrompt = true, canDismissSemesterPrompt = true) }
    }

    fun dismissSemesterPrompt() {
        _uiState.update { it.copy(showSemesterPrompt = false) }
    }

    private fun filterEvents(
        events: List<CalendarEvent>,
        semester: String?,
        includePde: Boolean
    ): List<CalendarEvent> {
        if (semester.isNullOrBlank() || semester.equals("ALL", ignoreCase = true)) {
            return events
        }
        return events.filter { event ->
            val sem = event.semester.trim()
            if (sem.equals(semester, ignoreCase = true)) {
                true
            } else if (includePde && sem.equals("PDE", ignoreCase = true)) {
                true
            } else if (sem.isBlank()) {
                // User-created general event not tied to a uni course
                true
            } else {
                false
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        _uiState.update { current ->
            val shouldOpenSheet = current.displayLayoutMode == DisplayLayoutMode.FULLSCREEN
            current.copy(
                selectedDate = date,
                currentMonth = YearMonth.from(date),
                currentWeekStart = weekStart,
                selectedDateEvents = current.monthEvents[date] ?: emptyList(),
                showDayDetailsSheet = shouldOpenSheet
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
        semester: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        type: EventType
    ) {
        viewModelScope.launch {
            val event = CalendarEvent(
                title = title.trim(),
                description = description.trim(),
                location = location.trim(),
                semester = semester.trim(),
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

    fun syncFromGitHub(targetSemester: String? = null) {
        val githubTimetableUrl = "https://raw.githubusercontent.com/Kastoria-Dev-Core/UniCalendar/main/app/src/main/assets/default_timetable.docx"
        syncSchedule(githubTimetableUrl, targetSemester)
    }

    fun syncSchedule(url: String, targetSemester: String? = null) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, showSyncDialog = false) }
            val result = syncManager.syncFromUrl(url.trim(), targetSemester)
            val message = when (result) {
                is SyncResult.Success -> UiText.StringResource(R.string.sync_success_format, result.importedCount)
                is SyncResult.Failure -> UiText.StringResource(R.string.sync_failed_format, result.message)
            }
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = message
                )
            }
        }
    }

    fun syncFromStream(stream: InputStream, fileName: String?, targetSemester: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, showSyncDialog = false) }
            val result = syncManager.syncFromStream(stream, fileName, targetSemester)
            val message = when (result) {
                is SyncResult.Success -> UiText.StringResource(R.string.sync_success_stream_format, result.importedCount)
                is SyncResult.Failure -> UiText.StringResource(R.string.import_failed_format, result.message)
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
        private val syncManager: ScheduleSyncManager,
        private val userPreferences: UserPreferences? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                return CalendarViewModel(
                    repository = repository,
                    syncManager = syncManager,
                    userPreferences = userPreferences
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
