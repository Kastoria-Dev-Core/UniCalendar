package org.kastoriadev.unicalendar.domain.repository

import kotlinx.coroutines.flow.Flow
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventFeed
import java.time.LocalDateTime

interface EventRepository {
    fun getAllEvents(): Flow<List<CalendarEvent>>
    suspend fun getEventById(id: Long): CalendarEvent?
    suspend fun insertEvent(event: CalendarEvent): Long
    suspend fun insertEvents(events: List<CalendarEvent>): List<Long>
    suspend fun updateEvent(event: CalendarEvent)
    suspend fun updateNotes(id: Long, notes: String)
    suspend fun deleteEvent(event: CalendarEvent)
    suspend fun deleteEventById(id: Long)

    /**
     * Atomically replaces the synced events of [feed] that start within [from]..[to] (limited to
     * [semester] when set) with [events], moving personal notes onto the matching new events.
     * Events outside the window, e.g. past semesters, are kept.
     *
     * @return the number of events stored.
     */
    suspend fun replaceSyncedEvents(
        feed: EventFeed,
        semester: String?,
        from: LocalDateTime,
        to: LocalDateTime,
        events: List<CalendarEvent>
    ): Int

    suspend fun clearAllEvents()
}
