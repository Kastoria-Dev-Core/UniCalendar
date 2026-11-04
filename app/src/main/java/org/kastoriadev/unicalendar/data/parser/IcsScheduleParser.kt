package org.kastoriadev.unicalendar.data.parser

import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class IcsScheduleParser : ScheduleParser {

    private val basicDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val utcDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
    private val basicDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    override fun parse(content: String, sourceUrl: String?): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val lines = unfoldLines(content)

        var inEvent = false
        var summary = ""
        var description = ""
        var location = ""
        var startDateTime: LocalDateTime? = null
        var endDateTime: LocalDateTime? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.equals("BEGIN:VEVENT", ignoreCase = true)) {
                inEvent = true
                summary = ""
                description = ""
                location = ""
                startDateTime = null
                endDateTime = null
                continue
            }

            if (trimmed.equals("END:VEVENT", ignoreCase = true)) {
                if (inEvent && summary.isNotBlank() && startDateTime != null) {
                    val resolvedEnd = endDateTime ?: startDateTime.plusHours(1)
                    val eventType = inferEventType(summary, description)

                    events.add(
                        CalendarEvent(
                            title = summary,
                            description = description,
                            location = location,
                            startTime = startDateTime,
                            endTime = resolvedEnd,
                            type = eventType,
                            sourceUrl = sourceUrl,
                            isSynced = true
                        )
                    )
                }
                inEvent = false
                continue
            }

            if (!inEvent) continue

            val colonIndex = trimmed.indexOf(':')
            if (colonIndex == -1) continue

            val keyPart = trimmed.substring(0, colonIndex).uppercase()
            val valuePart = unescapeIcsText(trimmed.substring(colonIndex + 1))

            when {
                keyPart.startsWith("SUMMARY") -> summary = valuePart
                keyPart.startsWith("DESCRIPTION") -> description = valuePart
                keyPart.startsWith("LOCATION") -> location = valuePart
                keyPart.startsWith("DTSTART") -> startDateTime = parseDateTime(valuePart)
                keyPart.startsWith("DTEND") -> endDateTime = parseDateTime(valuePart)
            }
        }

        return events
    }

    private fun parseDateTime(raw: String): LocalDateTime? {
        val clean = raw.trim()
        return try {
            when {
                clean.endsWith("Z") -> LocalDateTime.parse(clean, utcDateTimeFormatter)
                clean.contains("T") -> LocalDateTime.parse(clean, basicDateTimeFormatter)
                clean.length == 8 -> LocalDate.parse(clean, basicDateFormatter).atStartOfDay()
                else -> null
            }
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private fun unfoldLines(content: String): List<String> {
        val rawLines = content.lines()
        val result = mutableListOf<String>()
        val buffer = StringBuilder()

        for (line in rawLines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                buffer.append(line.substring(1))
            } else {
                if (buffer.isNotEmpty()) {
                    result.add(buffer.toString())
                    buffer.setLength(0)
                }
                buffer.append(line)
            }
        }
        if (buffer.isNotEmpty()) {
            result.add(buffer.toString())
        }
        return result
    }

    private fun unescapeIcsText(text: String): String {
        return text
            .replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }

    private fun inferEventType(summary: String, description: String): EventType {
        val combined = "$summary $description".lowercase()
        return when {
            combined.contains("exam") || combined.contains("εξέταση") || combined.contains("εξεταστική") -> EventType.EXAM
            combined.contains("lab") || combined.contains("εργαστήριο") || combined.contains("εργαστηριακό") -> EventType.LAB
            combined.contains("deadline") || combined.contains("προθεσμία") || combined.contains("παράδοση") -> EventType.DEADLINE
            combined.contains("lecture") || combined.contains("διάλεξη") || combined.contains("μάθημα") || combined.contains("θεωρία") -> EventType.LECTURE
            else -> EventType.GENERAL
        }
    }
}
