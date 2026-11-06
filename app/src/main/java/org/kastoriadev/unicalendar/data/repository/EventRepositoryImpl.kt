package org.kastoriadev.unicalendar.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.kastoriadev.unicalendar.data.local.AppDatabase
import org.kastoriadev.unicalendar.data.local.EventEntity
import org.kastoriadev.unicalendar.data.sync.NotesCarryOver
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventFeed
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import java.time.LocalDateTime

class EventRepositoryImpl(
    private val database: AppDatabase
) : EventRepository {

    private val dao = database.eventDao()

    override fun getAllEvents(): Flow<List<CalendarEvent>> {
        return dao.getAllEvents().map { entities ->
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

    override suspend fun replaceSyncedEvents(
        feed: EventFeed,
        semester: String?,
        from: LocalDateTime,
        to: LocalDateTime,
        events: List<CalendarEvent>
    ): Int = database.withTransaction {
        val previous = dao.getSyncedEventsInRange(feed.key, semester, from, to).map { it.toDomain() }
        val incoming = events.map { it.copy(id = 0, feed = feed, isSynced = true) }
        val merged = NotesCarryOver.apply(previous, incoming)
        dao.deleteSyncedEventsInRange(feed.key, semester, from, to)
        dao.insertAll(merged.map { it.toEntity() })
        merged.size
    }

    override suspend fun clearAllEvents() {
        dao.clearAllEvents()
    }

    private fun EventEntity.toDomain(): CalendarEvent = CalendarEvent(
        id = id,
        title = title,
        description = description,
        location = location,
        notes = notes,
        semester = semester,
        startTime = startTime,
        endTime = endTime,
        type = type,
        sourceUrl = sourceUrl,
        isSynced = isSynced,
        feed = EventFeed.fromKey(feed)
    )

    private fun CalendarEvent.toEntity(): EventEntity = EventEntity(
        id = id,
        title = title,
        description = description,
        location = location,
        notes = notes,
        semester = semester,
        startTime = startTime,
        endTime = endTime,
        type = type,
        sourceUrl = sourceUrl,
        isSynced = isSynced,
        feed = feed.key
    )
}
