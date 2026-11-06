package org.kastoriadev.unicalendar.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kastoriadev.unicalendar.ui.calendar.components.monthTitle
import org.kastoriadev.unicalendar.ui.calendar.components.weekRangeTitle
import org.kastoriadev.unicalendar.ui.common.uppercaseFor
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class CalendarTextFormattingTest {

    private val greek = Locale("el", "GR")

    @Test
    fun monthTitle_usesTheNominativeMonthName() {
        // "MMMM" would give the genitive "Νοεμβρίου 2026".
        assertEquals("Νοέμβριος 2026", monthTitle(YearMonth.of(2026, 11), greek))
        assertEquals("November 2026", monthTitle(YearMonth.of(2026, 11), Locale.ENGLISH))
    }

    @Test
    fun weekRangeTitle_isCompact() {
        assertEquals("2 – 8 Nov 2026", weekRangeTitle(LocalDate.of(2026, 11, 2), Locale.ENGLISH))
        assertEquals("30 Nov – 6 Dec 2026", weekRangeTitle(LocalDate.of(2026, 11, 30), Locale.ENGLISH))
        assertEquals("28 Dec 2026 – 3 Jan 2027", weekRangeTitle(LocalDate.of(2026, 12, 28), Locale.ENGLISH))
    }

    @Test
    fun uppercaseFor_dropsGreekAccents() {
        assertEquals("ΤΡΙ", "Τρί".uppercaseFor(greek))
        assertEquals("ΣΑΒ", "Σάβ".uppercaseFor(greek))
        assertEquals("TUE", "Tue".uppercaseFor(Locale.ENGLISH))
    }
}
