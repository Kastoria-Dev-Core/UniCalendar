package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import org.kastoriadev.unicalendar.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeekHeader(
    weekStart: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val rangeTitle = remember(weekStart, locale) { weekRangeTitle(weekStart, locale) }

    CalendarNavigationHeader(
        title = rangeTitle,
        titleStyle = MaterialTheme.typography.titleLarge,
        previousDescription = stringResource(R.string.action_previous_week),
        nextDescription = stringResource(R.string.action_next_week),
        onPrevious = onPreviousWeek,
        onNext = onNextWeek,
        onTodayClick = onTodayClick,
        modifier = modifier
    )
}

/** Compact week range: "2 – 8 Nov 2026", "30 Nov – 6 Dec 2026", "28 Dec 2026 – 3 Jan 2027". */
internal fun weekRangeTitle(weekStart: LocalDate, locale: Locale): String {
    val weekEnd = weekStart.plusDays(6)
    val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
    return when {
        weekStart.year != weekEnd.year -> "${weekStart.format(dayMonthYear)} – ${weekEnd.format(dayMonthYear)}"
        weekStart.month != weekEnd.month -> "${weekStart.format(dayMonth)} – ${weekEnd.format(dayMonthYear)}"
        else -> "${weekStart.dayOfMonth} – ${weekEnd.format(dayMonthYear)}"
    }
}
