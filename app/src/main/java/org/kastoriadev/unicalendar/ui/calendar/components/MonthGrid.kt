package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    eventsMap: Map<LocalDate, List<CalendarEvent>>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false
) {
    val today = LocalDate.now()
    val days = calculateCalendarDays(currentMonth)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        val weeks = days.chunked(7)
        for (week in weeks) {
            val rowModifier = if (isFullScreen) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier.fillMaxWidth()
            }

            Row(modifier = rowModifier) {
                for (date in week) {
                    val isCurrentMonth = date.month == currentMonth.month && date.year == currentMonth.year
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val dayEvents = eventsMap[date] ?: emptyList()

                    DayCell(
                        date = date,
                        isSelected = isSelected,
                        isToday = isToday,
                        isCurrentMonth = isCurrentMonth,
                        events = dayEvents,
                        onDateClick = onDateClick,
                        modifier = Modifier.weight(1f),
                        isFullScreen = isFullScreen
                    )
                }
            }
        }
    }
}

fun calculateCalendarDays(month: YearMonth): List<LocalDate> {
    val firstDayOfMonth = month.atDay(1)
    val lastDayOfMonth = month.atEndOfMonth()

    val daysFromPrevMonth = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val startDate = firstDayOfMonth.minusDays(daysFromPrevMonth.toLong())

    val daysToNextMonth = (DayOfWeek.SUNDAY.value - lastDayOfMonth.dayOfWeek.value + 7) % 7
    val endDate = lastDayOfMonth.plusDays(daysToNextMonth.toLong())

    val days = mutableListOf<LocalDate>()
    var current = startDate
    while (!current.isAfter(endDate)) {
        days.add(current)
        current = current.plusDays(1)
    }

    // Ensure 5 or 6 full weeks
    while (days.size < 35 || (days.size % 7 != 0)) {
        days.add(current)
        current = current.plusDays(1)
    }

    return days
}
