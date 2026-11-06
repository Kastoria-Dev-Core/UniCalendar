package org.kastoriadev.unicalendar.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kastoriadev.unicalendar.domain.model.EventFeed
import org.kastoriadev.unicalendar.domain.model.EventType
import org.kastoriadev.unicalendar.testutil.TestDocuments
import java.time.LocalDate
import java.time.LocalDateTime

/** Lines are taken from the department's exam schedules (2024-2026), which vary in format. */
class DocxExamScheduleParserTest {

    private val parser = DocxExamScheduleParser()

    @Test
    fun parseLines_septemberFormat_readsSemesterInstructorAndRooms() {
        val events = parser.parseLines(
            listOf(
                "Πρόγραμμα Εξετάσεων Σεπτεμβρίου 2025-2026",
                "1η εβδομάδα",
                "Δευτέρα 31-08-2026",
                "09:00-11:00 Εκπαιδευτική Ψυχολογία (Δ’) (ΠΔΕ), (5+0)  Κ. Μόσχου  Αμφ.2 Φ.Π.",
                "15:00-17:00 Επιχειρησιακή Έρευνα (Η’), (22+9)  Γ. Βασιλειάδης  Μ.Αμφ."
            ),
            "exams.docx"
        )

        assertEquals(2, events.size)
        val pde = events[0]
        assertEquals("Εκπαιδευτική Ψυχολογία", pde.title)
        assertEquals("PDE", pde.semester)
        assertEquals(LocalDateTime.of(2026, 8, 31, 9, 0), pde.startTime)
        assertEquals(LocalDateTime.of(2026, 8, 31, 11, 0), pde.endTime)
        assertEquals("Αμφ.2 Φ.Π.", pde.location)
        assertEquals("Διδάσκων: Κ. Μόσχου", pde.description)
        assertEquals(EventType.EXAM, pde.type)
        assertEquals(EventFeed.EXAMS, pde.feed)
        assertTrue(pde.isSynced)
        assertEquals("8", events[1].semester)
    }

    @Test
    fun parseLines_coursesExaminedTogether_areSplitPerSemester() {
        val events = parser.parseLines(
            listOf(
                "Δευτέρα 31-08-2026",
                "11:00-13:00 Αλγόριθμοι και Πολυπλοκότητα (Ε’) / Υπολογισιμότητα και Πολυπλοκότητα (Ζ’), (63+18+9)  Ν. Δημόκας  Μ.Αμφ.",
                "19:00-21:00 Τεχνητή Νοημοσύνη (ΣΤ’)–Λογικός Προγραμματισμός (Η’)  ΣΙΝΑΤΚΑΣ, Μ.ΑΜΦ."
            ),
            null
        )

        assertEquals(
            listOf(
                "Αλγόριθμοι και Πολυπλοκότητα" to "5",
                "Υπολογισιμότητα και Πολυπλοκότητα" to "7",
                "Τεχνητή Νοημοσύνη" to "6",
                "Λογικός Προγραμματισμός" to "8"
            ),
            events.map { it.title to it.semester }
        )
        assertTrue(events.take(2).all { it.location == "Μ.Αμφ." && it.description == "Διδάσκων: Ν. Δημόκας" })
    }

    @Test
    fun parseLines_oneCourseWithSeveralNamesOrSemesters_staysOneEvent() {
        val events = parser.parseLines(
            listOf(
                "Τρίτη 20/1/2026",
                "11:00 – 13:00 Τεχνολογία Πολυμέσων (Ε’/ΣΤ’), Γ.Σίσιας, Μεγ. Αμφ., Αμφ.2 Φ.Π.",
                "13:00 – 15:00 Γραμμική Άλγεβρα (Α’)(Β’) (Εμβόλιμη), Ι.Τουλόπουλος, Αμφ.2 Φ.Π.",
                "15:00 – 16:00 Εισαγωγή στον Προγραμματισμό Υπολογιστών / Προγραμματισμός Ι – ΑΠ (Α’), Ι.Σινάτκας / Γ.Τασκασαπλίδης, Μεγ. Αμφ., Β1",
                "17:00 – 19:00 Αλγόριθμοι & Πολυπλοκότητα/Υπολογισιμότητα και Πολυπλοκότητα (Ε’-Ζ’), Ν.Δημόκας, Μεγ. Αμφ."
            ),
            null
        )

        assertEquals(4, events.size)
        assertEquals("Τεχνολογία Πολυμέσων", events[0].title)
        assertEquals("5,6", events[0].semester)
        assertEquals("Μεγ. Αμφ., Αμφ.2 Φ.Π.", events[0].location)

        assertEquals("Γραμμική Άλγεβρα", events[1].title)
        assertEquals("1,2", events[1].semester)
        assertEquals("Διδάσκων: Ι.Τουλόπουλος\nΕμβόλιμη", events[1].description)

        assertEquals("Εισαγωγή στον Προγραμματισμό Υπολογιστών / Προγραμματισμός Ι – ΑΠ", events[2].title)
        assertEquals("1", events[2].semester)
        assertEquals("Διδάσκοντες: Ι.Σινάτκας, Γ.Τασκασαπλίδης", events[2].description)
        assertEquals("Μεγ. Αμφ., Β1", events[2].location)

        assertEquals("5,7", events[3].semester)
    }

