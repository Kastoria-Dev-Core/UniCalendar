package org.kastoriadev.unicalendar.data.local

import androidx.room.TypeConverter
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class Converters {
    @TypeConverter
    fun fromEpochSeconds(value: Long?): LocalDateTime? {
        return value?.let {
            LocalDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneOffset.UTC)
        }
    }

    @TypeConverter
    fun toEpochSeconds(date: LocalDateTime?): Long? {
        return date?.toEpochSecond(ZoneOffset.UTC)
    }

    @TypeConverter
    fun fromEventType(value: String?): EventType? {
        return value?.let {
            try {
                EventType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                EventType.GENERAL
            }
        }
    }

    @TypeConverter
    fun toEventType(type: EventType?): String? {
        return type?.name
    }
}
