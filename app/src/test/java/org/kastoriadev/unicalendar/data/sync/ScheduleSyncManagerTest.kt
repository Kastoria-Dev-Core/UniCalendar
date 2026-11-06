package org.kastoriadev.unicalendar.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kastoriadev.unicalendar.data.parser.DocxScheduleParser
import org.kastoriadev.unicalendar.data.parser.ScheduleDocumentParser
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventFeed
import org.kastoriadev.unicalendar.domain.model.SyncError
import org.kastoriadev.unicalendar.domain.model.SyncResult
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import org.kastoriadev.unicalendar.testutil.TestDocuments
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList

class ScheduleSyncManagerTest {

    /** Mirrors the Room implementation's replace semantics (feed + date window + semester). */
    private class InMemoryEventRepository : EventRepository {
        private var nextId = 1L
        val events = MutableStateFlow<List<CalendarEvent>>(emptyList())

        /** Thrown by [replaceSyncedEvents] to simulate a failure past the parsers. */
        var failure: Throwable? = null

        override fun getAllEvents(): Flow<List<CalendarEvent>> = events
        override suspend fun getEventById(id: Long) = events.value.firstOrNull { it.id == id }
        override suspend fun insertEvent(event: CalendarEvent): Long {
            val id = if (event.id == 0L) nextId++ else event.id
            events.value = events.value.filterNot { it.id == id } + event.copy(id = id)
            return id
        }
        override suspend fun insertEvents(events: List<CalendarEvent>) = events.map { insertEvent(it) }
        override suspend fun updateEvent(event: CalendarEvent) {
            insertEvent(event)
        }
        override suspend fun updateNotes(id: Long, notes: String) {
            events.value = events.value.map { if (it.id == id) it.copy(notes = notes) else it }
        }
        override suspend fun deleteEvent(event: CalendarEvent) = deleteEventById(event.id)
        override suspend fun deleteEventById(id: Long) {
            events.value = events.value.filterNot { it.id == id }
        }
        override suspend fun replaceSyncedEvents(
            feed: EventFeed,
            semester: String?,
            from: LocalDateTime,
            to: LocalDateTime,
            events: List<CalendarEvent>
        ): Int {
            failure?.let { throw it }
            fun replaced(event: CalendarEvent) = event.isSynced && event.feed == feed &&
                !event.startTime.isBefore(from) && !event.startTime.isAfter(to) &&
                (semester == null || event.semesters.isEmpty() || semester in event.semesters)

            val previous = this.events.value.filter(::replaced)
            val merged = NotesCarryOver.apply(previous, events.map { it.copy(id = 0, feed = feed, isSynced = true) })
            this.events.value = this.events.value.filterNot(::replaced) + merged.map { it.copy(id = nextId++) }
            return merged.size
        }
        override suspend fun clearAllEvents() {
            events.value = emptyList()
        }
    }

