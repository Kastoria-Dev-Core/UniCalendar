package org.kastoriadev.unicalendar.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class IcsScheduleParserTest {

    // UTC so the "Z" times below read back unchanged; conversion is covered separately.
    private val parser = IcsScheduleParser(ZoneOffset.UTC)

    @Test
    fun parse_validIcsContent_extractsEventsCorrectly() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Kastoria Dev Core//UniCalendar//EN
            BEGIN:VEVENT
            SUMMARY:Software Engineering Lecture
            DESCRIPTION:Prof. Smith, Chapter 4 Design Patterns
            LOCATION:Auditorium 1
            DTSTART:20261110T090000Z
            DTEND:20261110T110000Z
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Databases Lab Exam
            DESCRIPTION:Final lab evaluation
            LOCATION:Lab 3
            DTSTART:20261112T140000Z
            DTEND:20261112T160000Z
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics, "https://example.edu/schedule.ics")

        assertEquals(2, events.size)

        val first = events[0]
        assertEquals("Software Engineering Lecture", first.title)
        assertEquals("Auditorium 1", first.location)
        assertEquals(EventType.LECTURE, first.type)
        assertEquals(LocalDateTime.of(2026, 11, 10, 9, 0, 0), first.startTime)
        assertEquals(LocalDateTime.of(2026, 11, 10, 11, 0, 0), first.endTime)
        assertTrue(first.isSynced)

        val second = events[1]
        assertEquals("Databases Lab Exam", second.title)
        assertEquals("Lab 3", second.location)
        assertEquals(EventType.EXAM, second.type)
    }

    @Test
    fun parse_convertsUtcAndTzidTimesToTheLocalZone() {
        val athens = IcsScheduleParser(ZoneId.of("Europe/Athens"))
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Moodle quiz
            DTSTART:20261110T090000Z
            DTEND:20261110T100000Z
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Seminar
            DTSTART;TZID=Europe/London:20261110T090000
            DTEND;TZID=Europe/London:20261110T100000
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Floating
            DTSTART:20261110T090000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = athens.parse(ics)

        // Greece is UTC+2 in November; floating times stay as written.
        assertEquals(LocalDateTime.of(2026, 11, 10, 11, 0), events[0].startTime)
        assertEquals(LocalDateTime.of(2026, 11, 10, 12, 0), events[0].endTime)
        assertEquals(LocalDateTime.of(2026, 11, 10, 11, 0), events[1].startTime)
        assertEquals(LocalDateTime.of(2026, 11, 10, 9, 0), events[2].startTime)
        assertEquals(LocalDateTime.of(2026, 11, 10, 10, 0), events[2].endTime)
    }

    @Test
    fun parse_ignoresAlarmTextAndCancelledEvents() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Networks lecture
            DESCRIPTION:Room B1
            DTSTART:20261110T090000Z
            BEGIN:VALARM
            ACTION:DISPLAY
            DESCRIPTION:This is an event reminder
            END:VALARM
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Cancelled lab
            STATUS:CANCELLED
            DTSTART:20261111T090000Z
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)

        assertEquals(1, events.size)
        assertEquals("Room B1", events[0].description)
    }

    @Test
    fun parse_expandsWeeklyRecurrenceWithExceptionsAndOverrides() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:os-lecture
            SUMMARY:Operating Systems
            DTSTART:20261005T090000
            DTEND:20261005T110000
            RRULE:FREQ=WEEKLY;COUNT=4
            EXDATE:20261012T090000
            END:VEVENT
            BEGIN:VEVENT
            UID:os-lecture
            RECURRENCE-ID:20261019T090000
            SUMMARY:Operating Systems
            DTSTART:20261020T090000
            DTEND:20261020T110000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val starts = parser.parse(ics).map { it.startTime.toLocalDate() }.sorted()

        assertEquals(
            listOf(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 26)),
            starts
        )
    }

    @Test
    fun parse_expandsByDayUntil() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Lab
            DTSTART:20261005T150000
            DTEND:20261005T170000
            RRULE:FREQ=WEEKLY;BYDAY=MO,WE;UNTIL=20261014T235959Z
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val starts = parser.parse(ics).map { it.startTime.toLocalDate() }

        assertEquals(
            listOf(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14)),
            starts
        )
    }

    @Test
    fun parse_unescapesTextInOnePass() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Lab\, room 2\; group A
            DESCRIPTION:Line one\nLine two \\n stays literal
            DTSTART:20261110T090000Z
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val event = parser.parse(ics).single()

        assertEquals("Lab, room 2; group A", event.title)
        assertEquals("Line one\nLine two \\n stays literal", event.description)
    }

    @Test
    fun parse_handlesMultilineUnfolding() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Operating Systems Long Course
              Title Continued
            LOCATION:Room 204
            DTSTART:20261115T100000Z
            DTEND:20261115T120000Z
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val events = parser.parse(ics)
        assertEquals(1, events.size)
        assertEquals("Operating Systems Long Course Title Continued", events[0].title)
    }
}
