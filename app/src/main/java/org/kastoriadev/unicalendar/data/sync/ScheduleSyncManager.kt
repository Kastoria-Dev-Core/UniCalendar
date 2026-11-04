package org.kastoriadev.unicalendar.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.kastoriadev.unicalendar.data.parser.ScheduleParserFactory
import org.kastoriadev.unicalendar.domain.model.SyncResult
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import java.io.IOException
import java.util.concurrent.TimeUnit

class ScheduleSyncManager(
    private val repository: EventRepository,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    suspend fun syncFromUrl(url: String, replaceExisting: Boolean = true): SyncResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "UniCalendar-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext SyncResult.Failure("HTTP request failed with status code ${response.code}")
            }

            val body = response.body?.string() ?: return@withContext SyncResult.Failure("Empty response received from server")
            val parser = ScheduleParserFactory.getParser(body, url)
            val events = parser.parse(body, url)

            if (events.isEmpty()) {
                return@withContext SyncResult.Failure("No schedule events could be parsed from the provided URL")
            }

            if (replaceExisting) {
                repository.clearSyncedEvents()
            }

            repository.insertEvents(events)
            SyncResult.Success(events.size)
        } catch (e: IOException) {
            SyncResult.Failure("Network error: ${e.localizedMessage ?: "Connection error"}")
        } catch (e: Exception) {
            SyncResult.Failure("Parsing error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
