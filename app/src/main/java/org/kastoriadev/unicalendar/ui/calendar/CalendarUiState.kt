package org.kastoriadev.unicalendar.ui.calendar

import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val monthEvents: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val selectedDateEvents: List<CalendarEvent> = emptyList(),
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showSyncDialog: Boolean = false
)
