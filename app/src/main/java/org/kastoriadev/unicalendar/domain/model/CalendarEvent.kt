package org.kastoriadev.unicalendar.domain.model

import java.time.LocalDateTime

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val location: String = "",
    val notes: String = "",
    val semester: String = "",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val type: EventType = EventType.GENERAL,
    val sourceUrl: String? = null,
    val isSynced: Boolean = false
)
