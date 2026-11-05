package org.kastoriadev.unicalendar.data.parser

object ScheduleParserFactory {
    private val icsParser = IcsScheduleParser()
    private val htmlParser = HtmlScheduleParser()
    private val docxParser = DocxScheduleParser()

    fun getParser(content: String, url: String? = null): ScheduleParser {
        val trimmed = content.trim()
        val isDocx = url?.endsWith(".docx", ignoreCase = true) == true ||
                content.startsWith("PK\u0003\u0004")
        val isIcs = trimmed.startsWith("BEGIN:VCALENDAR", ignoreCase = true) ||
                url?.endsWith(".ics", ignoreCase = true) == true ||
                url?.endsWith(".ical", ignoreCase = true) == true

        return when {
            isDocx -> docxParser
            isIcs -> icsParser
            else -> htmlParser
        }
    }

    fun getDocxParser(): DocxScheduleParser = docxParser
    fun getIcsParser(): IcsScheduleParser = icsParser
    fun getHtmlParser(): HtmlScheduleParser = htmlParser
}
