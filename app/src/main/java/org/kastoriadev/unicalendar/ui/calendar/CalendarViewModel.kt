package org.kastoriadev.unicalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
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
import org.kastoriadev.unicalendar.util.toErrorDetails
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
    private val userPreferences: UserPreferences? = null,
    /** Report of a crash in the previous run, shown once on start. */
    private val lastCrash: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    /** Last deleted event, kept so the deletion can be undone from the snackbar. */
    private var recentlyDeleted: CalendarEvent? = null

    init {
        observeEvents()
    }

    private fun initialState(): CalendarUiState {
        val prefs = userPreferences
        val lastSync = prefs?.lastSyncEpochMillis?.takeIf { it > 0 }?.let { millis ->
            LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        }
        return CalendarUiState(
            viewMode = prefs?.calendarViewModeName
                ?.let { name -> CalendarViewMode.entries.firstOrNull { it.name == name } }
                ?: CalendarViewMode.MONTH,
            themeMode = prefs?.themeMode ?: ThemeMode.LIGHT,
            displayLayoutMode = prefs?.displayLayoutMode ?: DisplayLayoutMode.SPLIT,
            appLanguage = prefs?.appLanguage ?: AppLanguage.GREEK,
            selectedSemester = prefs?.selectedSemester,
            includePde = prefs?.includePde ?: false,
            showSemesterPrompt = prefs?.let { !it.hasCompletedOnboarding } ?: false,
            canDismissSemesterPrompt = prefs?.hasCompletedOnboarding ?: true,
            lastSyncedAt = lastSync,
            lastSyncLabel = prefs?.lastSyncLabel,
            errorReport = lastCrash?.let { ErrorReport(ErrorReport.Kind.CRASH, it) }
        )
    }

    private fun observeEvents() {
        viewModelScope.launch {
            repository.getAllEvents()
                .catch { emit(emptyList()) }
                .collect { events ->
                    _uiState.update { it.withEvents(events = events) }
                }
        }
    }

    /** Recomputes the visible (filtered, grouped by day) events. */
    private fun CalendarUiState.withEvents(
        events: List<CalendarEvent> = allEvents,
        semester: String? = selectedSemester,
        pde: Boolean = includePde
    ): CalendarUiState {
        val grouped = filterEvents(events, semester, pde).groupBy { it.startTime.toLocalDate() }
        return copy(
            allEvents = events,
            selectedSemester = semester,
            includePde = pde,
            monthEvents = grouped,
            selectedDateEvents = grouped[selectedDate].orEmpty()
        )
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
            val semesters = event.semesters
            // Events without a semester (e.g. personal ones) are shown in every calendar.
            semesters.isEmpty() ||
                semesters.any { it.equals(semester, ignoreCase = true) } ||
                (includePde && CalendarEvent.SEMESTER_PDE in semesters)
        }
    }

    // region Startup & settings

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

    fun clearAllSchedules() {
        viewModelScope.launch {
            repository.clearAllEvents()
            recentlyDeleted = null
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
                    userMessage = UserMessage(UiText.StringResource(R.string.msg_schedules_cleared))
                )
            }
        }
    }

    // endregion

    // region Semester

    fun setSemesterFilter(semester: String?) {
        val target = semester?.takeUnless { it.isBlank() || it.equals("ALL", ignoreCase = true) }
        userPreferences?.selectedSemester = target
        _uiState.update { it.withEvents(semester = target) }
    }

    fun setIncludePde(include: Boolean) {
        userPreferences?.includePde = include
        _uiState.update { it.withEvents(pde = include) }
    }

    fun onSemesterConfirmed(semester: String, includePde: Boolean) {
        val target = semester.takeUnless { it.isBlank() || it.equals("ALL", ignoreCase = true) }
        userPreferences?.let { prefs ->
            prefs.selectedSemester = target
            prefs.includePde = includePde
            prefs.hasCompletedOnboarding = true
        }
        _uiState.update { current ->
            current.withEvents(semester = target, pde = includePde).copy(
                showSemesterPrompt = false,
                canDismissSemesterPrompt = true
            )
        }
    }

    fun openSemesterPrompt() {
        _uiState.update { it.copy(showSemesterPrompt = true, canDismissSemesterPrompt = true) }
    }

    fun dismissSemesterPrompt() {
        _uiState.update { it.copy(showSemesterPrompt = false) }
    }

    // endregion

    // region Navigation

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

    fun openDayDetailsSheet() {
        _uiState.update { it.copy(showDayDetailsSheet = true) }
    }

    fun dismissDayDetailsSheet() {
        _uiState.update { it.copy(showDayDetailsSheet = false) }
    }

    fun onPreviousMonth() = shiftMonth(-1)

    fun onNextMonth() = shiftMonth(1)

    private fun shiftMonth(months: Long) {
        _uiState.update { current ->
            val month = current.currentMonth.plusMonths(months)
            val date = month.atDay(current.selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
            current.copy(
                currentMonth = month,
                selectedDate = date,
                currentWeekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                selectedDateEvents = current.monthEvents[date].orEmpty()
            )
        }
    }

    fun onPreviousWeek() = shiftWeek(-1)

    fun onNextWeek() = shiftWeek(1)

    private fun shiftWeek(weeks: Long) {
        _uiState.update { current ->
            val weekStart = current.currentWeekStart.plusWeeks(weeks)
            val date = weekStart.plusDays((current.selectedDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
            current.copy(
                currentWeekStart = weekStart,
                selectedDate = date,
                currentMonth = YearMonth.from(date),
                selectedDateEvents = current.monthEvents[date].orEmpty()
            )
        }
    }

    // endregion

    // region Events & notes

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
        _uiState.update { it.copy(selectedEventForNotes = null) }
        viewModelScope.launch {
            repository.updateNotes(eventId, notes.trim())
        }
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
        _uiState.update { it.copy(showAddDialog = false) }
        viewModelScope.launch {
            repository.insertEvent(
                CalendarEvent(
                    title = title.trim(),
                    description = description.trim(),
                    location = location.trim(),
                    semester = semester.trim(),
                    startTime = startTime,
                    endTime = endTime,
                    type = type,
                    isSynced = false
                )
            )
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event)
            recentlyDeleted = event
            _uiState.update {
                it.copy(
                    userMessage = UserMessage(
                        text = UiText.StringResource(R.string.msg_event_deleted, event.title),
                        action = MessageAction.UNDO_DELETE
                    )
                )
            }
        }
    }

    fun onMessageAction(action: MessageAction) {
        when (action) {
            MessageAction.UNDO_DELETE -> {
                val event = recentlyDeleted ?: return
                recentlyDeleted = null
                // Re-inserting with the same id restores the event exactly, notes included.
                viewModelScope.launch { repository.insertEvent(event) }
            }
        }
    }

    fun onUserMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    // endregion

    // region Sync

    /** Imports the department's current timetable and exam schedule. */
    fun syncFromUniversity(targetSemester: String? = null) {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, showSyncDialog = false) }
            val result = reportingErrors {
                syncManager.syncUniversitySchedule(targetSemester).also { result ->
                    if (result is SyncResult.Success) recordSuccessfulSync(result.timetableLabel ?: result.examsLabel)
                }
            }
            showSyncResult(result)
        }
    }

    /** Imports a file picked by the user (.docx timetable / exam schedule, .ics, .html). */
    fun importFile(openStream: () -> InputStream?, fileName: () -> String?, targetSemester: String? = null) {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, showSyncDialog = false) }
            val result = reportingErrors { syncManager.importFile(openStream, fileName, targetSemester) }
            showSyncResult(result)
        }
    }

    /** Last line of defence: whatever a sync throws is reported in a dialog instead of crashing the app. */
    private suspend fun reportingErrors(sync: suspend () -> SyncResult): SyncResult =
        try {
            sync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            SyncResult.Failure(SyncError.Unexpected(e.toErrorDetails()))
        }

    private fun showSyncResult(result: SyncResult) {
        val errors = when (result) {
            is SyncResult.Failure -> listOf(result.error)
            is SyncResult.Success -> listOfNotNull(result.timetableError, result.examsError)
        }
        val unexpected = errors.filterIsInstance<SyncError.Unexpected>().firstOrNull()
        _uiState.update { current ->
            current.copy(
                isSyncing = false,
                errorReport = unexpected?.let { ErrorReport(ErrorReport.Kind.SYNC, it.details) } ?: current.errorReport,
                // The dialog already explains a complete failure; a partial one still reports what was imported.
                userMessage = if (result is SyncResult.Failure && unexpected != null) null else UserMessage(syncMessage(result))
            )
        }
    }

    fun onErrorReportDismissed() {
        _uiState.update { it.copy(errorReport = null) }
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
        is SyncError.Unexpected -> UiText.StringResource(R.string.sync_error_unexpected)
    }

    // endregion

    class Factory(
        private val repository: EventRepository,
        private val syncManager: ScheduleSyncManager,
        private val userPreferences: UserPreferences? = null,
        private val lastCrash: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
                return CalendarViewModel(
                    repository = repository,
                    syncManager = syncManager,
                    userPreferences = userPreferences,
                    lastCrash = lastCrash
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
