package org.kastoriadev.unicalendar.data.parser

import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventFeed
import java.io.ByteArrayInputStream

/**
 * Detects what a downloaded or picked file is (by content, not by name) and parses it:
 * a .docx timetable or exam schedule, an iCalendar feed, or an HTML timetable page.
 */
class ScheduleDocumentParser(
    private val timetableParser: DocxScheduleParser = DocxScheduleParser(),
    private val examParser: DocxExamScheduleParser = DocxExamScheduleParser(),
    private val icsParser: IcsScheduleParser = IcsScheduleParser(),
    private val htmlParser: HtmlScheduleParser = HtmlScheduleParser()
) {

    sealed interface Result {
        /** The file was understood as [feed]; [events] may be empty if nothing was recognised. */
        data class Parsed(val feed: EventFeed, val events: List<CalendarEvent>) : Result

        /** PDF, legacy .doc, spreadsheets and other formats that cannot be imported. */
        data object UnsupportedFormat : Result
    }

    /**
     * @param expected the kind of document the caller asked for. University sync passes the
     *   feed it requested so an exam schedule is never read as a weekly timetable; local imports
     *   pass null and the content decides.
     */
    fun parse(bytes: ByteArray, sourceName: String?, expected: EventFeed? = null): Result {
        if (DocxReader.isZip(bytes)) {
            // Spreadsheets and ODF files are ZIP archives too, but have no word/document.xml.
            val document = DocxReader.readDocument(ByteArrayInputStream(bytes)) ?: return Result.UnsupportedFormat
            return when (expected) {
                EventFeed.TIMETABLE -> Result.Parsed(EventFeed.TIMETABLE, timetableParser.parseDocument(document, sourceName))
                EventFeed.EXAMS -> Result.Parsed(EventFeed.EXAMS, examParser.parseDocument(document, sourceName))
                else -> {
                    val lessons = timetableParser.parseDocument(document, sourceName)
                    if (lessons.isNotEmpty()) {
                        Result.Parsed(EventFeed.TIMETABLE, lessons)
                    } else {
                        Result.Parsed(EventFeed.EXAMS, examParser.parseDocument(document, sourceName))
                    }
                }
            }
        }

        val text = decodeText(bytes) ?: return Result.UnsupportedFormat
        val head = text.trimStart().take(512)
        return when {
            head.startsWith("BEGIN:VCALENDAR", ignoreCase = true) ->
                Result.Parsed(EventFeed.CALENDAR, icsParser.parse(text, sourceName))
            HTML_MARKERS.any { head.contains(it, ignoreCase = true) } ->
                Result.Parsed(EventFeed.CALENDAR, htmlParser.parse(text, sourceName))
            else -> Result.UnsupportedFormat
        }
    }

    /** UTF-8 text without a BOM, or null for binary content (PDF, .doc, images...). */
    private fun decodeText(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val sample = bytes.take(1024)
        if (sample.any { it == 0.toByte() }) return null
        return String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
    }

    private companion object {
        val HTML_MARKERS = listOf("<!doctype html", "<html", "<table", "<body")
    }
}
