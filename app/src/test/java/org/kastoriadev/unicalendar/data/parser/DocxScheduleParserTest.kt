package org.kastoriadev.unicalendar.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.DayOfWeek

class DocxScheduleParserTest {

    private val parser = DocxScheduleParser()

    @Test
    fun parseDocumentXml_extractsCoursesAndSemesters() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p>
                        <w:r><w:t>ΩΡΟΛΟΓΙΟ ΠΡΟΓΡΑΜΜΑ - 3ο ΕΞΑΜΗΝΟ</w:t></w:r>
                    </w:p>
                    <w:tbl>
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>Δευτέρα</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>09:00 - 12:00</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Αντικειμενοστραφής Προγραμματισμός (Θ)</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Αίθουσα 1</w:t></w:r></w:p></w:tc>
                        </w:tr>
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>Τετάρτη</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>13:00 - 15:00</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Εργαστήριο Δομών Δεδομένων (Ε)</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Εργαστήριο 2</w:t></w:r></w:p></w:tc>
                        </w:tr>
                    </w:tbl>
                    <w:p>
                        <w:r><w:t>ΠΡΟΓΡΑΜΜΑ ΠΔΕ</w:t></w:r>
                    </w:p>
                    <w:tbl>
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>Παρασκευή</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>15:00 - 18:00</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Εκπαιδευτική Τεχνολογία</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>Αμφιθέατρο</w:t></w:r></w:p></w:tc>
                        </w:tr>
                    </w:tbl>
                </w:body>
            </w:document>
        """.trimIndent()

        val events = parser.parseDocumentXml(xml, "test.docx")

        assertTrue("Must extract timetable events", events.isNotEmpty())

        val oopEvents = events.filter { it.title.contains("Αντικειμενοστραφής") }
        assertTrue(oopEvents.isNotEmpty())
        val firstOop = oopEvents[0]
        assertEquals(DayOfWeek.MONDAY, firstOop.startTime.dayOfWeek)
        assertEquals(9, firstOop.startTime.hour)
        assertEquals(12, firstOop.endTime.hour)
        assertEquals("3", firstOop.semester)
        assertEquals(EventType.LECTURE, firstOop.type)
        assertEquals("Αίθουσα 1", firstOop.location)

        val labEvents = events.filter { it.title.contains("Δομών") }
        assertTrue(labEvents.isNotEmpty())
        val firstLab = labEvents[0]
        assertEquals(DayOfWeek.WEDNESDAY, firstLab.startTime.dayOfWeek)
        assertEquals("3", firstLab.semester)
        assertEquals(EventType.LAB, firstLab.type)

        val pdeEvents = events.filter { it.title.contains("Εκπαιδευτική") }
        assertTrue(pdeEvents.isNotEmpty())
        val firstPde = pdeEvents[0]
        assertEquals(DayOfWeek.FRIDAY, firstPde.startTime.dayOfWeek)
        assertEquals("PDE", firstPde.semester)
        assertEquals("Αμφιθέατρο", firstPde.location)
    }
}
