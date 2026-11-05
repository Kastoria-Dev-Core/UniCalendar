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
    private val instructorPattern = Pattern.compile("^[A-ZΑ-Ω]\\.\\s*[A-ZΑ-Ωα-ωά-ώ]+")
    private val locationKeywordPattern = Pattern.compile("(?iu)(αμφιθέατρ|αμφιθεατρ|εργ\\.|εργαστήρ|εργαστηρ|αίθουσ|αιθουσ|^[βb]\\d+|«φίλιππος|πετσάλνικος)")

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

        val semesterStartDate = detectSemesterStartDate(xmlContent)
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
                    val rows = getDirectChildren(element, "tr")
                    if (rows.isEmpty()) continue

                    // Check if this is a grid timetable (Row 0 has day names as column headers)
                    val headerRow = rows[0]
                    val dayColumnMap = detectGridDayColumns(headerRow)

                    if (dayColumnMap.size >= 2) {
                        // Grid layout (e.g. official Greek university schedule)
                        val gridEvents = processGridTable(
                            rows = rows,
                            dayColumnMap = dayColumnMap,
                            activeSemester = currentSemester,
                            sourceUrl = sourceUrl,
                            baseDate = semesterStartDate
                        )
                        events.addAll(gridEvents)
                    } else {
                        // Row-based layout (each row defines a course with day & time)
                        for (r in rows) {
                            val cells = getDirectChildren(r, "tc")
                            val cellTexts = cells.map { extractAllText(it).trim() }

                            if (cellTexts.isNotEmpty()) {
                                val rowEvents = processTableRow(
                                    cells = cellTexts,
                                    activeSemester = currentSemester,
                                    previousDay = currentDay,
                                    sourceUrl = sourceUrl,
                                    baseDate = semesterStartDate
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
        }

        return events
    }

    private fun detectSemesterStartDate(xmlContent: String): LocalDate {
        val yearPattern = Pattern.compile("\\b(20\\d\\d)\\s*[-/–]\\s*(20\\d\\d)\\b")
        val yearMatcher = yearPattern.matcher(xmlContent)
        var startYear: Int? = null
        var endYear: Int? = null
        if (yearMatcher.find()) {
            startYear = yearMatcher.group(1)?.toIntOrNull()
            endYear = yearMatcher.group(2)?.toIntOrNull()
        }

        val headerText = normalizeGreek(xmlContent.take(6000))
        val isWinter = headerText.contains("ΧΕΙΜΕΡ") || headerText.contains("ΧΕ")
        val isSpring = headerText.contains("ΕΑΡΙΝ") || headerText.contains("EE")

        return when {
            startYear != null && isWinter -> {
                // Official Greek university winter semester starts on the first Monday of October
                LocalDate.of(startYear, 10, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY))
            }
            endYear != null && isSpring -> {
                // Official Greek university spring semester starts mid-February
                LocalDate.of(endYear, 2, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)).plusWeeks(1)
            }
            else -> {
                // Default fallback: anchor around current semester
                LocalDate.now().minusWeeks(6).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            }
        }
    }

    private fun detectGridDayColumns(headerRow: Element): Map<Int, DayOfWeek> {
        val map = mutableMapOf<Int, DayOfWeek>()
        val cells = getDirectChildren(headerRow, "tc")
        for (c in cells.indices) {
            val text = extractAllText(cells[c])
            val day = detectDayOfWeek(text)
            if (day != null) {
                map[c] = day
            }
        }
        return map
    }

    private data class PendingGridCourse(
        val startRow: Int,
        var endRow: Int,
        val lines: List<String>,
        val dayOfWeek: DayOfWeek
    )

    private fun processGridTable(
        rows: List<Element>,
        dayColumnMap: Map<Int, DayOfWeek>,
        activeSemester: String,
        sourceUrl: String?,
        baseDate: LocalDate
    ): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        if (rows.size <= 1) return events

        // Parse time slots for each row (Row 1..N)
        val rowTimeSlots = mutableMapOf<Int, Pair<LocalTime, LocalTime>>()
        for (r in 1 until rows.size) {
            val cells = getDirectChildren(rows[r], "tc")
            if (cells.isNotEmpty()) {
                val timeCellText = extractAllText(cells[0])
                val range = extractTimeRange(timeCellText)
                if (range != null) {
                    rowTimeSlots[r] = range
                }
            }
        }

        val pendingCourses = mutableMapOf<Int, PendingGridCourse>()

        fun finishCourse(colIdx: Int) {
            val pending = pendingCourses.remove(colIdx) ?: return
            val startTime = rowTimeSlots[pending.startRow]?.first ?: return
            val endTime = rowTimeSlots[pending.endRow]?.second ?: return
            val (title, prof, loc) = parseCellContent(pending.lines)
            if (title.isBlank()) return

            val eventType = inferEventType(title, pending.lines.joinToString(" "))
            val description = if (prof.isNotBlank()) "Διδάσκων: $prof" else ""

            events.addAll(
                generateRecurringEvents(
                    title = title,
                    description = description,
                    location = loc,
                    semester = activeSemester,
                    dayOfWeek = pending.dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    type = eventType,
                    sourceUrl = sourceUrl,
                    baseDate = baseDate,
                    weeks = 16
                )
            )
        }

        for (r in 1 until rows.size) {
            if (!rowTimeSlots.containsKey(r)) continue
            val cells = getDirectChildren(rows[r], "tc")

            for ((colIdx, dayOfWeek) in dayColumnMap) {
                if (colIdx >= cells.size) continue
                val cell = cells[colIdx]

                val vMergeInfo = getVMergeStatus(cell)
                when (vMergeInfo) {
                    VMergeStatus.RESTART -> {
                        finishCourse(colIdx)
                        val lines = extractCellLines(cell)
                        if (lines.isNotEmpty()) {
                            pendingCourses[colIdx] = PendingGridCourse(
                                startRow = r,
                                endRow = r,
                                lines = lines,
                                dayOfWeek = dayOfWeek
                            )
                        }
                    }
                    VMergeStatus.CONTINUE -> {
                        val pending = pendingCourses[colIdx]
                        if (pending != null) {
                            pending.endRow = r
                        }
                    }
                    VMergeStatus.NONE -> {
                        finishCourse(colIdx)
                        val lines = extractCellLines(cell)
                        if (lines.isNotEmpty()) {
                            pendingCourses[colIdx] = PendingGridCourse(
                                startRow = r,
                                endRow = r,
                                lines = lines,
                                dayOfWeek = dayOfWeek
                            )
                            finishCourse(colIdx)
                        }
                    }
                }
            }
        }

        // Finish any remaining open courses at the end of the table
        for (colIdx in dayColumnMap.keys.toList()) {
            finishCourse(colIdx)
        }

        return events
    }

    private enum class VMergeStatus {
        NONE,
        RESTART,
        CONTINUE
    }

    private fun getVMergeStatus(cell: Element): VMergeStatus {
        val tcPrList = getDirectChildren(cell, "tcPr")
        if (tcPrList.isEmpty()) return VMergeStatus.NONE
        val tcPr = tcPrList[0]
        val vMergeList = getDirectChildren(tcPr, "vMerge")
        if (vMergeList.isEmpty()) return VMergeStatus.NONE

        val vMerge = vMergeList[0]
        val valAttr = (vMerge.getAttributeNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "val")
            .takeIf { it.isNotBlank() }
            ?: vMerge.getAttribute("w:val").takeIf { it.isNotBlank() }
            ?: vMerge.getAttribute("val")).lowercase()

        return if (valAttr == "restart") {
            VMergeStatus.RESTART
        } else {
            VMergeStatus.CONTINUE
        }
    }

    private fun extractCellLines(cell: Element): List<String> {
        val lines = mutableListOf<String>()
        val paragraphs = getDirectChildren(cell, "p")
        for (p in paragraphs) {
            val lineBuilder = StringBuilder()
            val children = p.childNodes
            for (i in 0 until children.length) {
                val node = children.item(i)
                if (node.nodeType != Node.ELEMENT_NODE) continue
                val elem = node as Element
                val tag = elem.localName ?: elem.nodeName.substringAfter(":")

                if (tag.equals("r", ignoreCase = true)) {
                    val rChildren = elem.childNodes
                    for (j in 0 until rChildren.length) {
                        val rChild = rChildren.item(j)
                        if (rChild.nodeType != Node.ELEMENT_NODE) continue
                        val rElem = rChild as Element
                        val rTag = rElem.localName ?: rElem.nodeName.substringAfter(":")
                        if (rTag.equals("t", ignoreCase = true)) {
                            lineBuilder.append(rElem.textContent)
                        } else if (rTag.equals("br", ignoreCase = true)) {
                            val current = lineBuilder.toString().trim()
                            if (current.isNotEmpty()) lines.add(current)
                            lineBuilder.setLength(0)
                        }
                    }
                } else if (tag.equals("br", ignoreCase = true)) {
                    val current = lineBuilder.toString().trim()
                    if (current.isNotEmpty()) lines.add(current)
                    lineBuilder.setLength(0)
                }
            }
            val remaining = lineBuilder.toString().trim()
            if (remaining.isNotEmpty()) lines.add(remaining)
        }
        return lines
    }

    fun parseCellContent(lines: List<String>): Triple<String, String, String> {
        if (lines.isEmpty()) return Triple("", "", "")

        var profIdx = -1
        for (idx in lines.indices) {
            val line = lines[idx].trim()
            if (instructorPattern.matcher(line).find() && !locationKeywordPattern.matcher(line).find()) {
                profIdx = idx
                break
            }
        }

        val titleLines: List<String>
        val profLines: List<String>
        val locLines: List<String>

        if (profIdx != -1) {
            titleLines = lines.subList(0, profIdx)
            profLines = listOf(lines[profIdx])
            locLines = lines.subList(profIdx + 1, lines.size)
        } else {
            var locIdx = -1
            for (idx in lines.indices) {
                if (locationKeywordPattern.matcher(lines[idx]).find()) {
                    locIdx = idx
                    break
                }
            }
            if (locIdx != -1) {
                titleLines = lines.subList(0, locIdx)
                profLines = emptyList()
                locLines = lines.subList(locIdx, lines.size)
            } else {
                titleLines = lines
                profLines = emptyList()
                locLines = emptyList()
            }
        }

        val title = titleLines.joinToString(" ")
            .replace("\\s+".toRegex(), " ")
            .replace(" - ", "-")
            .trim()

        val prof = profLines.joinToString(" ").replace("\\s+".toRegex(), " ").trim()
        val loc = locLines.joinToString(" ")
            .replace("\\s+".toRegex(), " ")
            .trimEnd('»', ' ')
            .trim()

        return Triple(title, prof, loc)
    }

    private fun getDirectChildren(parent: Element, localName: String): List<Element> {
        val result = mutableListOf<Element>()
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val elem = node as Element
                val name = elem.localName ?: elem.nodeName.substringAfter(":")
                if (name.equals(localName, ignoreCase = true)) {
                    result.add(elem)
                }
            }
        }
        return result
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
        sourceUrl: String?,
        baseDate: LocalDate
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
            description = "",
            location = location,
            semester = activeSemester,
            dayOfWeek = detectedDay,
            startTime = startTime,
            endTime = endTime,
            type = eventType,
            sourceUrl = sourceUrl,
            baseDate = baseDate,
            weeks = 16
        )
    }

    fun detectSemester(text: String): String? {
        val clean = normalizeGreek(text.replace("\\s+".toRegex(), " ").trim())
        if (clean.contains("ΠΑΙΔΑΓΩΓΙΚ") || clean.contains("ΔΙΔΑΚΤΙΚ") || clean.contains("ΠΔΕ") || clean.contains("PDE")) {
            return "PDE"
        }
        if (Pattern.compile("\\bΑ\\s*['΄]?\\s*ΕΞΑΜΗΝ").matcher(clean).find() || clean.contains("1Ο ΕΞ") || clean.contains("1O ΕΞ") || clean.contains("ΕΞΑΜΗΝΟ 1")) {
            return "1"
        }
        if (Pattern.compile("\\bΒ\\s*['΄]?\\s*ΕΞΑΜΗΝ").matcher(clean).find() || clean.contains("2Ο ΕΞ") || clean.contains("2O ΕΞ") || clean.contains("ΕΞΑΜΗΝΟ 2")) {
            return "2"
        }
        if (Pattern.compile("\\bΓ\\s*['΄]?\\s*ΕΞΑΜΗΝ").matcher(clean).find() || clean.contains("3Ο ΕΞ") || clean.contains("3O ΕΞ") || clean.contains("ΕΞΑΜΗΝΟ 3")) {
            return "3"
        }
        if (Pattern.compile("\\bΔ\\s*['΄]?\\s*ΕΞΑΜΗΝ").matcher(clean).find() || clean.contains("4Ο ΕΞ") || clean.contains("4O ΕΞ") || clean.contains("ΕΞΑΜΗΝΟ 4")) {
            return "4"
        }
        if (Pattern.compile("\\bΕ\\s*['΄]?\\s*ΕΞΑΜΗΝ").matcher(clean).find() || clean.contains("5Ο ΕΞ") || clean.contains("5O ΕΞ") || clean.contains("ΕΞΑΜΗΝΟ 5")) {
            return "5"
        }
        if (Pattern.compile("\\bΣΤ\\s*['΄]?\\s*ΕΞΑΜΗΝ").matcher(clean).find() || clean.contains("6Ο ΕΞ") || clean.contains("6O ΕΞ") || clean.contains("ΕΞΑΜΗΝΟ 6")) {
            return "6"
        }
        if (Pattern.compile("\\bΖ\\s*['΄]?\\s*ΕΞΑΜΗΝ").matcher(clean).find() || clean.contains("7Ο ΕΞ") || clean.contains("7O ΕΞ") || clean.contains("ΕΞΑΜΗΝΟ 7")) {
            return "7"
        }
        if (Pattern.compile("\\bΗ\\s*['΄]?\\s*ΕΞΑΜΗΝ").matcher(clean).find() || clean.contains("8Ο ΕΞ") || clean.contains("8O ΕΞ") || clean.contains("ΕΞΑΜΗΝΟ 8")) {
            return "8"
        }
        return null
    }

    private fun normalizeGreek(s: String): String {
        return s.uppercase()
            .replace('Ά', 'Α')
            .replace('Έ', 'Ε')
            .replace('Ή', 'Η')
            .replace('Ί', 'Ι')
            .replace('Ό', 'Ο')
            .replace('Ύ', 'Υ')
            .replace('Ώ', 'Ω')
    }

    fun detectDayOfWeek(text: String): DayOfWeek? {
        val clean = normalizeGreek(text.replace("\\s+".toRegex(), "").trim())
        return when {
            clean.contains("ΔΕΥΤΕΡ") || clean.contains("MONDAY") -> DayOfWeek.MONDAY
            clean.contains("ΤΡΙΤ") || clean.contains("TUESDAY") -> DayOfWeek.TUESDAY
            clean.contains("ΤΕΤΑΡΤ") || clean.contains("WEDNESDAY") -> DayOfWeek.WEDNESDAY
            clean.contains("ΠΕΜΠΤ") || clean.contains("THURSDAY") -> DayOfWeek.THURSDAY
            clean.contains("ΠΑΡΑΣΚΕΥ") || clean.contains("FRIDAY") -> DayOfWeek.FRIDAY
            clean.contains("ΣΑΒΒΑΤ") || clean.contains("SATURDAY") -> DayOfWeek.SATURDAY
            clean.contains("ΚΥΡΙΑΚ") || clean.contains("SUNDAY") -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun extractTimeRange(text: String): Pair<LocalTime, LocalTime>? {
        // Strip out whitespace within digits or time colons e.g. "0 9 :00 –  1 0:00" -> "09:00–10:00"
        val normalized = text.replace(" ", "")
        val matcher = timeRangePattern.matcher(normalized)
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
            combined.contains("lab") || combined.contains("(ε)") || combined.contains("(απ)") || combined.contains("εργαστήριο") || combined.contains("εργαστηριακό") -> EventType.LAB
            combined.contains("deadline") || combined.contains("προθεσμία") || combined.contains("παράδοση") -> EventType.DEADLINE
            combined.contains("lecture") || combined.contains("(θ)") || combined.contains("διάλεξη") || combined.contains("θεωρία") -> EventType.LECTURE
            else -> EventType.LECTURE
        }
    }

    private fun generateRecurringEvents(
        title: String,
        description: String,
        location: String,
        semester: String,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        type: EventType,
        sourceUrl: String?,
        baseDate: LocalDate,
        weeks: Int
    ): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        var nextDate = baseDate.with(TemporalAdjusters.nextOrSame(dayOfWeek))

        for (i in 0 until weeks) {
            events.add(
                CalendarEvent(
                    title = title,
                    description = description,
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
