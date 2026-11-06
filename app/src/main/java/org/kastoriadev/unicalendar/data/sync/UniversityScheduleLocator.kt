package org.kastoriadev.unicalendar.data.sync

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.kastoriadev.unicalendar.data.parser.collapseWhitespace
import org.kastoriadev.unicalendar.domain.model.SyncError
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder

/** A sync step failed for a reason the UI can explain. */
class SyncException(val error: SyncError, cause: Throwable? = null) : Exception(error.toString(), cause)

/**
 * Finds and downloads the current timetable / exam-schedule document published by the UOWM
 * Department of Informatics.
 *
 * The uowm-exam-watcher service already tracks which document is current, so it is asked first.
 * If it is unreachable or rejects the request, the department page is read directly with the same
 * selection rules the watcher uses.
 */
class UniversityScheduleLocator(
    private val client: OkHttpClient,
    private val watcherBaseUrl: String = WATCHER_BASE_URL,
    private val departmentPageUrl: (Schedule) -> String = { it.pageUrl }
) {

    enum class Schedule(internal val watcherEndpoint: String, internal val pageUrl: String) {
        TIMETABLE("api/check-timetable", "https://cs.uowm.gr/archiki-selida/orologio-programma-mathimaton/"),
        EXAMS("api/check-exams", "https://cs.uowm.gr/archiki-selida/programma-spoudwn/programma-exetaseon/")
    }

    data class RemoteDocument(val url: String, val label: String)

    /** @throws SyncException when the current document cannot be determined. */
    fun locate(schedule: Schedule): RemoteDocument {
        val fromWatcher = try {
            queryWatcher(schedule)
        } catch (_: IOException) {
            null
        } catch (_: JSONException) {
            null
        }
        if (fromWatcher != null) return fromWatcher

        val pageUrl = departmentPageUrl(schedule)
        val html = String(fetch(pageUrl, MAX_PAGE_BYTES), Charsets.UTF_8)
        return selectDocument(schedule, html, pageUrl)
            ?: throw SyncException(SyncError.NoDocumentFound)
    }

    /** @throws SyncException on network errors, HTTP errors or oversized files. */
    fun download(url: String): ByteArray = fetch(url, MAX_DOCUMENT_BYTES)

    private fun queryWatcher(schedule: Schedule): RemoteDocument? {
        // dryRun=1 is essential: without it the watcher would post to Discord and mark the
        // document as seen. It only reports which document is current.
        val url = "${watcherBaseUrl.trimEnd('/')}/${schedule.watcherEndpoint}?dryRun=1"
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            return parseWatcherResponse(body)
        }
    }

    private fun fetch(url: String, maxBytes: Long): ByteArray {
        val request = try {
            Request.Builder().url(url).build()
        } catch (e: IllegalArgumentException) {
            throw SyncException(SyncError.NoDocumentFound, e)
        }
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw SyncException(SyncError.Http(response.code))
                val body = response.body ?: throw SyncException(SyncError.NothingRecognized)
                return body.byteStream().use { readBounded(it, maxBytes) }
            }
        } catch (e: SyncException) {
            throw e
        } catch (e: IOException) {
            throw SyncException(SyncError.Network, e)
        }
    }

    internal companion object {
        const val WATCHER_BASE_URL = "https://uowm-exam-watcher.vercel.app"

        private const val MAX_PAGE_BYTES = 4L * 1024 * 1024
        private const val MAX_DOCUMENT_BYTES = 20L * 1024 * 1024

        private val TIMETABLE_DOCUMENT = Regex("\\.(docx?|xlsx?|odt|ods|pdf)(\\?|#|$)", RegexOption.IGNORE_CASE)
        private val EXAM_DOCUMENT = Regex("\\.(docx|doc|pdf)$", RegexOption.IGNORE_CASE)
        private val UPLOAD_STAMP = Regex("/uploads/(\\d{4})/(\\d{2})/")

        /** Reads `current` (or `lastSeen`) from the watcher's JSON answer. */
        fun parseWatcherResponse(json: String): RemoteDocument? {
            val root = JSONObject(json)
            if (!root.optBoolean("ok", false)) return null
            val current = root.optJSONObject("current")
            val url = current?.stringOrNull("url") ?: root.stringOrNull("lastSeen") ?: return null
            if (!url.startsWith("https://") && !url.startsWith("http://")) return null
            return RemoteDocument(url, current?.stringOrNull("label") ?: fileNameOf(url))
        }

        /**
         * Picks the current document from a department page, like the watcher does: newest
         * upload folder for the timetable (ties go to the first link), first link for exams.
         */
        fun selectDocument(schedule: Schedule, html: String, pageUrl: String): RemoteDocument? {
            val page = Jsoup.parse(html, pageUrl)
            val scope = page.selectFirst(".entry-content, article .entry-content, article") ?: page.body()
            val pattern = if (schedule == Schedule.TIMETABLE) TIMETABLE_DOCUMENT else EXAM_DOCUMENT

            data class Candidate(val url: String, val label: String, val stamp: Int, val order: Int)

            val candidates = scope.select("a[href]").mapIndexedNotNull { order, anchor ->
                val url = anchor.absUrl("href").takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
                if (!pattern.containsMatchIn(url)) return@mapIndexedNotNull null
                val stamp = UPLOAD_STAMP.find(url)?.let { it.groupValues[1].toInt() * 100 + it.groupValues[2].toInt() } ?: 0
                Candidate(url, anchor.text().collapseWhitespace().ifEmpty { fileNameOf(url) }, stamp, order)
            }

            val chosen = when (schedule) {
                Schedule.TIMETABLE -> candidates.sortedWith(compareByDescending<Candidate> { it.stamp }.thenBy { it.order })
                Schedule.EXAMS -> candidates
            }.firstOrNull() ?: return null
            return RemoteDocument(chosen.url, chosen.label)
        }

        fun fileNameOf(url: String): String {
            val last = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
            return try {
                URLDecoder.decode(last, "UTF-8")
            } catch (_: Exception) {
                last
            }
        }

        private fun JSONObject.stringOrNull(key: String): String? =
            if (has(key) && !isNull(key)) optString(key).trim().takeIf { it.isNotEmpty() } else null

        fun readBounded(stream: InputStream, maxBytes: Long): ByteArray {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) throw IOException("File exceeds $maxBytes bytes")
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }
}
