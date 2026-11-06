package org.kastoriadev.unicalendar.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AcademicCalendarTest {

    @Test
    fun orthodoxEaster_matchesKnownDates() {
        assertEquals(LocalDate.of(2025, 4, 20), AcademicCalendar.orthodoxEaster(2025))
        assertEquals(LocalDate.of(2026, 4, 12), AcademicCalendar.orthodoxEaster(2026))
        assertEquals(LocalDate.of(2027, 5, 2), AcademicCalendar.orthodoxEaster(2027))
    }

    @Test
    fun isNoLectureDay_coversHolidaysAndBreaks() {
        val holidays = listOf(
            LocalDate.of(2026, 10, 28),
            LocalDate.of(2026, 11, 17),
            LocalDate.of(2026, 12, 24),
            LocalDate.of(2027, 1, 6),
            LocalDate.of(2027, 1, 30),
            LocalDate.of(2026, 3, 25),
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 2, 23), // Clean Monday
            LocalDate.of(2026, 4, 6), // Holy Monday
            LocalDate.of(2026, 4, 13), // Easter Monday
            LocalDate.of(2026, 6, 1) // Whit Monday
        )
        holidays.forEach { assertTrue("$it should be a holiday", AcademicCalendar.isNoLectureDay(it)) }

        val lectureDays = listOf(
            LocalDate.of(2026, 10, 27),
            LocalDate.of(2026, 11, 18),
            LocalDate.of(2027, 1, 7),
            LocalDate.of(2026, 4, 20), // after Thomas Sunday
            LocalDate.of(2026, 2, 16)
        )
        lectureDays.forEach { assertFalse("$it should have lectures", AcademicCalendar.isNoLectureDay(it)) }
    }

    @Test
    fun termStart_readsHeadingThenFileNameThenToday() {
        val today = LocalDate.of(2026, 11, 6)

        assertEquals(
            LocalDate.of(2026, 10, 5),
            AcademicCalendar.termStart("Ωρολόγιο Πρόγραμμα μαθημάτων Χειμερινού Εξαμήνου Aκαδ. Έτους 2026-2027", null, today)
        )
        assertEquals(
            LocalDate.of(2026, 2, 9),
            AcademicCalendar.termStart("Ωρολόγιο Πρόγραμμα μαθημάτων Εαρινού Εξαμήνου Aκαδ. Έτους 2025-2026", null, today)
        )
        assertEquals(
            LocalDate.of(2026, 2, 9),
            AcademicCalendar.termStart("", "https://cs.uowm.gr/wp-content/uploads/2026/04/Timetable_EE_2025-2026_v9.4-2.docx", today)
        )
        assertEquals(
            LocalDate.of(2026, 10, 5),
            AcademicCalendar.termStart("", "https://cs.uowm.gr/wp-content/uploads/2026/10/Timetable_%CE%A7E_2026-2027_V1.2-1.docx", today)
        )
        assertEquals(LocalDate.of(2026, 10, 5), AcademicCalendar.termStart("", null, today))
        assertEquals(LocalDate.of(2027, 2, 8), AcademicCalendar.termStart("", null, LocalDate.of(2027, 3, 1)))
    }
}
