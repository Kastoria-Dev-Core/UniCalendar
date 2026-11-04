package org.kastoriadev.unicalendar.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.LocalDateTime

class IcsScheduleParserTest {

    private val parser = IcsScheduleParser()

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
