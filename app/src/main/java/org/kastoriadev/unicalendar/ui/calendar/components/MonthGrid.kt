package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Largest day cell in split mode; keeps the grid compact on wide screens and in landscape. */
private val MaxSplitCellSize = 56.dp

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
    val weeks = remember(currentMonth) { calculateCalendarDays(currentMonth).chunked(7) }

    if (isFullScreen) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            for (week in weeks) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (date in week) {
                        DayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            isCurrentMonth = YearMonth.from(date) == currentMonth,
                            events = eventsMap[date].orEmpty(),
                            onDateClick = onDateClick,
                            modifier = Modifier.weight(1f),
                            isFullScreen = true
                        )
                    }
                }
            }
        }
    } else {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // Square cells sized by the width, but never taller than the space allows (landscape).
            val byWidth = maxWidth / 7
            val byHeight = if (constraints.hasBoundedHeight) maxHeight / weeks.size else MaxSplitCellSize
            val cellSize: Dp = min(min(byWidth, byHeight), MaxSplitCellSize)

            Column(modifier = Modifier.fillMaxWidth()) {
                for (week in weeks) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cellSize)
                    ) {
                        for (date in week) {
                            DayCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == today,
                                isCurrentMonth = YearMonth.from(date) == currentMonth,
                                events = eventsMap[date].orEmpty(),
                                onDateClick = onDateClick,
                                modifier = Modifier.weight(1f),
                                isFullScreen = false,
                                circleSize = cellSize
                            )
                        }
                    }
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
