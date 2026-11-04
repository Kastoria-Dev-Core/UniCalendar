package org.kastoriadev.unicalendar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kastoriadev.unicalendar.ui.calendar.components.calculateCalendarDays
import java.time.DayOfWeek
import java.time.YearMonth

class MonthGridTest {

    @Test
    fun calculateCalendarDays_alwaysStartsOnMondayAndEndsOnSunday() {
        val testMonths = listOf(
            YearMonth.of(2026, 1),
            YearMonth.of(2026, 2),
            YearMonth.of(2026, 11),
            YearMonth.of(2027, 2)
        )

        for (month in testMonths) {
            val days = calculateCalendarDays(month)
            assertEquals("Total days must be a multiple of 7", 0, days.size % 7)
            assertEquals("First day must be Monday", DayOfWeek.MONDAY, days.first().dayOfWeek)
            assertEquals("Last day must be Sunday", DayOfWeek.SUNDAY, days.last().dayOfWeek)
            assertTrue("Days must include all days of target month", days.contains(month.atDay(1)))
            assertTrue("Days must include last day of target month", days.contains(month.atEndOfMonth()))
        }
    }
}
