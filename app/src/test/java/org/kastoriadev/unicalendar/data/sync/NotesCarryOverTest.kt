package org.kastoriadev.unicalendar.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import java.time.LocalDateTime

class NotesCarryOverTest {

    private fun lesson(title: String, start: LocalDateTime, notes: String = "") = CalendarEvent(
        title = title,
        notes = notes,
        startTime = start,
        endTime = start.plusHours(2),
        isSynced = true
    )

    private val monday9 = LocalDateTime.of(2026, 11, 2, 9, 0)

    @Test
    fun movesNotesToTheSameCourseOnTheSameDay() {
        val previous = listOf(
            lesson("Πιθανότητες - Στατιστική", monday9, notes = "Ασκήσεις κεφ. 3"),
            lesson("Φυσική", monday9.plusWeeks(1), notes = "Εργασία 2")
        )
        // New timetable version: spacing changed and the class moved one hour later.
        val incoming = listOf(
            lesson("Πιθανότητες-Στατιστική", monday9.plusHours(1)),
            lesson("Φυσική", monday9.plusWeeks(1)),
            lesson("Φυσική", monday9.plusWeeks(2))
        )

        val merged = NotesCarryOver.apply(previous, incoming)

        assertEquals(listOf("Ασκήσεις κεφ. 3", "Εργασία 2", ""), merged.map { it.notes })
    }

    @Test
    fun renamedCourseKeepsNotesWhenItsSlotIsUnchanged() {
        val previous = listOf(lesson("Εισαγωγή σε Υπολογιστές", monday9, notes = "Quiz"))
        val incoming = listOf(lesson("Εισαγωγή στους Υπολογιστές", monday9), lesson("Φυσική", monday9.plusHours(2)))

        val merged = NotesCarryOver.apply(previous, incoming)

        assertEquals(listOf("Quiz", ""), merged.map { it.notes })
    }

    @Test
    fun eachNoteIsUsedOnceAndUnmatchedNotesAreDropped() {
        val previous = listOf(
            lesson("Φυσική", monday9, notes = "A"),
            lesson("Μεταγλωττιστές", monday9.plusDays(1), notes = "B")
        )
        val incoming = listOf(lesson("Φυσική", monday9), lesson("Φυσική", monday9.plusHours(3)))

        val merged = NotesCarryOver.apply(previous, incoming)

        assertEquals(listOf("A", ""), merged.map { it.notes })
    }
}
