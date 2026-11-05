package org.kastoriadev.unicalendar.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.kastoriadev.unicalendar.data.parser.DocxScheduleParser
import org.kastoriadev.unicalendar.data.parser.ScheduleParserFactory
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.SyncResult
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class ScheduleSyncManager(
    private val repository: EventRepository,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    suspend fun syncFromUrl(
        url: String,
        targetSemester: String? = null,
        replaceExisting: Boolean = true
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = url.trim()
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "UniCalendar-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext SyncResult.Failure("HTTP request failed with status ${response.code}")
            }

            val contentType = response.header("Content-Type", "")?.lowercase() ?: ""
            val isDocx = cleanUrl.endsWith(".docx", ignoreCase = true) ||
                    contentType.contains("wordprocessingml") ||
                    contentType.contains("application/zip")

            val events: List<CalendarEvent> = if (isDocx) {
                val bytes = response.body?.bytes() ?: return@withContext SyncResult.Failure("Empty file received")
                DocxScheduleParser().parseStream(ByteArrayInputStream(bytes), cleanUrl)
            } else {
                val bodyString = response.body?.string() ?: return@withContext SyncResult.Failure("Empty response received")

                // Check if this HTML page contains an announcement link to a .docx schedule file
                val docxLink = extractDocxLinkFromHtml(bodyString, cleanUrl)
                if (docxLink != null) {
                    val docxRequest = Request.Builder().url(docxLink).header("User-Agent", "UniCalendar-Android/1.0").build()
                    val docxResponse = client.newCall(docxRequest).execute()
                    if (docxResponse.isSuccessful && docxResponse.body != null) {
                        val docxBytes = docxResponse.body!!.bytes()
                        DocxScheduleParser().parseStream(ByteArrayInputStream(docxBytes), docxLink)
                    } else {
                        val parser = ScheduleParserFactory.getParser(bodyString, cleanUrl)
                        parser.parse(bodyString, cleanUrl)
                    }
                } else {
                    val parser = ScheduleParserFactory.getParser(bodyString, cleanUrl)
                    parser.parse(bodyString, cleanUrl)
                }
            }

            if (events.isEmpty()) {
                return@withContext SyncResult.Failure("No schedule courses could be parsed from the provided source")
            }

            val filteredEvents = if (!targetSemester.isNullOrBlank() && targetSemester != "ALL") {
                events.filter { it.semester.equals(targetSemester, ignoreCase = true) || it.semester.isBlank() }
            } else {
                events
            }

            if (filteredEvents.isEmpty()) {
                return@withContext SyncResult.Failure("Found ${events.size} courses, but none matched semester: $targetSemester")
            }

            if (replaceExisting) {
                if (!targetSemester.isNullOrBlank() && targetSemester != "ALL") {
                    repository.clearSyncedEventsBySemester(targetSemester)
                } else {
                    repository.clearSyncedEvents()
                }
            }

            repository.insertEvents(filteredEvents)
            SyncResult.Success(filteredEvents.size)
        } catch (e: IOException) {
            SyncResult.Failure("Network error: ${e.localizedMessage ?: "Connection error"}")
        } catch (e: Exception) {
            SyncResult.Failure("Parsing error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun syncFromStream(
        stream: InputStream,
        fileName: String? = null,
        targetSemester: String? = null,
        replaceExisting: Boolean = true
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val parser = if (fileName?.endsWith(".docx", ignoreCase = true) == true) {
                DocxScheduleParser()
            } else {
                DocxScheduleParser() // Default binary stream parser
            }

            val events = parser.parseStream(stream, fileName)
            if (events.isEmpty()) {
                return@withContext SyncResult.Failure("No timetable entries could be extracted from this document")
            }

            val filteredEvents = if (!targetSemester.isNullOrBlank() && targetSemester != "ALL") {
                events.filter { it.semester.equals(targetSemester, ignoreCase = true) || it.semester.isBlank() }
            } else {
                events
            }

            if (replaceExisting) {
                if (!targetSemester.isNullOrBlank() && targetSemester != "ALL") {
                    repository.clearSyncedEventsBySemester(targetSemester)
                } else {
                    repository.clearSyncedEvents()
                }
            }

            repository.insertEvents(filteredEvents)
            SyncResult.Success(filteredEvents.size)
        } catch (e: Exception) {
            SyncResult.Failure("Failed to import file: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun extractDocxLinkFromHtml(html: String, baseUrl: String): String? {
        try {
            val doc = Jsoup.parse(html, baseUrl)
            val docxAnchor = doc.select("a[href$=.docx], a[href*=.docx]").firstOrNull()
            return docxAnchor?.absUrl("href")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            return null
        }
    }
}
