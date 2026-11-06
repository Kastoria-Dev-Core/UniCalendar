package org.kastoriadev.unicalendar.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE startTime >= :start AND startTime < :end ORDER BY startTime ASC")
    fun getEventsBetween(start: LocalDateTime, end: LocalDateTime): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>): List<Long>

    @Update
    suspend fun update(event: EventEntity)

    @Query("UPDATE events SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Synced events of [feed] starting inside [from]..[to]. When [semester] is set, only events
     * of that semester (semesters are stored comma separated) and semester-less events match.
     */
    @Query(
        """
        SELECT * FROM events
        WHERE isSynced = 1 AND feed = :feed AND startTime >= :from AND startTime <= :to
          AND (:semester IS NULL OR semester = '' OR (',' || semester || ',') LIKE ('%,' || :semester || ',%'))
        """
    )
    suspend fun getSyncedEventsInRange(
        feed: String,
        semester: String?,
        from: LocalDateTime,
        to: LocalDateTime
    ): List<EventEntity>

    @Query(
        """
        DELETE FROM events
        WHERE isSynced = 1 AND feed = :feed AND startTime >= :from AND startTime <= :to
          AND (:semester IS NULL OR semester = '' OR (',' || semester || ',') LIKE ('%,' || :semester || ',%'))
        """
    )
    suspend fun deleteSyncedEventsInRange(
        feed: String,
        semester: String?,
        from: LocalDateTime,
        to: LocalDateTime
    )

    @Query("DELETE FROM events")
    suspend fun clearAllEvents()
}
