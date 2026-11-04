package org.kastoriadev.unicalendar.data.parser

object ScheduleParserFactory {
    private val icsParser = IcsScheduleParser()
    private val htmlParser = HtmlScheduleParser()

    fun getParser(content: String, url: String? = null): ScheduleParser {
        val trimmed = content.trim()
        val isIcs = trimmed.startsWith("BEGIN:VCALENDAR", ignoreCase = true) ||
                url?.endsWith(".ics", ignoreCase = true) == true ||
                url?.endsWith(".ical", ignoreCase = true) == true

        return if (isIcs) icsParser else htmlParser
    }
}
