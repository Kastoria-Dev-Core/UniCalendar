package org.kastoriadev.unicalendar.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.DayOfWeek

class HtmlScheduleParserTest {

    private val parser = HtmlScheduleParser()

    @Test
    fun parse_htmlTableTimetable_extractsWeeklyEvents() {
        val html = """
            <html>
            <body>
                <h2>Weekly Timetable</h2>
                <table>
                    <thead>
                        <tr><th>Day</th><th>Time</th><th>Course</th><th>Classroom</th></tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Monday</td>
                            <td>09:00 - 11:00</td>
                            <td>Algorithms and Complexity</td>
                            <td>Room 101</td>
                        </tr>
                        <tr>
                            <td>Τετάρτη</td>
                            <td>12:00 - 14:00</td>
                            <td>Εργαστήριο Δικτύων</td>
                            <td>Εργαστήριο 2</td>
                        </tr>
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent()

        val events = parser.parse(html, "https://example.edu/timetable.html")

        assertTrue("Must extract recurring semester events", events.isNotEmpty())

        val algorithms = events.filter { it.title.contains("Algorithms") }
        assertTrue(algorithms.isNotEmpty())
        assertEquals(DayOfWeek.MONDAY, algorithms[0].startTime.dayOfWeek)
        assertEquals(9, algorithms[0].startTime.hour)
        assertEquals(11, algorithms[0].endTime.hour)

        val networksLab = events.filter { it.title.contains("Δικτύων") }
        assertTrue(networksLab.isNotEmpty())
        assertEquals(DayOfWeek.WEDNESDAY, networksLab[0].startTime.dayOfWeek)
        assertEquals(EventType.LAB, networksLab[0].type)
    }
}
