package org.kastoriadev.unicalendar.ui.calendar

import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class CalendarUiState(
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val selectedSemester: String? = null, // e.g. "1", "2", "3", "4", "5", "6", "7", "8", "PDE", "ALL"
    val includePde: Boolean = false,
    val showSemesterPrompt: Boolean = false,
    val canDismissSemesterPrompt: Boolean = true,
    val currentMonth: YearMonth = YearMonth.now(),
    val currentWeekStart: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val selectedDate: LocalDate = LocalDate.now(),
    val allEvents: List<CalendarEvent> = emptyList(),
    val monthEvents: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val selectedDateEvents: List<CalendarEvent> = emptyList(),
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showSyncDialog: Boolean = false,
    val selectedEventForNotes: CalendarEvent? = null
)
