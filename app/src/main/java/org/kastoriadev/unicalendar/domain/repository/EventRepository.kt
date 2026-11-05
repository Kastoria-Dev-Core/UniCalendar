package org.kastoriadev.unicalendar.domain.repository

import kotlinx.coroutines.flow.Flow
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import java.time.LocalDate

interface EventRepository {
    fun getAllEvents(): Flow<List<CalendarEvent>>
    fun getEventsForDate(date: LocalDate): Flow<List<CalendarEvent>>
    fun getEventsForMonth(year: Int, month: Int): Flow<List<CalendarEvent>>
    suspend fun getEventById(id: Long): CalendarEvent?
    suspend fun insertEvent(event: CalendarEvent): Long
    suspend fun insertEvents(events: List<CalendarEvent>): List<Long>
    suspend fun updateEvent(event: CalendarEvent)
    suspend fun updateNotes(id: Long, notes: String)
    suspend fun deleteEvent(event: CalendarEvent)
    suspend fun deleteEventById(id: Long)
    suspend fun clearSyncedEvents()
}
