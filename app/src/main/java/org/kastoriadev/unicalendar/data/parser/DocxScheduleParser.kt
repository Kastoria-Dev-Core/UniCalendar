package org.kastoriadev.unicalendar.data.parser

import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventType
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringReader
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.regex.Pattern
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class DocxScheduleParser : ScheduleParser {

    private val timeRangePattern = Pattern.compile("(\\d{1,2}[:.]\\d{2}|\\b\\d{1,2}\\b)\\s*(?:-|–|to)\\s*(\\d{1,2}[:.]\\d{2}|\\b\\d{1,2}\\b)")

    override fun parse(content: String, sourceUrl: String?): List<CalendarEvent> {
        val stream = ByteArrayInputStream(content.toByteArray(Charsets.ISO_8859_1))
        return parseStream(stream, sourceUrl)
    }

    override fun parseStream(stream: InputStream, sourceUrl: String?): List<CalendarEvent> {
        val documentXml = extractDocumentXml(stream) ?: return emptyList()
        return parseDocumentXml(documentXml, sourceUrl)
    }

    private fun extractDocumentXml(stream: InputStream): String? {
        val zip = ZipInputStream(stream)
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name.equals("word/document.xml", ignoreCase = true)) {
                return zip.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            entry = zip.nextEntry
        }
        return null
    }

    fun parseDocumentXml(xmlContent: String, sourceUrl: String?): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        dbFactory.isNamespaceAware = true
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(InputSource(StringReader(xmlContent)))
        doc.documentElement.normalize()

        val bodyNodes = doc.getElementsByTagNameNS("*", "body")
        val body = (if (bodyNodes.length > 0) bodyNodes.item(0) as? Element else doc.documentElement) ?: return emptyList()

        var currentSemester = "1"
        var currentDay: DayOfWeek? = null

        val children = body.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val element = node as Element
            val localName = element.localName ?: element.nodeName.substringAfter(":")

            when (localName) {
                "p" -> {
                    val pText = extractAllText(element).trim()
                    val detectedSemester = detectSemester(pText)
                    if (detectedSemester != null) {
                        currentSemester = detectedSemester
                    }
                }
                "tbl" -> {
                    val rows = element.getElementsByTagNameNS("*", "tr")
                    for (r in 0 until rows.length) {
                        val rowElem = rows.item(r) as Element
                        val cells = rowElem.getElementsByTagNameNS("*", "tc")
                        val cellTexts = mutableListOf<String>()
                        for (c in 0 until cells.length) {
                            val cellElem = cells.item(c) as Element
                            cellTexts.add(extractAllText(cellElem).trim())
                        }

                        if (cellTexts.isNotEmpty()) {
                            val rowEvents = processTableRow(
                                cells = cellTexts,
                                activeSemester = currentSemester,
                                previousDay = currentDay,
                                sourceUrl = sourceUrl
                            )
                            if (rowEvents.isNotEmpty()) {
                                events.addAll(rowEvents)
                                currentDay = rowEvents.first().startTime.dayOfWeek
                            } else {
                                val rowText = cellTexts.joinToString(" ")
                                val rowDay = detectDayOfWeek(rowText)
                                if (rowDay != null) {
                                    currentDay = rowDay
                                }
                            }
                        }
                    }
                }
            }
        }

        return events
    }

    private fun extractAllText(element: Element): String {
        val textNodes = element.getElementsByTagNameNS("*", "t")
        val sb = StringBuilder()
        for (i in 0 until textNodes.length) {
            sb.append(textNodes.item(i).textContent).append(" ")
        }
        return sb.toString().trim()
    }

    private fun processTableRow(
        cells: List<String>,
        activeSemester: String,
        previousDay: DayOfWeek?,
        sourceUrl: String?
    ): List<CalendarEvent> {
        val rowText = cells.joinToString(" ").trim()
        if (rowText.isBlank()) return emptyList()

        val detectedDay = detectDayOfWeek(rowText) ?: previousDay ?: return emptyList()
        val (startTime, endTime) = extractTimeRange(rowText) ?: return emptyList()

        val title = extractCourseTitle(cells)
        if (title.isBlank()) return emptyList()

        val location = extractLocation(cells, rowText)
        val eventType = inferEventType(title, rowText)

        return generateRecurringEvents(
            title = title,
            location = location,
            semester = activeSemester,
            dayOfWeek = detectedDay,
            startTime = startTime,
            endTime = endTime,
            type = eventType,
            sourceUrl = sourceUrl,
            weeks = 14
        )
    }

    private fun detectSemester(text: String): String? {
        val upper = text.uppercase()
        return when {
            upper.contains("ΠΔΕ") || upper.contains("ΠΑΙΔΑΓΩΓΙΚΗ") || upper.contains("ΔΙΔΑΚΤΙΚΗ") || upper.contains("PDE") -> "PDE"
            upper.contains("1Ο ΕΞ") || upper.contains("1O ΕΞ") || upper.contains("1Ο EX") || upper.contains("Α' ΕΞ") || upper.contains("ΕΞΑΜΗΝΟ 1") -> "1"
            upper.contains("2Ο ΕΞ") || upper.contains("2O ΕΞ") || upper.contains("Β' ΕΞ") || upper.contains("ΕΞΑΜΗΝΟ 2") -> "2"
            upper.contains("3Ο ΕΞ") || upper.contains("3O ΕΞ") || upper.contains("Γ' ΕΞ") || upper.contains("ΕΞΑΜΗΝΟ 3") -> "3"
            upper.contains("4Ο ΕΞ") || upper.contains("4O ΕΞ") || upper.contains("Δ' ΕΞ") || upper.contains("ΕΞΑΜΗΝΟ 4") -> "4"
            upper.contains("5Ο ΕΞ") || upper.contains("5O ΕΞ") || upper.contains("Ε' ΕΞ") || upper.contains("ΕΞΑΜΗΝΟ 5") -> "5"
            upper.contains("6Ο ΕΞ") || upper.contains("6O ΕΞ") || upper.contains("ΣΤ' ΕΞ") || upper.contains("ΕΞΑΜΗΝΟ 6") -> "6"
            upper.contains("7Ο ΕΞ") || upper.contains("7O ΕΞ") || upper.contains("Ζ' ΕΞ") || upper.contains("ΕΞΑΜΗΝΟ 7") -> "7"
            upper.contains("8Ο ΕΞ") || upper.contains("8O ΕΞ") || upper.contains("Η' ΕΞ") || upper.contains("ΕΞΑΜΗΝΟ 8") -> "8"
            else -> null
        }
    }

    private fun detectDayOfWeek(text: String): DayOfWeek? {
        val lower = text.lowercase()
        return when {
            lower.contains("δευτέρα") || lower.contains("δευτερα") || lower.contains("monday") -> DayOfWeek.MONDAY
            lower.contains("τρίτη") || lower.contains("τριτη") || lower.contains("tuesday") -> DayOfWeek.TUESDAY
            lower.contains("τετάρτη") || lower.contains("τεταρτη") || lower.contains("wednesday") -> DayOfWeek.WEDNESDAY
            lower.contains("πέμπτη") || lower.contains("πεμπτη") || lower.contains("thursday") -> DayOfWeek.THURSDAY
            lower.contains("παρασκευή") || lower.contains("παρασκευη") || lower.contains("friday") -> DayOfWeek.FRIDAY
            lower.contains("σάββατο") || lower.contains("σαββατο") || lower.contains("saturday") -> DayOfWeek.SATURDAY
            lower.contains("κυριακή") || lower.contains("κυριακη") || lower.contains("sunday") -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun extractTimeRange(text: String): Pair<LocalTime, LocalTime>? {
        val matcher = timeRangePattern.matcher(text)
        if (matcher.find()) {
            val startStr = matcher.group(1) ?: return null
            val endStr = matcher.group(2) ?: return null

            val start = parseFlexibleTime(startStr) ?: return null
            val end = parseFlexibleTime(endStr) ?: return null
            return Pair(start, end)
        }
        return null
    }

    private fun parseFlexibleTime(raw: String): LocalTime? {
        val clean = raw.trim().replace('.', ':')
        return try {
            if (clean.contains(":")) {
                val parts = clean.split(":")
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            } else {
                val hour = clean.toInt()
                LocalTime.of(hour, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractCourseTitle(cells: List<String>): String {
        for (cell in cells) {
            val trimmed = cell.trim()
            val lower = trimmed.lowercase()
            if (trimmed.length > 3 &&
                detectDayOfWeek(trimmed) == null &&
                !timeRangePattern.matcher(trimmed).find() &&
                !lower.contains("αίθουσ") &&
                !lower.contains("αιθουσ") &&
                !lower.contains("αμφιθέατρ") &&
                !lower.contains("αμφιθεατρ") &&
                !lower.contains("room") &&
                !lower.contains("ημέρα") &&
                !lower.contains("ημερα") &&
                !lower.contains("ώρα") &&
                !lower.contains("ωρα")
            ) {
                return trimmed
            }
        }
        return ""
    }

    private fun extractLocation(cells: List<String>, text: String): String {
        for (cell in cells) {
            val lower = cell.lowercase()
            if (lower.contains("αίθουσ") || lower.contains("αιθουσ") ||
                lower.contains("εργαστήρι") || lower.contains("εργαστηρι") ||
                lower.contains("αμφιθέατρ") || lower.contains("αμφιθεατρ") ||
                lower.contains("room") || lower.contains("lab")
            ) {
                return cell.trim()
            }
        }
        val roomRegex = "(?iu)(?:αίθουσα|αιθουσα|εργαστήριο|εργαστηριο|αμφιθέατρο|αμφιθεατρο|room|lab)\\s*[A-Za-z0-9._-]+".toRegex()
        val match = roomRegex.find(text)
        return match?.value?.trim() ?: ""
    }

    private fun inferEventType(title: String, text: String): EventType {
        val combined = "$title $text".lowercase()
        return when {
            combined.contains("exam") || combined.contains("εξέταση") || combined.contains("εξεταστική") -> EventType.EXAM
            combined.contains("lab") || combined.contains("(ε)") || combined.contains("εργαστήριο") || combined.contains("εργαστηριακό") -> EventType.LAB
            combined.contains("deadline") || combined.contains("προθεσμία") || combined.contains("παράδοση") -> EventType.DEADLINE
            combined.contains("lecture") || combined.contains("(θ)") || combined.contains("διάλεξη") || combined.contains("θεωρία") -> EventType.LECTURE
            else -> EventType.LECTURE
        }
    }

    private fun generateRecurringEvents(
        title: String,
        location: String,
        semester: String,
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
                    semester = semester,
                    startTime = LocalDateTime.of(nextDate, startTime),
                    endTime = LocalDateTime.of(nextDate, endTime),
                    type = type,
                    sourceUrl = sourceUrl,
                    isSynced = true
                )
            )
            nextDate = nextDate.plusWeeks(1)
        }

        return events
    }
}
