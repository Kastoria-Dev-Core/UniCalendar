package org.kastoriadev.unicalendar.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.kastoriadev.unicalendar.data.local.EventDao
import org.kastoriadev.unicalendar.data.local.EventEntity
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class EventRepositoryImpl(
    private val dao: EventDao
) : EventRepository {

    override fun getAllEvents(): Flow<List<CalendarEvent>> {
        return dao.getAllEvents().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEventsForDate(date: LocalDate): Flow<List<CalendarEvent>> {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)
        return dao.getEventsBetween(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEventsForMonth(year: Int, month: Int): Flow<List<CalendarEvent>> {
        val yearMonth = YearMonth.of(year, month)
        val startOfMonth = yearMonth.atDay(1).atStartOfDay()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(LocalTime.MAX)
        return dao.getEventsBetween(startOfMonth, endOfMonth).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEventById(id: Long): CalendarEvent? {
        return dao.getEventById(id)?.toDomain()
    }

    override suspend fun insertEvent(event: CalendarEvent): Long {
        return dao.insert(event.toEntity())
    }

    override suspend fun insertEvents(events: List<CalendarEvent>): List<Long> {
        return dao.insertAll(events.map { it.toEntity() })
    }

    override suspend fun updateEvent(event: CalendarEvent) {
        dao.update(event.toEntity())
    }

    override suspend fun updateNotes(id: Long, notes: String) {
        dao.updateNotes(id, notes)
    }

    override suspend fun deleteEvent(event: CalendarEvent) {
        dao.delete(event.toEntity())
    }

    override suspend fun deleteEventById(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearSyncedEvents() {
        dao.clearSyncedEvents()
    }

    private fun EventEntity.toDomain(): CalendarEvent = CalendarEvent(
        id = id,
        title = title,
        description = description,
        location = location,
        notes = notes,
        startTime = startTime,
        endTime = endTime,
        type = type,
        sourceUrl = sourceUrl,
        isSynced = isSynced
    )

    private fun CalendarEvent.toEntity(): EventEntity = EventEntity(
        id = id,
        title = title,
        description = description,
        location = location,
        notes = notes,
        startTime = startTime,
        endTime = endTime,
        type = type,
        sourceUrl = sourceUrl,
        isSynced = isSynced
    )
}
