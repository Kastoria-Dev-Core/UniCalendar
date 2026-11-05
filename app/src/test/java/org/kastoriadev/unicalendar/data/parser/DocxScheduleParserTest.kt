package org.kastoriadev.unicalendar.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kastoriadev.unicalendar.domain.model.EventType
import java.io.File
import java.io.FileInputStream
import java.time.DayOfWeek
import java.time.LocalDate

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

    @Test
    fun parseDocumentXml_gridLayout_withVerticalMerge() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                <w:body>
                    <w:p><w:r><w:t>Ωρολόγιο Πρόγραμμα μαθημάτων Χειμερινού Εξαμήνου 2026-2027</w:t></w:r></w:p>
                    <w:p><w:r><w:t>Α΄ εξάμηνο</w:t></w:r></w:p>
                    <w:tbl>
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>ΩΡΑ</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>ΔΕΥΤΕΡΑ</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:t>ΤΡΙΤΗ</w:t></w:r></w:p></w:tc>
                        </w:tr>
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>09:00 - 10:00</w:t></w:r></w:p></w:tc>
                            <w:tc><w:tcPr><w:vMerge w:val="restart"/></w:tcPr><w:p><w:r><w:t>Ηλεκτρονική</w:t><w:br/><w:t>Δ. Φωτιάδης</w:t><w:br/><w:t>Μεγ. Αμφιθέατρο</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p/></w:tc>
                        </w:tr>
                        <w:tr>
                            <w:tc><w:p><w:r><w:t>10:00 - 11:00</w:t></w:r></w:p></w:tc>
                            <w:tc><w:tcPr><w:vMerge/></w:tcPr><w:p/></w:tc>
                            <w:tc><w:p/></w:tc>
                        </w:tr>
                    </w:tbl>
                </w:body>
            </w:document>
        """.trimIndent()

        val events = parser.parseDocumentXml(xml, "grid_test.docx")
        assertTrue(events.isNotEmpty())
        val first = events[0]
        assertEquals("Ηλεκτρονική", first.title)
        assertEquals("1", first.semester)
        assertEquals(DayOfWeek.MONDAY, first.startTime.dayOfWeek)
        assertEquals(9, first.startTime.hour)
        assertEquals(11, first.endTime.hour)
        assertTrue(first.description.contains("Φωτιάδης"))
        assertEquals("Μεγ. Αμφιθέατρο", first.location)
    }

    @Test
    fun parseRealTimetableDocx_identifiesSemestersAndCoursesCorrectly() {
        val file = File("Timetable_ΧE_2026-2027_V1.2-1.docx")
        if (!file.exists()) return // Skip if file is moved

        val events = FileInputStream(file).use { stream ->
            parser.parseStream(stream, file.name)
        }

        assertTrue("Events must not be empty", events.isNotEmpty())

        val semesters = events.map { it.semester }.distinct()
        assertTrue("Must contain Semester 1", semesters.contains("1"))
        assertTrue("Must contain Semester 3", semesters.contains("3"))
        assertTrue("Must contain Semester 5", semesters.contains("5"))
        assertTrue("Must contain Semester 7", semesters.contains("7"))
        assertTrue("Must contain Semester PDE", semesters.contains("PDE"))

        // Verify that academic calendar dates cover October 2026 through January 2027
        val minDate = events.minOf { it.startTime.toLocalDate() }
        val maxDate = events.maxOf { it.endTime.toLocalDate() }
        assertEquals(LocalDate.of(2026, 10, 5), minDate)
        assertTrue(maxDate >= LocalDate.of(2027, 1, 15))

        // Verify Semester 1 lessons
        val sem1Events = events.filter { it.semester == "1" }
        val titles1 = sem1Events.map { it.title }.distinct()
        assertTrue("Sem 1 has Ηλεκτρονική", titles1.any { it.contains("Ηλεκτρονική") })
        assertTrue("Sem 1 has Εισαγωγή στους Υπολογιστές", titles1.any { it.contains("Εισαγωγή στους Υπολογιστές") })
        assertTrue("Sem 1 has Φυσική", titles1.any { it.contains("Φυσική") })
        assertTrue("Sem 1 has Μαθηματική Ανάλυση Ι", titles1.any { it.contains("Μαθηματική Ανάλυση Ι") })
        assertTrue("Sem 1 has Διακριτά Μαθηματικά", titles1.any { it.contains("Διακριτά Μαθηματικά") })

        // Check time span and instructor for Sem 1 Ηλεκτρονική (Tue 09:00 - 11:00)
        val hlektroniki = sem1Events.first { it.title.contains("Ηλεκτρονική") && it.startTime.dayOfWeek == DayOfWeek.TUESDAY }
        assertEquals(9, hlektroniki.startTime.hour)
        assertEquals(11, hlektroniki.endTime.hour)
        assertTrue(hlektroniki.description.contains("Φωτιάδης"))
        assertTrue(hlektroniki.location.contains("Αμφιθέατρο"))

        // Verify Semester 3
        val sem3Events = events.filter { it.semester == "3" }
        val titles3 = sem3Events.map { it.title }.distinct()
        assertTrue("Sem 3 has Μεταγλωττιστές", titles3.any { it.contains("Μεταγλωττιστές") })
        assertTrue("Sem 3 has Αριθμητική Ανάλυση", titles3.any { it.contains("Αριθμητική Ανάλυση") })
        assertTrue("Sem 3 has Προγραμματισμός στο Διαδίκτυο", titles3.any { it.contains("Προγραμματισμός στο Διαδίκτυο") })

        // Verify Semester 5
        val sem5Events = events.filter { it.semester == "5" }
        val titles5 = sem5Events.map { it.title }.distinct()
        assertTrue("Sem 5 has Κατανεμημένα Συστήματα", titles5.any { it.contains("Κατανεμημένα Συστήματα") })
        assertTrue("Sem 5 has Τεχνολογία Λογισμικού", titles5.any { it.contains("Τεχνολογία Λογισμικού") })

        // Verify Semester 7
        val sem7Events = events.filter { it.semester == "7" }
        val titles7 = sem7Events.map { it.title }.distinct()
        assertTrue("Sem 7 has Αλγόριθμοι Μηχανικής Μάθησης", titles7.any { it.contains("Αλγόριθμοι Μηχανικής Μάθησης") })
        assertTrue("Sem 7 has Ασφάλεια Δικτύων", titles7.any { it.contains("Ασφάλεια Δικτύων") })

        // Verify PDE (Pedagogical Certification)
        val pdeEvents = events.filter { it.semester == "PDE" }
        val titlesPde = pdeEvents.map { it.title }.distinct()
        assertTrue("PDE has Ανάπτυξη Ψηφιακού Υλικού", titlesPde.any { it.contains("Ανάπτυξη Ψηφιακού Υλικού") })
        assertTrue("PDE has Εισαγωγή στην Παιδαγωγική", titlesPde.any { it.contains("Εισαγωγή στην Παιδαγωγική") })
        assertTrue("PDE has Διδακτική Μεθοδολογία", titlesPde.any { it.contains("Διδακτική Μεθοδολογία") })

        // Check 3-hour duration of Thursday PDE class (13:00 - 16:00)
        val pdeThu = pdeEvents.first { it.startTime.dayOfWeek == DayOfWeek.THURSDAY }
        assertEquals(13, pdeThu.startTime.hour)
        assertEquals(16, pdeThu.endTime.hour)
        assertTrue(pdeThu.description.contains("Πέλλας"))
        assertEquals("Β1", pdeThu.location)

        // Verify Thursday, November 5, 2026 (Week 5) has accurate Sem 1 classes
        val nov5Sem1 = sem1Events.filter { it.startTime.toLocalDate() == LocalDate.of(2026, 11, 5) }
        assertTrue("Nov 5 Sem 1 has classes", nov5Sem1.isNotEmpty())
        val nov5Titles = nov5Sem1.map { it.title }
        assertTrue(nov5Titles.any { it.contains("Εισαγωγή στους Υπολογιστές") })
        assertTrue(nov5Titles.any { it.contains("Μαθηματική Ανάλυση Ι") })
    }
}
