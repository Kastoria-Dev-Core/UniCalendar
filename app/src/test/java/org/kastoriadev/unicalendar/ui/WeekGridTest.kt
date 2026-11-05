package org.kastoriadev.unicalendar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kastoriadev.unicalendar.ui.calendar.components.calculateWeekDays
import java.time.DayOfWeek
import java.time.LocalDate

class WeekGridTest {

    @Test
    fun calculateWeekDays_alwaysReturnsMondayThroughSunday() {
        val testDates = listOf(
            LocalDate.of(2026, 11, 4), // Wednesday
            LocalDate.of(2026, 11, 2), // Monday
            LocalDate.of(2026, 11, 8), // Sunday
            LocalDate.of(2026, 12, 31) // Thursday
        )

        for (date in testDates) {
            val days = calculateWeekDays(date)
            assertEquals("Week must contain exactly 7 days", 7, days.size)
            assertEquals("First day must be Monday", DayOfWeek.MONDAY, days.first().dayOfWeek)
            assertEquals("Last day must be Sunday", DayOfWeek.SUNDAY, days.last().dayOfWeek)
            assertTrue("Days must be contiguous", days.zipWithNext().all { it.first.plusDays(1) == it.second })
        }
    }
}
