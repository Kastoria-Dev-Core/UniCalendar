package org.kastoriadev.unicalendar.data.parser

import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventFeed
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * RFC 5545 (iCalendar) parser for calendar feeds such as Moodle or Open eClass exports.
 *
 * Times are converted to [zoneId] (UTC "Z" and TZID times), reminders (VALARM) are ignored, and
 * simple recurrences (DAILY / WEEKLY / MONTHLY / YEARLY with INTERVAL, COUNT, UNTIL, BYDAY,
 * EXDATE and overridden occurrences) are expanded.
 */
class IcsScheduleParser(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : ScheduleParser {

    private class Property(val name: String, val params: Map<String, String>, val value: String)

    private class IcsTime(val dateTime: LocalDateTime, val isDate: Boolean)

    private class VEvent {
        var uid: String = ""
        var summary: String = ""
        var description: String = ""
        var location: String = ""
        var status: String = ""
        var start: IcsTime? = null
        var end: IcsTime? = null
        var duration: Duration? = null
        var rrule: String? = null
        var recurrenceId: IcsTime? = null
        val exceptions = mutableListOf<IcsTime>()
    }

    override fun parse(content: String, sourceUrl: String?): List<CalendarEvent> {
        val vevents = mutableListOf<VEvent>()
        var current: VEvent? = null
        var nestedDepth = 0

        for (line in unfoldLines(content)) {
            val property = parseProperty(line) ?: continue
            when (property.name) {
                "BEGIN" -> when {
                    current != null -> nestedDepth++
                    property.value.equals("VEVENT", ignoreCase = true) -> current = VEvent()
                }
                "END" -> when {
                    current == null -> Unit
                    nestedDepth > 0 -> nestedDepth--
                    property.value.equals("VEVENT", ignoreCase = true) -> {
                        vevents += current
                        current = null
                    }
                }
                // Properties of nested components (e.g. VALARM's DESCRIPTION) are not the event's.
                else -> if (current != null && nestedDepth == 0) apply(current, property)
            }
        }

        val overridden = vevents
            .filter { it.recurrenceId != null && it.uid.isNotEmpty() }
            .groupBy({ it.uid }, { it.recurrenceId!!.dateTime })

        val events = mutableListOf<CalendarEvent>()
        for (vevent in vevents) {
            if (vevent.status.equals("CANCELLED", ignoreCase = true)) continue
            val start = vevent.start ?: continue
            if (vevent.summary.isBlank()) continue

            val length = eventLength(vevent, start)
            val skipped = overridden[vevent.uid].orEmpty().toSet().takeIf { vevent.recurrenceId == null }.orEmpty()
            val exceptionTimes = vevent.exceptions.map { it.dateTime }.toSet()
            val exceptionDates = vevent.exceptions.filter { it.isDate }.map { it.dateTime.toLocalDate() }.toSet()
            val type = inferEventType(vevent.summary, vevent.description)

            for (occurrence in occurrences(vevent, start)) {
                if (occurrence in skipped || occurrence in exceptionTimes) continue
                if (occurrence.toLocalDate() in exceptionDates) continue
                events += CalendarEvent(
                    title = vevent.summary,
                    description = vevent.description,
                    location = vevent.location,
                    startTime = occurrence,
                    endTime = occurrence.plus(length),
                    type = type,
                    sourceUrl = sourceUrl,
                    isSynced = true,
                    feed = EventFeed.CALENDAR
                )
            }
        }
        return events
    }

    private fun apply(event: VEvent, property: Property) {
        when (property.name) {
            "UID" -> event.uid = property.value.trim()
            "SUMMARY" -> event.summary = unescapeText(property.value).trim()
            "DESCRIPTION" -> event.description = unescapeText(property.value).trim()
            "LOCATION" -> event.location = unescapeText(property.value).trim()
            "STATUS" -> event.status = property.value.trim()
            "DTSTART" -> event.start = parseTime(property.value, property.params)
            "DTEND" -> event.end = parseTime(property.value, property.params)
            "DURATION" -> event.duration = parseDuration(property.value)
            "RRULE" -> event.rrule = property.value.trim()
            "RECURRENCE-ID" -> event.recurrenceId = parseTime(property.value, property.params)
            "EXDATE" -> property.value.split(',').mapNotNullTo(event.exceptions) { parseTime(it, property.params) }
        }
    }

    /** All-day events end at 23:59 of their last day; timed events default to one hour. */
    private fun eventLength(event: VEvent, start: IcsTime): Duration {
        val end = event.end
        return when {
            start.isDate -> {
                val lastDay = end?.dateTime?.toLocalDate()?.minusDays(1)
                    ?.takeIf { !it.isBefore(start.dateTime.toLocalDate()) }
                    ?: start.dateTime.toLocalDate()
                Duration.between(start.dateTime, lastDay.atTime(23, 59))
            }
            end != null && end.dateTime.isAfter(start.dateTime) -> Duration.between(start.dateTime, end.dateTime)
            event.duration != null && !event.duration!!.isNegative -> event.duration!!
            else -> Duration.ofHours(1)
        }
    }

    private fun occurrences(event: VEvent, start: IcsTime): List<LocalDateTime> {
        val first = start.dateTime
        val rule = event.rrule?.takeIf { event.recurrenceId == null } ?: return listOf(first)
        val parts = rule.split(';').associate { part ->
            part.substringBefore('=').trim().uppercase() to part.substringAfter('=', "").trim()
        }
        val interval = parts["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val count = parts["COUNT"]?.toIntOrNull()?.coerceIn(1, MAX_OCCURRENCES) ?: MAX_OCCURRENCES
        val until = parts["UNTIL"]?.let { parseTime(it, emptyMap()) }?.let { time ->
            if (time.isDate) time.dateTime.toLocalDate().atTime(LocalTime.MAX) else time.dateTime
        }
        val horizon = first.plusYears(MAX_YEARS)
        val limit = until?.takeIf { it.isBefore(horizon) } ?: horizon

        val result = mutableListOf<LocalDateTime>()
        fun accept(candidate: LocalDateTime): Boolean {
            if (candidate.isAfter(limit) || result.size >= count) return false
            if (!candidate.isBefore(first)) result += candidate
            return true
        }

        when (parts["FREQ"]?.uppercase()) {
            "DAILY" -> {
                var next = first
                while (accept(next)) next = next.plusDays(interval.toLong())
            }
            "WEEKLY" -> {
                val days = parts["BYDAY"]?.split(',')
                    ?.mapNotNull { dayCode(it.trim().takeLast(2)) }
                    ?.sorted()
                    ?.takeIf { it.isNotEmpty() }
                    ?: listOf(first.dayOfWeek)
                var weekStart = first.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                weeks@ while (true) {
                    for (day in days) {
                        val candidate = weekStart.with(TemporalAdjusters.nextOrSame(day)).atTime(first.toLocalTime())
                        if (!accept(candidate)) break@weeks
                    }
                    weekStart = weekStart.plusWeeks(interval.toLong())
                }
            }
            "MONTHLY" -> {
                var step = 0L
                while (accept(first.plusMonths(step))) step += interval
            }
            "YEARLY" -> {
                var step = 0L
                while (accept(first.plusYears(step))) step += interval
            }
            else -> result += first
        }
        return result.ifEmpty { listOf(first) }
    }

    private fun dayCode(code: String): DayOfWeek? = when (code.uppercase()) {
        "MO" -> DayOfWeek.MONDAY
        "TU" -> DayOfWeek.TUESDAY
        "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY
        "FR" -> DayOfWeek.FRIDAY
        "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun parseTime(raw: String, params: Map<String, String>): IcsTime? {
        val value = raw.trim()
        return try {
            when {
                params["VALUE"].equals("DATE", ignoreCase = true) || value.length == 8 ->
                    IcsTime(LocalDate.parse(value.take(8), DATE_FORMAT).atStartOfDay(), isDate = true)
                value.endsWith("Z", ignoreCase = true) ->
                    IcsTime(convert(parseLocalDateTime(value.dropLast(1)), ZoneOffset.UTC), isDate = false)
                else -> {
                    val local = parseLocalDateTime(value)
                    val sourceZone = params["TZID"]?.let(::resolveZone)
                    IcsTime(if (sourceZone != null) convert(local, sourceZone) else local, isDate = false)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLocalDateTime(value: String): LocalDateTime =
        if (value.length >= 15) LocalDateTime.parse(value.take(15), DATE_TIME_FORMAT)
        else LocalDateTime.parse(value.take(13), DATE_TIME_NO_SECONDS_FORMAT)

    private fun convert(local: LocalDateTime, from: ZoneId): LocalDateTime =
        local.atZone(from).withZoneSameInstant(zoneId).toLocalDateTime()

    /** TZIDs may be IANA ids, sometimes prefixed ("/Europe/Athens"); unknown ids stay floating. */
    private fun resolveZone(tzid: String): ZoneId? {
        val candidate = tzid.trim().trimStart('/')
        return try {
            ZoneId.of(candidate)
        } catch (_: Exception) {
            null
        }
    }

    /** RFC 5545 durations such as "PT1H30M", "P1D" or "P2W". */
    private fun parseDuration(raw: String): Duration? {
        val value = raw.trim().uppercase()
        WEEKS.matchEntire(value)?.let { return Duration.ofDays(7L * it.groupValues[2].toLong()) }
        return try {
            Duration.parse(value)
        } catch (_: Exception) {
            null
        }
    }

    private fun unfoldLines(content: String): List<String> {
        val result = mutableListOf<String>()
        val buffer = StringBuilder()
        for (line in content.removePrefix("\uFEFF").lines()) {
            if ((line.startsWith(" ") || line.startsWith("\t")) && buffer.isNotEmpty()) {
                buffer.append(line, 1, line.length)
            } else {
                if (buffer.isNotEmpty()) result += buffer.toString()
                buffer.setLength(0)
                buffer.append(line)
            }
        }
        if (buffer.isNotEmpty()) result += buffer.toString()
        return result
    }

    /** "NAME;PARAM=value;PARAM="quoted:value":content"; colons inside quotes do not end the name. */
    private fun parseProperty(line: String): Property? {
        var inQuotes = false
        var colon = -1
        for ((index, char) in line.withIndex()) {
            if (char == '"') inQuotes = !inQuotes
            if (char == ':' && !inQuotes) {
                colon = index
                break
            }
        }
        if (colon <= 0) return null

        val segments = mutableListOf<String>()
        val segment = StringBuilder()
        inQuotes = false
        for (char in line.substring(0, colon)) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ';' && !inQuotes -> {
                    segments += segment.toString()
                    segment.setLength(0)
                }
                else -> segment.append(char)
            }
        }
        segments += segment.toString()

        val params = segments.drop(1).mapNotNull { param ->
            val equals = param.indexOf('=')
            if (equals <= 0) null else param.substring(0, equals).trim().uppercase() to param.substring(equals + 1).trim()
        }.toMap()
        return Property(segments.first().trim().uppercase(), params, line.substring(colon + 1))
    }

    /** Single-pass unescape so "\\n" (an escaped backslash followed by n) is not read as a newline. */
    private fun unescapeText(text: String): String {
        val builder = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (char == '\\' && index + 1 < text.length) {
                when (val next = text[index + 1]) {
                    'n', 'N' -> builder.append('\n')
                    else -> builder.append(next)
                }
                index += 2
            } else {
                builder.append(char)
                index++
            }
        }
        return builder.toString()
    }

    private fun inferEventType(summary: String, description: String): EventType {
        val folded = GreekText.fold("$summary $description")
        return when {
            EXAM_WORDS.containsMatchIn(folded) -> EventType.EXAM
            LAB_WORDS.containsMatchIn(folded) -> EventType.LAB
            DEADLINE_WORDS.containsMatchIn(folded) -> EventType.DEADLINE
            LECTURE_WORDS.containsMatchIn(folded) -> EventType.LECTURE
            else -> EventType.GENERAL
        }
    }

    private companion object {
        // Matched at word starts so "EXAMPLE" or "AVAILABLE" do not count as an exam or a lab.
        val EXAM_WORDS = Regex("(?<![\\p{L}])(EXAM(?!PLE)|MIDTERM|QUIZ|ΕΞΕΤΑΣ|ΠΡΟΟΔΟ)")
        val LAB_WORDS = Regex("(?<![\\p{L}])(LAB|ΕΡΓΑΣΤΗΡ)")
        val DEADLINE_WORDS = Regex("(?<![\\p{L}])(DEADLINE|ΠΡΟΘΕΣΜΙ|ΠΑΡΑΔΟΣ|ΥΠΟΒΟΛ)")
        val LECTURE_WORDS = Regex("(?<![\\p{L}])(LECTURE|ΔΙΑΛΕΞ|ΜΑΘΗΜΑ|ΘΕΩΡΙΑ)")

        const val MAX_OCCURRENCES = 500
        const val MAX_YEARS = 2L
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        val DATE_TIME_NO_SECONDS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm")
        val WEEKS = Regex("([+-]?)P(\\d+)W")
    }
}
