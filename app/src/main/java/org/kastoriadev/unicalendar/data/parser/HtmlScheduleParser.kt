package org.kastoriadev.unicalendar.data.parser

import org.jsoup.Jsoup
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventFeed
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.regex.Pattern

class HtmlScheduleParser : ScheduleParser {

    private val timeRangePattern = Pattern.compile("(\\d{1,2}[:.]\\d{2})\\s*(?:-|–|to)\\s*(\\d{1,2}[:.]\\d{2})")

    override fun parse(content: String, sourceUrl: String?): List<CalendarEvent> {
        val doc = Jsoup.parse(content)
        val events = mutableListOf<CalendarEvent>()

        // 1. Process HTML tables
        val tables = doc.select("table")
        for (table in tables) {
            val rows = table.select("tr")
            for (row in rows) {
                val cells = row.select("td, th").map { it.text().trim() }
                if (cells.size < 2) continue

                val rowText = cells.joinToString(" ")
                val detectedDay = detectDayOfWeek(rowText)
                val (startTime, endTime) = extractTimeRange(rowText)

                if (detectedDay != null && startTime != null && endTime != null) {
                    val title = extractTitleFromCells(cells)
                    if (title.isNotBlank()) {
                        val location = extractLocation(rowText)
                        val eventType = inferEventType(title, rowText)

                        // Generate dates for the upcoming 12 weeks of the semester
                        val semesterEvents = generateRecurringEvents(
                            title = title,
                            location = location,
                            dayOfWeek = detectedDay,
                            startTime = startTime,
                            endTime = endTime,
                            type = eventType,
                            sourceUrl = sourceUrl,
                            weeks = 12
                        )
                        events.addAll(semesterEvents)
                    }
                }
            }
        }

        // 2. Process list items or structured schedule blocks if no table events were found
        if (events.isEmpty()) {
            val scheduleElements = doc.select(".schedule-item, .timetable-entry, .event-item, li")
            for (element in scheduleElements) {
                val text = element.text().trim()
                val detectedDay = detectDayOfWeek(text)
                val (startTime, endTime) = extractTimeRange(text)

                if (detectedDay != null && startTime != null && endTime != null) {
                    val title = cleanTitle(text)
                    if (title.isNotBlank()) {
                        val location = extractLocation(text)
                        val eventType = inferEventType(title, text)
                        events.addAll(
                            generateRecurringEvents(
                                title = title,
                                location = location,
                                dayOfWeek = detectedDay,
                                startTime = startTime,
                                endTime = endTime,
                                type = eventType,
                                sourceUrl = sourceUrl,
                                weeks = 12
                            )
                        )
                    }
                }
            }
        }

        return events
    }

    private fun detectDayOfWeek(text: String): DayOfWeek? {
        val lower = text.lowercase()
        return when {
            lower.contains("monday") || lower.contains("δευτέρα") || lower.contains("δευτερα") -> DayOfWeek.MONDAY
            lower.contains("tuesday") || lower.contains("τρίτη") || lower.contains("τριτη") -> DayOfWeek.TUESDAY
            lower.contains("wednesday") || lower.contains("τετάρτη") || lower.contains("τεταρτη") -> DayOfWeek.WEDNESDAY
            lower.contains("thursday") || lower.contains("πέμπτη") || lower.contains("πεμπτη") -> DayOfWeek.THURSDAY
            lower.contains("friday") || lower.contains("παρασκευή") || lower.contains("παρασκευη") -> DayOfWeek.FRIDAY
            lower.contains("saturday") || lower.contains("σάββατο") || lower.contains("σαββατο") -> DayOfWeek.SATURDAY
            lower.contains("sunday") || lower.contains("κυριακή") || lower.contains("κυριακη") -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun extractTimeRange(text: String): Pair<LocalTime?, LocalTime?> {
        val matcher = timeRangePattern.matcher(text)
        if (matcher.find()) {
            val startStr = matcher.group(1)?.replace('.', ':') ?: return Pair(null, null)
            val endStr = matcher.group(2)?.replace('.', ':') ?: return Pair(null, null)
            val start = parseTime(startStr)
            val end = parseTime(endStr)
            return Pair(start, end)
        }
        return Pair(null, null)
    }

    private fun parseTime(timeStr: String): LocalTime? {
        return try {
            val parts = timeStr.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            LocalTime.of(hour, minute)
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTitleFromCells(cells: List<String>): String {
        for (cell in cells) {
            if (cell.length > 3 &&
                detectDayOfWeek(cell) == null &&
                !timeRangePattern.matcher(cell).find() &&
                !cell.startsWith("Αίθουσα", ignoreCase = true) &&
                !cell.startsWith("Room", ignoreCase = true)
            ) {
                return cell
            }
        }
        return cells.firstOrNull { it.length > 2 } ?: ""
    }

    private fun cleanTitle(text: String): String {
        return text.replace(timeRangePattern.toRegex(), "").trim()
    }

    private fun extractLocation(text: String): String {
        val roomRegex = "(?i)(?:αίθουσα|αιθουσα|εργαστήριο|εργαστηριο|room|lab|amphi|αμφιθέατρο|αμφιθεατρο)\\s*[A-Za-z0-9._-]+".toRegex()
        val match = roomRegex.find(text)
        return match?.value?.trim() ?: ""
    }

    private fun inferEventType(title: String, text: String): EventType {
        val combined = "$title $text".lowercase()
        return when {
            combined.contains("exam") || combined.contains("εξέταση") || combined.contains("εξεταστική") -> EventType.EXAM
            combined.contains("lab") || combined.contains("εργαστήριο") || combined.contains("εργαστηριακό") -> EventType.LAB
            combined.contains("deadline") || combined.contains("προθεσμία") || combined.contains("παράδοση") -> EventType.DEADLINE
            combined.contains("lecture") || combined.contains("διάλεξη") || combined.contains("μάθημα") || combined.contains("θεωρία") -> EventType.LECTURE
            else -> EventType.GENERAL
        }
    }

    private fun generateRecurringEvents(
        title: String,
        location: String,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        type: EventType,
        sourceUrl: String?,
        weeks: Int
    ): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        var nextDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(dayOfWeek))

        for (i in 0 until weeks) {
            events.add(
                CalendarEvent(
                    title = title,
                    location = location,
                    startTime = LocalDateTime.of(nextDate, startTime),
                    endTime = LocalDateTime.of(nextDate, endTime),
                    type = type,
                    sourceUrl = sourceUrl,
                    isSynced = true,
                    feed = EventFeed.CALENDAR
                )
            )
            nextDate = nextDate.plusWeeks(1)
        }

        return events
    }
}
