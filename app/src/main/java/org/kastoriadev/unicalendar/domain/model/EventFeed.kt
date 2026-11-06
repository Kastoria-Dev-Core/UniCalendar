package org.kastoriadev.unicalendar.domain.model

/**
 * Where a synced event came from. Each feed is replaced independently when it is re-synced,
 * so importing exams never removes lessons and vice versa.
 */
enum class EventFeed(val key: String) {
    /** Created by the user inside the app. */
    MANUAL(""),

    /** Weekly lessons from the department timetable. */
    TIMETABLE("timetable"),

    /** Exam-period schedule. */
    EXAMS("exams"),

    /** Generic calendar imports (.ics / web pages). */
    CALENDAR("calendar");

    companion object {
        fun fromKey(key: String?): EventFeed = entries.firstOrNull { it.key == key } ?: MANUAL
    }
}