    private lateinit var server: MockWebServer
    private val repository = InMemoryEventRepository()
    private val watcherRequests = CopyOnWriteArrayList<RecordedRequest>()
    private val routes = mutableMapOf<String, MockResponse>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath.orEmpty()
                if (path.startsWith("/api/")) watcherRequests += request
                return routes[path] ?: MockResponse().setResponseCode(404)
            }
        }
        server.start()

        routes["/api/check-timetable"] = watcherAnswer("/files/Timetable_XE_2026-2027.docx", "Timetable_ΧE_2026-2027_V1.2")
        routes["/api/check-exams"] = watcherAnswer("/files/Exams_CS_Sep_2026.docx", "Πρόγραμμα Επαναληπτικών Εξετάσεων Σεπτεμβρίου")
        routes["/files/Timetable_XE_2026-2027.docx"] = file(TestDocuments.docx(TestDocuments.winterTimetableXml()))
        routes["/files/Exams_CS_Sep_2026.docx"] = file(TestDocuments.docx(TestDocuments.examScheduleXml()))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun manager(): ScheduleSyncManager {
        val client = OkHttpClient()
        val locator = UniversityScheduleLocator(
            client = client,
            watcherBaseUrl = server.url("/").toString(),
            departmentPageUrl = { server.url("/page/${it.name}").toString() }
        )
        val parser = ScheduleDocumentParser(timetableParser = DocxScheduleParser(today = { LocalDate.of(2026, 11, 6) }))
        return ScheduleSyncManager(repository, client, locator, parser)
    }

    private fun watcherAnswer(path: String, label: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody("""{"ok":true,"dryRun":true,"current":{"url":"${server.url(path)}","label":"$label"},"isNew":false,"posted":false}""")

    private fun file(bytes: ByteArray) = MockResponse().setBody(Buffer().write(bytes))

    @Test
    fun syncUniversitySchedule_importsTimetableAndExamsThroughWatcherDryRun() = runBlocking {
        val result = manager().syncUniversitySchedule(targetSemester = null)

        assertTrue(result is SyncResult.Success)
        result as SyncResult.Success
        assertEquals(2, result.lessonCount)
        assertEquals(4, result.examCount)
        assertEquals("Timetable_ΧE_2026-2027_V1.2", result.timetableLabel)

        val stored = repository.events.value
        assertTrue(stored.any { it.feed == EventFeed.TIMETABLE && it.title == "Φυσική" })
        assertEquals(4, stored.count { it.feed == EventFeed.EXAMS })

        // Without dryRun the watcher would post to Discord; the app must never do that.
        assertEquals(2, watcherRequests.size)
        assertTrue(watcherRequests.all { it.requestUrl?.queryParameter("dryRun") == "1" })
    }

    @Test
    fun syncUniversitySchedule_fallsBackToDepartmentPageWhenWatcherRejects() = runBlocking {
        routes["/api/check-timetable"] = MockResponse().setResponseCode(401).setBody("""{"error":"unauthorized"}""")
        routes["/api/check-exams"] = MockResponse().setResponseCode(401).setBody("""{"error":"unauthorized"}""")
        routes["/page/TIMETABLE"] = MockResponse().setBody(
            """<article><div class="entry-content"><a href="/files/Timetable_XE_2026-2027.docx">Ωρολόγιο</a></div></article>"""
        )
        routes["/page/EXAMS"] = MockResponse().setBody(
            """<div class="entry-content"><a href="/files/Exams_CS_Sep_2026.docx">Εξετάσεις</a></div>"""
        )

        val result = manager().syncUniversitySchedule(targetSemester = null)

        assertTrue(result is SyncResult.Success)
        assertEquals(4, (result as SyncResult.Success).examCount)
    }

    @Test
    fun syncUniversitySchedule_keepsTimetableWhenExamScheduleIsPdf() = runBlocking {
        routes["/api/check-exams"] = watcherAnswer("/files/exams.pdf", "Εξετάσεις")
        routes["/files/exams.pdf"] = file("%PDF-1.7\n%âã\n1 0 obj".toByteArray(Charsets.ISO_8859_1))

        val result = manager().syncUniversitySchedule(targetSemester = null) as SyncResult.Success

        assertEquals(2, result.lessonCount)
        assertEquals(0, result.examCount)
        assertEquals(SyncError.UnsupportedFormat, result.examsError)
    }

    @Test
    fun syncUniversitySchedule_reportsDamagedDocumentsInsteadOfCrashing() = runBlocking {
        routes["/files/Exams_CS_Sep_2026.docx"] = file(TestDocuments.docx("<w:document><w:body><w:p>"))

        val result = manager().syncUniversitySchedule(targetSemester = null) as SyncResult.Success

        assertEquals(2, result.lessonCount)
        assertEquals(SyncError.NothingRecognized, result.examsError)
    }

    @Test
    fun syncUniversitySchedule_reportsUnexpectedErrorsWithTheirDetails() = runBlocking {
        // An Error is not an Exception; it has to be reported too instead of crashing the app.
        repository.failure = NoSuchMethodError("java.util.List.removeFirst")

        val result = manager().syncUniversitySchedule(targetSemester = null)

        val error = (result as SyncResult.Failure).error
        assertTrue(error is SyncError.Unexpected)
        assertTrue((error as SyncError.Unexpected).details.startsWith("java.lang.NoSuchMethodError: java.util.List.removeFirst"))
    }

    @Test
    fun syncUniversitySchedule_withoutConnection_failsWithNetworkError() = runBlocking {
        val sync = manager()
        server.shutdown()

        val result = sync.syncUniversitySchedule(targetSemester = null)

        assertEquals(SyncResult.Failure(SyncError.Network), result)
    }

    @Test
    fun resync_replacesOnlySyncedEventsAndKeepsNotes() = runBlocking {
        val sync = manager()
        sync.syncUniversitySchedule(targetSemester = null)
        val physics = repository.events.value.first { it.title == "Φυσική" }
        repository.updateNotes(physics.id, "Κεφάλαιο 2")
        repository.insertEvent(
            CalendarEvent(
                title = "Ομαδική μελέτη",
                startTime = physics.startTime.plusHours(4),
                endTime = physics.startTime.plusHours(5)
            )
        )
        val countBefore = repository.events.value.size

        sync.syncUniversitySchedule(targetSemester = null)

        val events = repository.events.value
        assertEquals(countBefore, events.size)
        assertEquals("Κεφάλαιο 2", events.first { it.title == "Φυσική" && it.startTime == physics.startTime }.notes)
        assertTrue(events.any { it.title == "Ομαδική μελέτη" && !it.isSynced })
    }

    @Test
    fun syncUniversitySchedule_forOneSemester_importsOnlyThatSemester() = runBlocking {
        val result = manager().syncUniversitySchedule(targetSemester = "7") as SyncResult.Success

        assertEquals(0, result.lessonCount)
        assertEquals(1, result.examCount)
        assertEquals(listOf("Υπολογισιμότητα και Πολυπλοκότητα"), repository.events.value.map { it.title }.distinct())
    }

    @Test
    fun importFile_detectsFormatFromContent() = runBlocking {
        val sync = manager()

        val docx = sync.importFile({ ByteArrayInputStream(TestDocuments.docx(TestDocuments.examScheduleXml())) }, { "schedule.docx" }, null)
        assertEquals(4, (docx as SyncResult.Success).examCount)

        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Παράδοση εργασίας
            DTSTART:20261120T100000
            DTEND:20261120T110000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        val calendar = sync.importFile({ ByteArrayInputStream(ics.toByteArray()) }, { "moodle.ics" }, null)
        assertEquals(1, (calendar as SyncResult.Success).otherCount)

        val pdf = sync.importFile({ ByteArrayInputStream("%PDF-1.4\u0000".toByteArray()) }, { "exams.pdf" }, null)
        assertEquals(SyncResult.Failure(SyncError.UnsupportedFormat), pdf)

        val unreadable = sync.importFile({ throw IOException("revoked") }, { null }, null)
        assertEquals(SyncResult.Failure(SyncError.FileUnreadable), unreadable)
    }
}
