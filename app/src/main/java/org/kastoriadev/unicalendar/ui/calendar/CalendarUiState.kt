package org.kastoriadev.unicalendar.ui.calendar

import org.kastoriadev.unicalendar.domain.model.AppLanguage
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.DisplayLayoutMode
import org.kastoriadev.unicalendar.domain.model.ThemeMode
import org.kastoriadev.unicalendar.util.UiText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class CalendarUiState(
    /** True until the launch animation has played once (kept across rotations). */
    val showStartupSplash: Boolean = true,
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val displayLayoutMode: DisplayLayoutMode = DisplayLayoutMode.SPLIT,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val appLanguage: AppLanguage = AppLanguage.GREEK,
    val selectedSemester: String? = null,
    val includePde: Boolean = false,
    val showSemesterPrompt: Boolean = false,
    val canDismissSemesterPrompt: Boolean = true,
    val showSettingsScreen: Boolean = false,
    val showDayDetailsSheet: Boolean = false,
    val currentMonth: YearMonth = YearMonth.now(),
    val currentWeekStart: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val selectedDate: LocalDate = LocalDate.now(),
    val allEvents: List<CalendarEvent> = emptyList(),
    val monthEvents: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val selectedDateEvents: List<CalendarEvent> = emptyList(),
    val isSyncing: Boolean = false,
    val userMessage: UserMessage? = null,
    /** An error that would otherwise have crashed the app, shown in a dialog until dismissed. */
    val errorReport: ErrorReport? = null,
    val lastSyncedAt: LocalDateTime? = null,
    val lastSyncLabel: String? = null,
    val showAddDialog: Boolean = false,
    val showSyncDialog: Boolean = false,
    val selectedEventForNotes: CalendarEvent? = null
) {
    /** False until a timetable has been synced or an event added; drives the empty state. */
    val hasAnyEvents: Boolean get() = allEvents.isNotEmpty()
}

/** A one-off snackbar message, optionally with an action button. */
data class UserMessage(
    val text: UiText,
    val action: MessageAction? = null,
    /** Distinguishes consecutive messages with the same text. */
    val id: Long = System.nanoTime()
)

enum class MessageAction {
    UNDO_DELETE
}

/** Technical details of a failure, shown so they can be copied and reported. */
data class ErrorReport(val kind: Kind, val details: String) {
    enum class Kind {
        /** A sync or file import failed unexpectedly; the app kept running. */
        SYNC,

        /** The app crashed the last time it ran. */
        CRASH
    }
}
