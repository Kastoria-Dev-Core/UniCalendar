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

    @Query("SELECT * FROM events WHERE (:semester IS NULL OR semester = :semester) ORDER BY startTime ASC")
    fun getEventsBySemester(semester: String?): Flow<List<EventEntity>>

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

    @Query("DELETE FROM events WHERE isSynced = 1")
    suspend fun clearSyncedEvents()

    @Query("DELETE FROM events WHERE isSynced = 1 AND (:semester IS NULL OR semester = :semester)")
    suspend fun clearSyncedEventsBySemester(semester: String?)

    @Query("DELETE FROM events")
    suspend fun clearAllEvents()
}