    @Test
    fun parseLines_capitalSurnamesLatinMarkersAndTrailingTags() {
        val events = parser.parseLines(
            listOf(
                "Δευτέρα 8/6/2026",
                "09:00-11:00 Ηλεκτρομαγνητισμός (Α’)  ΣΙΝΑΤΚΑΣ, B11 (Εμβόλιμη)",
                "16:00-18:00 Συνδυαστικά Ψηφιακά Ηλεκτρονικά (B’)  ΔΟΣΗΣ-ΠΛΙΑΤΣΙΟΣ, Μ.ΑΜΦ., ΑΜΦ.2, Β1, Β2",
                "13:00-15:00 Πληροφοριακά Συστήματα Διοίκησης (Παλαιό ΠΠΣ ΤΕΙ)  ΔΗΜΟΚΑΣ, Μ. ΑΜΦ. (Εμβόλιμη)"
            ),
            null
        )

        assertEquals("1", events[0].semester)
        assertEquals("B11", events[0].location)
        assertEquals("Διδάσκων: ΣΙΝΑΤΚΑΣ\nΕμβόλιμη", events[0].description)

        // Latin "B" in the semester marker
        assertEquals("2", events[1].semester)
        assertEquals("Διδάσκων: ΔΟΣΗΣ-ΠΛΙΑΤΣΙΟΣ", events[1].description)
        assertEquals("Μ.ΑΜΦ., ΑΜΦ.2, Β1, Β2", events[1].location)

        assertEquals("Πληροφοριακά Συστήματα Διοίκησης", events[2].title)
        assertEquals("", events[2].semester)
        assertEquals("Μ. ΑΜΦ.", events[2].location)
        assertEquals("Διδάσκων: ΔΗΜΟΚΑΣ\nΠαλαιό ΠΠΣ ΤΕΙ\nΕμβόλιμη", events[2].description)
    }

    @Test
    fun parseLines_ignoresHolidaysNotesAndLinesBeforeTheFirstDay() {
        val events = parser.parseLines(
            listOf(
                "09:00-11:00 Μάθημα χωρίς ημερομηνία (Α’)  Κ. Μόσχου  Αμφ.2",
                "Δευτέρα 10/6/2024 Αργία λόγω Ευρωεκλογών 2024",
                "Τρίτη 11/6/2024",
                "09:00-11:00 Εισαγωγή στους Υπολογιστές (Α’)  ΦΩΤΙΑΔΗΣ, ΑΜΦ.2 *** (Εμβόλιμη)",
                "ΣΗΜΕΙΩΣΕΙΣ",
                "Αλγοριθμική (Παλαιό ΠΠΣ ΤΕΙ)  ΚΑΛΟΓΗΡΑΤΟΥ  **Εξετάζεται με απαλλακτική εργασία."
            ),
            null
        )

        assertEquals(1, events.size)
        assertEquals(LocalDate.of(2024, 6, 11), events[0].startTime.toLocalDate())
        assertEquals("ΑΜΦ.2", events[0].location)
        assertEquals("Διδάσκων: ΦΩΤΙΑΔΗΣ\nΕμβόλιμη", events[0].description)
    }

    @Test
    fun parseDocumentXml_readsParagraphDocument() {
        val events = parser.parseDocumentXml(TestDocuments.examScheduleXml(), "Exams_CS_Sep_2026.docx")

        assertEquals(4, events.size)
        assertEquals(setOf("PDE", "5", "7", "1"), events.map { it.semester }.toSet())
        assertTrue(events.all { it.type == EventType.EXAM })
        assertEquals(LocalDate.of(2026, 9, 1), events.last().startTime.toLocalDate())
    }
}
