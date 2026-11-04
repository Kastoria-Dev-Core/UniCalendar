package org.kastoriadev.unicalendar.data.parser

import org.kastoriadev.unicalendar.domain.model.CalendarEvent

interface ScheduleParser {
    /**
     * Parses the raw content (HTML, ICS, or text) into domain CalendarEvents.
     */
    fun parse(content: String, sourceUrl: String? = null): List<CalendarEvent>
}
