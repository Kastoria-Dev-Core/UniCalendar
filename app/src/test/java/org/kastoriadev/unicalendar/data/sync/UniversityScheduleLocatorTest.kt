package org.kastoriadev.unicalendar.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kastoriadev.unicalendar.data.sync.UniversityScheduleLocator.Schedule

class UniversityScheduleLocatorTest {

    @Test
    fun parseWatcherResponse_readsCurrentDocument() {
        // Shape of https://uowm-exam-watcher.vercel.app/api/check-timetable?dryRun=1
        val json = """
            {"ok":true,"dryRun":true,"redisConnected":true,
             "current":{"url":"https://cs.uowm.gr/wp-content/uploads/2026/10/Timetable_%CE%A7E_2026-2027_V1.2-1.docx","label":"Timetable_ΧE_2026-2027_V1.2"},
             "lastSeen":"https://cs.uowm.gr/wp-content/uploads/2026/10/Timetable_%CE%A7E_2026-2027_V1.2-1.docx",
             "isNew":false,"posted":false}
        """.trimIndent()

        val document = UniversityScheduleLocator.parseWatcherResponse(json)

        assertEquals("https://cs.uowm.gr/wp-content/uploads/2026/10/Timetable_%CE%A7E_2026-2027_V1.2-1.docx", document?.url)
        assertEquals("Timetable_ΧE_2026-2027_V1.2", document?.label)
    }

    @Test
    fun parseWatcherResponse_fallsBackToLastSeenAndRejectsErrors() {
        val lastSeenOnly = UniversityScheduleLocator.parseWatcherResponse(
            """{"ok":true,"current":null,"lastSeen":"https://cs.uowm.gr/wp-content/uploads/2026/08/Exams_CS_Sep_2026_updated.docx"}"""
        )
        assertEquals("https://cs.uowm.gr/wp-content/uploads/2026/08/Exams_CS_Sep_2026_updated.docx", lastSeenOnly?.url)
        assertEquals("Exams_CS_Sep_2026_updated.docx", lastSeenOnly?.label)

        assertNull(UniversityScheduleLocator.parseWatcherResponse("""{"ok":false,"error":"Exams page fetch failed: 503"}"""))
        assertNull(UniversityScheduleLocator.parseWatcherResponse("""{"error":"unauthorized"}"""))
        assertNull(UniversityScheduleLocator.parseWatcherResponse("""{"ok":true,"current":{"url":"javascript:alert(1)"}}"""))
        assertNull(UniversityScheduleLocator.parseWatcherResponse("""{"ok":true,"current":null,"lastSeen":null}"""))
    }

    @Test
    fun selectDocument_timetable_picksNewestUploadInsidePageContent() {
        val html = """
            <html><body>
              <aside><a href="/wp-content/uploads/2025/05/Quality-Manual.pdf">Εγχειρίδιο Ποιότητας</a></aside>
              <article><div class="entry-content">
                <a href="/wp-content/uploads/2026/04/Timetable_EE_2025-2026_v9.4-2.docx">Timetable_EE_2025-2026_v9.4</a>
                <a href="/wp-content/uploads/2026/10/Timetable_XE_2026-2027_V1.2-1.docx">Timetable_ΧE_2026-2027_V1.2</a>
                <a href="/archiki-selida/">Αρχική</a>
              </div></article>
            </body></html>
        """.trimIndent()

        val document = UniversityScheduleLocator.selectDocument(
            Schedule.TIMETABLE,
            html,
            "https://cs.uowm.gr/archiki-selida/orologio-programma-mathimaton/"
        )

        assertEquals("https://cs.uowm.gr/wp-content/uploads/2026/10/Timetable_XE_2026-2027_V1.2-1.docx", document?.url)
        assertEquals("Timetable_ΧE_2026-2027_V1.2", document?.label)
    }

    @Test
    fun selectDocument_exams_picksFirstDocumentEvenIfNotDocx() {
        val html = """
            <html><body>
              <nav><a href="/wp-content/uploads/2019/08/FEK.pdf">ΦΕΚ Ίδρυσης</a></nav>
              <div class="entry-content">
                <a href="/wp-content/uploads/2022/08/exams_2022_sept_v6.pdf">Πρόγραμμα Επαναληπτικών Εξετάσεων Σεπτεμβρίου</a>
                <a href="/wp-content/uploads/2022/05/exams_21_22EE_v5.docx">Πρόγραμμα Εξετάσεων Ε.Ε.</a>
              </div>
            </body></html>
        """.trimIndent()

        val document = UniversityScheduleLocator.selectDocument(
            Schedule.EXAMS,
            html,
            "https://cs.uowm.gr/archiki-selida/programma-spoudwn/programma-exetaseon/"
        )

        // The current schedule is the first link; an older .docx must not be imported instead.
        assertTrue(document!!.url.endsWith("exams_2022_sept_v6.pdf"))
    }
}
