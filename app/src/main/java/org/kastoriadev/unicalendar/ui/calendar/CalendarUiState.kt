package org.kastoriadev.unicalendar.ui.calendar

import org.kastoriadev.unicalendar.domain.model.AppLanguage
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.DisplayLayoutMode
import org.kastoriadev.unicalendar.domain.model.ThemeMode
import org.kastoriadev.unicalendar.util.UiText
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class CalendarUiState(
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
    val syncMessage: UiText? = null,
    val showAddDialog: Boolean = false,
    val showSyncDialog: Boolean = false,
    val selectedEventForNotes: CalendarEvent? = null
) {
    val showSettingsDialog: Boolean get() = showSettingsScreen
}
