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
    modifier: Modifier = Modifier
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
            Row(modifier = Modifier.fillMaxWidth()) {
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
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

fun calculateCalendarDays(yearMonth: YearMonth): List<LocalDate> {
    val days = mutableListOf<LocalDate>()
    val firstDayOfMonth = yearMonth.atDay(1)
    val dayOfWeekValue = firstDayOfMonth.dayOfWeek.value // 1 (Monday) to 7 (Sunday)

    // Leading days from previous month
    val leadingDays = dayOfWeekValue - 1
    if (leadingDays > 0) {
        val prevMonth = yearMonth.minusMonths(1)
        val prevMonthLength = prevMonth.lengthOfMonth()
        for (i in (prevMonthLength - leadingDays + 1)..prevMonthLength) {
            days.add(prevMonth.atDay(i))
        }
    }

    // Days of current month
    val currentMonthLength = yearMonth.lengthOfMonth()
    for (i in 1..currentMonthLength) {
        days.add(yearMonth.atDay(i))
    }

    // Trailing days from next month to fill out the last week
    val remainder = days.size % 7
    if (remainder != 0) {
        val trailingDays = 7 - remainder
        val nextMonth = yearMonth.plusMonths(1)
        for (i in 1..trailingDays) {
            days.add(nextMonth.atDay(i))
        }
    }

    return days
}
