package org.kastoriadev.unicalendar.ui.calendar

import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class CalendarUiState(
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val selectedSemesterFilter: String? = null, // null means "ALL", otherwise "1", "3", "5", "7", "PDE"
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
