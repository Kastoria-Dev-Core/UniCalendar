package org.kastoriadev.unicalendar.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.LocalDateTime

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["isSynced"])
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val location: String,
    val notes: String = "",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val type: EventType,
    val sourceUrl: String?,
    val isSynced: Boolean
)
