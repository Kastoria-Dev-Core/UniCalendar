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
import org.kastoriadev.unicalendar.domain.model.SyncError
import org.kastoriadev.unicalendar.domain.model.SyncResult
import org.kastoriadev.unicalendar.domain.model.ThemeMode
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import org.kastoriadev.unicalendar.util.UiText
import java.io.InputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class CalendarViewModel(
    private val repository: EventRepository,
    private val syncManager: ScheduleSyncManager,
    private val userPreferences: UserPreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CalendarUiState(
            viewMode = userPreferences?.calendarViewModeName
                ?.let { name -> CalendarViewMode.entries.firstOrNull { it.name == name } }
                ?: CalendarViewMode.MONTH,
            themeMode = userPreferences?.themeMode ?: ThemeMode.LIGHT,
            displayLayoutMode = userPreferences?.displayLayoutMode ?: DisplayLayoutMode.SPLIT,
            appLanguage = userPreferences?.appLanguage ?: AppLanguage.GREEK,
            selectedSemester = userPreferences?.selectedSemester,
            includePde = userPreferences?.includePde ?: false,
            showSemesterPrompt = userPreferences?.let { !it.hasCompletedOnboarding } ?: false,
            canDismissSemesterPrompt = userPreferences?.hasCompletedOnboarding ?: true,
            lastSyncedAt = userPreferences?.lastSyncEpochMillis?.takeIf { it > 0 }?.let { millis ->
                LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
            },
            lastSyncLabel = userPreferences?.lastSyncLabel
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

    fun onStartupSplashFinished() {
        _uiState.update { it.copy(showStartupSplash = false) }
    }

    fun setViewMode(mode: CalendarViewMode) {
        userPreferences?.calendarViewModeName = mode.name
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
        _uiState.update { it.copy(displayLayoutMode = mode, showDayDetailsSheet = false) }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettingsScreen = true) }
    }

    fun dismissSettings() {
        _uiState.update { it.copy(showSettingsScreen = false) }
    }

    fun openSettingsDialog() = openSettings()
    fun dismissSettingsDialog() = dismissSettings()

    fun clearAllSchedules() {
        viewModelScope.launch {
            repository.clearAllEvents()
            userPreferences?.let { prefs ->
                prefs.selectedSemester = null
                prefs.lastSyncEpochMillis = 0L
                prefs.lastSyncLabel = null
            }
            _uiState.update { current ->
                current.copy(
                    selectedSemester = null,
                    allEvents = emptyList(),
                    monthEvents = emptyMap(),
                    selectedDateEvents = emptyList(),
                    lastSyncedAt = null,
                    lastSyncLabel = null,
                    syncMessage = UiText.StringResource(R.string.msg_schedules_cleared)
                )
            }
        }
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
            // Exams can belong to several semesters ("5,6"); events without one are shown everywhere.
            val semesters = event.semesters
            semesters.isEmpty() ||
                semesters.any { it.equals(semester, ignoreCase = true) } ||
                (includePde && CalendarEvent.SEMESTER_PDE in semesters)
        }
    }

    /** A day was tapped: select it, and in full-screen mode open its details. */
    fun onDateSelected(date: LocalDate) {
        navigateTo(date, openDetails = _uiState.value.displayLayoutMode == DisplayLayoutMode.FULLSCREEN)
    }

    /** "Today" only moves the calendar; it does not pop up the day details. */
    fun onTodaySelected() {
        navigateTo(LocalDate.now(), openDetails = false)
    }

    private fun navigateTo(date: LocalDate, openDetails: Boolean) {
        _uiState.update { current ->
            current.copy(
                selectedDate = date,
                currentMonth = YearMonth.from(date),
                currentWeekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                selectedDateEvents = current.monthEvents[date].orEmpty(),
                showDayDetailsSheet = openDetails
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

    /** Imports the department's current timetable and exam schedule. */
    fun syncFromUniversity(targetSemester: String? = null) {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, showSyncDialog = false) }
            val result = syncManager.syncUniversitySchedule(targetSemester)
            if (result is SyncResult.Success) recordSuccessfulSync(result.timetableLabel ?: result.examsLabel)
            _uiState.update { it.copy(isSyncing = false, syncMessage = syncMessage(result)) }
        }
    }

    /** Imports a file picked by the user (.docx timetable / exam schedule, .ics, .html). */
    fun importFile(openStream: () -> InputStream?, fileName: () -> String?, targetSemester: String? = null) {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, showSyncDialog = false) }
            val result = syncManager.importFile(openStream, fileName, targetSemester)
            _uiState.update { it.copy(isSyncing = false, syncMessage = syncMessage(result)) }
        }
    }

    private fun recordSuccessfulSync(label: String?) {
        val now = LocalDateTime.now()
        userPreferences?.let { prefs ->
            prefs.lastSyncEpochMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            prefs.lastSyncLabel = label
        }
        _uiState.update { it.copy(lastSyncedAt = now, lastSyncLabel = label) }
    }

    private fun syncMessage(result: SyncResult): UiText = when (result) {
        is SyncResult.Failure -> UiText.StringResource(R.string.sync_failed_format, errorText(result.error))
        is SyncResult.Success -> {
            val parts = buildList {
                if (result.lessonCount > 0) add(UiText.PluralResource(R.plurals.count_courses, result.lessonCount, result.lessonCount))
                if (result.examCount > 0) add(UiText.PluralResource(R.plurals.count_exams, result.examCount, result.examCount))
                if (result.otherCount > 0) add(UiText.PluralResource(R.plurals.count_events, result.otherCount, result.otherCount))
            }
            val summary = when (parts.size) {
                0 -> UiText.StringResource(R.string.sync_nothing_for_semester)
                1 -> UiText.StringResource(R.string.sync_success_one, parts[0])
                else -> UiText.StringResource(R.string.sync_success_two, parts[0], parts[1])
            }
            when {
                result.examsError != null -> UiText.StringResource(R.string.sync_partial_exams_failed, summary)
                result.timetableError != null -> UiText.StringResource(R.string.sync_partial_timetable_failed, summary)
                else -> summary
            }
        }
    }

    private fun errorText(error: SyncError): UiText = when (error) {
        SyncError.Network -> UiText.StringResource(R.string.sync_error_network)
        is SyncError.Http -> UiText.StringResource(R.string.sync_error_http, error.code)
        SyncError.NoDocumentFound -> UiText.StringResource(R.string.sync_error_no_document)
        SyncError.UnsupportedFormat -> UiText.StringResource(R.string.sync_error_unsupported)
        SyncError.NothingRecognized -> UiText.StringResource(R.string.sync_error_nothing_recognized)
        SyncError.FileUnreadable -> UiText.StringResource(R.string.sync_error_file_unreadable)
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
