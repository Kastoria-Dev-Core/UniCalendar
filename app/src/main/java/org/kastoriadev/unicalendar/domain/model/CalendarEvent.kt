package org.kastoriadev.unicalendar.domain.model

import java.time.LocalDateTime

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val location: String = "",
    val notes: String = "",
    /**
     * Semester keys this event belongs to, comma separated ("3", "5,6", "PDE").
     * Blank means the event is shown regardless of the selected semester.
     */
    val semester: String = "",
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val type: EventType = EventType.GENERAL,
    val sourceUrl: String? = null,
    val isSynced: Boolean = false,
    val feed: EventFeed = EventFeed.MANUAL
) {
    val semesters: Set<String>
        get() = parseSemesters(semester)

    companion object {
        const val SEMESTER_PDE = "PDE"

        fun parseSemesters(value: String): Set<String> =
            value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

        fun formatSemesters(values: Collection<String>): String =
            values.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")
    }
}
