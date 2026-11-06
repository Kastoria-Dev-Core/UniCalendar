package org.kastoriadev.unicalendar.data.parser

import java.net.URLDecoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * Greek academic-year rules used when expanding a weekly timetable into dated lessons.
 */
internal object AcademicCalendar {

    enum class Term { WINTER, SPRING }

    private val YEAR_RANGE = Regex("(20\\d{2})\\s*[-–/]\\s*(20\\d{2})")
    private val SHORT_YEAR_RANGE = Regex("(?<!\\d)(\\d{2})\\s*[-–]\\s*(\\d{2})(?!\\d)")

    /**
     * Start date (a Monday) of the term a timetable belongs to.
     *
     * The term and academic year are read from the document heading ("Χειμερινού/Εαρινού
     * Εξαμήνου ... 2026-2027"), then from the file name ("Timetable_ΧE_2026-2027.docx"),
     * and otherwise inferred from [today].
     */
    fun termStart(headerText: String, sourceName: String?, today: LocalDate): LocalDate {
        val heading = GreekText.canonical(headerText)
        val fileName = sourceName?.let(::fileNameOf).orEmpty()
        val canonicalName = GreekText.canonical(fileName)

        val term = when {
            heading.contains("ΧΕΙΜΕΡΙΝ") -> Term.WINTER
            heading.contains("ΕΑΡΙΝ") -> Term.SPRING
            TERM_TOKEN_WINTER.containsMatchIn(heading) -> Term.WINTER
            TERM_TOKEN_SPRING.containsMatchIn(heading) -> Term.SPRING
            TERM_TOKEN_WINTER.containsMatchIn(canonicalName) -> Term.WINTER
            TERM_TOKEN_SPRING.containsMatchIn(canonicalName) -> Term.SPRING
            else -> null
        }

        val academicStartYear = academicStartYear(headerText)
            ?: academicStartYear(fileName)
            ?: shortAcademicStartYear(canonicalName)
            ?: if (today.monthValue >= Month.SEPTEMBER.value) today.year else today.year - 1

        val resolvedTerm = term ?: if (today.monthValue >= Month.SEPTEMBER.value || today.month == Month.JANUARY) {
            Term.WINTER
        } else {
            Term.SPRING
        }

        return when (resolvedTerm) {
            // Winter term lectures start on the first Monday of October.
            Term.WINTER -> LocalDate.of(academicStartYear, Month.OCTOBER, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
            // Spring term lectures start in the second week of February.
            Term.SPRING -> LocalDate.of(academicStartYear + 1, Month.FEBRUARY, 1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
                .plusWeeks(1)
        }
    }

    private val TERM_TOKEN_WINTER = Regex("(^|[^Α-Ω])ΧΕ([^Α-Ω]|$)")
    private val TERM_TOKEN_SPRING = Regex("(^|[^Α-Ω])ΕΕ([^Α-Ω]|$)")

    private fun academicStartYear(text: String): Int? =
        YEAR_RANGE.find(text)?.let { match ->
            val first = match.groupValues[1].toInt()
            val second = match.groupValues[2].toInt()
            first.takeIf { second == first + 1 }
        }

    private fun shortAcademicStartYear(text: String): Int? =
        SHORT_YEAR_RANGE.find(text)?.let { match ->
            val first = match.groupValues[1].toInt()
            val second = match.groupValues[2].toInt()
            if (second == (first + 1) % 100) 2000 + first else null
        }

    private fun fileNameOf(source: String): String {
        val lastSegment = source.substringBefore('?').substringBefore('#').substringAfterLast('/')
        return try {
            URLDecoder.decode(lastSegment, "UTF-8")
        } catch (_: Exception) {
            lastSegment
        }
    }

    /**
     * True on days when Greek universities hold no lectures: national and academic holidays,
     * the Christmas break and the Easter break.
     */
    fun isNoLectureDay(date: LocalDate): Boolean {
        val month = date.month
        val day = date.dayOfMonth
        when {
            month == Month.OCTOBER && day == 28 -> return true // National holiday
            month == Month.NOVEMBER && day == 17 -> return true // Polytechnic anniversary
            month == Month.DECEMBER && day >= 24 -> return true // Christmas break
            month == Month.JANUARY && day <= 6 -> return true // Christmas break / Epiphany
            month == Month.JANUARY && day == 30 -> return true // Three Hierarchs
            month == Month.MARCH && day == 25 -> return true // National holiday
            month == Month.MAY && day == 1 -> return true // Labour day
        }
        val easter = orthodoxEaster(date.year)
        return date == easter.minusDays(48) || // Clean Monday
            (!date.isBefore(easter.minusDays(6)) && !date.isAfter(easter.plusDays(7))) || // Easter break
            date == easter.plusDays(50) // Whit Monday (Αγίου Πνεύματος)
    }

    /** Orthodox Easter Sunday (Gregorian calendar) for years 1900-2099. */
    fun orthodoxEaster(year: Int): LocalDate {
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val month = (d + e + 114) / 31
        val day = (d + e + 114) % 31 + 1
        // Julian -> Gregorian offset is 13 days between 1900 and 2099.
        return LocalDate.of(year, month, day).plusDays(13)
    }
}
