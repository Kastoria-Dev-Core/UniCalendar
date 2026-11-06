package org.kastoriadev.unicalendar.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.kastoriadev.unicalendar.BuildConfig
import org.kastoriadev.unicalendar.data.parser.ScheduleDocumentParser
import org.kastoriadev.unicalendar.data.sync.UniversityScheduleLocator.Schedule
import org.kastoriadev.unicalendar.domain.model.EventFeed
import org.kastoriadev.unicalendar.domain.model.SyncError
import org.kastoriadev.unicalendar.domain.model.SyncResult
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import org.kastoriadev.unicalendar.util.toErrorDetails
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStream
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ScheduleSyncManager(
    private val repository: EventRepository,
    client: OkHttpClient = defaultHttpClient(),
    private val locator: UniversityScheduleLocator = UniversityScheduleLocator(client),
    private val documentParser: ScheduleDocumentParser = ScheduleDocumentParser(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private sealed interface FeedOutcome {
        data class Imported(val feed: EventFeed, val count: Int, val label: String?) : FeedOutcome
        data class Failed(val error: SyncError) : FeedOutcome
    }

    /**
     * Downloads the department's current timetable and exam schedule and replaces the previously
     * synced copies. A source that fails keeps its old events; the other is still updated.
     *
     * @param targetSemester semester key to import ("1".."8", "PDE"), or null/"ALL" for everything.
     */
    suspend fun syncUniversitySchedule(targetSemester: String?): SyncResult = withContext(ioDispatcher) {
        val semester = normalizeSemester(targetSemester)
        val (timetable, exams) = coroutineScope {
            val timetable = async { syncPublished(Schedule.TIMETABLE, EventFeed.TIMETABLE, semester) }
            val exams = async { syncPublished(Schedule.EXAMS, EventFeed.EXAMS, semester) }
            timetable.await() to exams.await()
        }

        if (timetable is FeedOutcome.Failed && exams is FeedOutcome.Failed) {
            return@withContext SyncResult.Failure(timetable.error)
        }
        SyncResult.Success(
            lessonCount = (timetable as? FeedOutcome.Imported)?.count ?: 0,
            examCount = (exams as? FeedOutcome.Imported)?.count ?: 0,
            timetableError = (timetable as? FeedOutcome.Failed)?.error,
            examsError = (exams as? FeedOutcome.Failed)?.error,
            timetableLabel = (timetable as? FeedOutcome.Imported)?.label,
            examsLabel = (exams as? FeedOutcome.Imported)?.label
        )
    }

    /**
     * Imports a file picked by the user. The format is detected from the content: a .docx
     * timetable or exam schedule, an .ics calendar or an HTML timetable page.
     *
     * @param openStream opens the file; it is called (and the stream closed) on the IO dispatcher,
     *   since content providers such as cloud storage may block while downloading.
     * @param fileName resolves the display name, also on the IO dispatcher.
     */
    suspend fun importFile(
        openStream: () -> InputStream?,
        fileName: () -> String?,
        targetSemester: String?
    ): SyncResult = withContext(ioDispatcher) {
        val bytes = try {
            openStream()?.use { UniversityScheduleLocator.readBounded(it, MAX_IMPORT_BYTES) }
        } catch (_: Exception) {
            null
        } ?: return@withContext SyncResult.Failure(SyncError.FileUnreadable)
        val name = try {
            fileName()
        } catch (_: Exception) {
            null
        }

        val outcome = try {
            store(documentParser.parse(bytes, name), normalizeSemester(targetSemester), null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            FeedOutcome.Failed(failureOf(e))
        }
        when (outcome) {
            is FeedOutcome.Failed -> SyncResult.Failure(outcome.error)
            is FeedOutcome.Imported -> when (outcome.feed) {
                EventFeed.TIMETABLE -> SyncResult.Success(lessonCount = outcome.count)
                EventFeed.EXAMS -> SyncResult.Success(examCount = outcome.count)
                else -> SyncResult.Success(otherCount = outcome.count)
            }
        }
    }

    private suspend fun syncPublished(schedule: Schedule, feed: EventFeed, semester: String?): FeedOutcome =
        try {
            val document = locator.locate(schedule)
            val bytes = locator.download(document.url)
            store(documentParser.parse(bytes, document.url, expected = feed), semester, document.label)
        } catch (e: SyncException) {
            FeedOutcome.Failed(e.error)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            FeedOutcome.Failed(failureOf(e))
        }

    /**
     * A broken archive or XML is the document's fault and gets the usual message. Anything else,
     * Errors included, is a bug: its details are passed on to be shown instead of crashing the app.
     */
    private fun failureOf(error: Throwable): SyncError = when (error) {
        is IOException, is SAXException -> SyncError.NothingRecognized
        else -> SyncError.Unexpected(error.toErrorDetails())
    }

    private suspend fun store(
        result: ScheduleDocumentParser.Result,
        semester: String?,
        label: String?
    ): FeedOutcome {
        val parsed = result as? ScheduleDocumentParser.Result.Parsed
            ?: return FeedOutcome.Failed(SyncError.UnsupportedFormat)
        if (parsed.events.isEmpty()) return FeedOutcome.Failed(SyncError.NothingRecognized)

        // The document is authoritative for the dates it covers; older terms stay untouched.
        val from = parsed.events.minOf { it.startTime }.toLocalDate().atStartOfDay()
        val to = parsed.events.maxOf { it.startTime }.toLocalDate().atTime(LocalTime.MAX)
        val selected = if (semester == null) {
            parsed.events
        } else {
            parsed.events.filter { it.semesters.isEmpty() || semester in it.semesters }
        }
        repository.replaceSyncedEvents(parsed.feed, semester, from, to, selected)

        val count = when (parsed.feed) {
            // A weekly lesson repeats all term; report courses rather than occurrences.
            EventFeed.TIMETABLE -> selected.distinctBy { it.title.lowercase() }.size
            else -> selected.size
        }
        return FeedOutcome.Imported(parsed.feed, count, label)
    }

    private fun normalizeSemester(semester: String?): String? =
        semester?.trim()?.takeUnless { it.isEmpty() || it.equals("ALL", ignoreCase = true) }

    companion object {
        private const val MAX_IMPORT_BYTES = 20L * 1024 * 1024
        private val USER_AGENT = "UniCalendar-Android/${BuildConfig.VERSION_NAME}"

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
            }
            .build()
    }
}
