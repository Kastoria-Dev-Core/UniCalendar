package org.kastoriadev.unicalendar.data.parser

import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import java.io.InputStream

interface ScheduleParser {
    fun parse(content: String, sourceUrl: String? = null): List<CalendarEvent>

    fun parseStream(stream: InputStream, sourceUrl: String? = null): List<CalendarEvent> {
        val text = stream.bufferedReader().use { it.readText() }
        return parse(text, sourceUrl)
    }
}
