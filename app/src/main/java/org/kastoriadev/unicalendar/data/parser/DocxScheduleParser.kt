package org.kastoriadev.unicalendar.data.parser

import org.kastoriadev.unicalendar.data.parser.DocxReader.GridCell
import org.kastoriadev.unicalendar.data.parser.DocxReader.VerticalMerge
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventFeed
import org.kastoriadev.unicalendar.domain.model.EventType
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Parses the department's weekly timetable (.docx) into dated lessons.
 *
 * Two layouts are understood:
 *  - grid tables: a header row of weekday columns, one row per hour, courses as vertically merged
 *    cells (the official UOWM layout; days may be split into parallel sub-columns);
 *  - row tables: one row per lesson with the day, time range and course in separate cells.
 *
 * Lessons repeat weekly from the start of the term, skipping Greek university holidays.
 */
class DocxScheduleParser(
    private val today: () -> LocalDate = { LocalDate.now() },
    private val weeksPerTerm: Int = TERM_WEEKS
) : ScheduleParser {

    override fun parse(content: String, sourceUrl: String?): List<CalendarEvent> =
        parseStream(ByteArrayInputStream(content.toByteArray(Charsets.ISO_8859_1)), sourceUrl)

    override fun parseStream(stream: InputStream, sourceUrl: String?): List<CalendarEvent> {
        val document = DocxReader.readDocument(stream) ?: return emptyList()
        return parseDocument(document, sourceUrl)
    }

    fun parseDocumentXml(xmlContent: String, sourceUrl: String?): List<CalendarEvent> =
        parseDocument(DocxReader.parseXml(xmlContent.toByteArray(Charsets.UTF_8)), sourceUrl)

    internal fun parseDocument(document: Document, sourceUrl: String?): List<CalendarEvent> {
        val blocks = DocxReader.bodyBlocks(document)
        val heading = blocks
            .takeWhile { !DocxReader.isTable(it) }
            .joinToString(" ") { DocxReader.paragraphText(it) }
        val termStart = AcademicCalendar.termStart(heading, sourceUrl, today())

        val events = mutableListOf<CalendarEvent>()
        var currentSemester = ""
        var currentDay: DayOfWeek? = null

        for (block in blocks) {
            if (DocxReader.isParagraph(block)) {
                detectSemester(DocxReader.paragraphText(block))?.let { currentSemester = it }
                continue
            }

            val rows = DocxReader.tableRows(block).map(DocxReader::rowCells).filter { it.isNotEmpty() }
            if (rows.isEmpty()) continue

            val headerIndex = rows.take(HEADER_SEARCH_ROWS).indexOfFirst { row ->
                dayColumns(row).values.toSet().size >= 2
            }
            if (headerIndex >= 0) {
                // A title row above the weekday header ("Α΄ ΕΞΑΜΗΝΟ") overrides the paragraph heading.
                val tableSemester = rows.take(headerIndex)
                    .firstNotNullOfOrNull { row -> detectSemester(row.joinToString(" ") { it.text }) }
                    ?: currentSemester
                events += parseGrid(rows, headerIndex, tableSemester, sourceUrl, termStart)
            } else {
                currentDay = parseRows(rows, currentSemester, currentDay, sourceUrl, termStart, events)
            }
        }

        return events.distinctBy { listOf(it.title, it.startTime, it.endTime, it.semester, it.location) }
    }

    // region Grid layout

    private class OpenCourse(val startRow: Int, var endRow: Int, val lines: List<String>, val day: DayOfWeek)

    private fun parseGrid(
        rows: List<List<GridCell>>,
        headerIndex: Int,
        semester: String,
        sourceUrl: String?,
        termStart: LocalDate
    ): List<CalendarEvent> {
        val dayByColumn = dayColumns(rows[headerIndex])

        // The hour of each row comes from a non-day cell ("08:00 – 09:00"); some layouts repeat it
        // in a trailing column, the first one wins.
        val slots = HashMap<Int, Pair<LocalTime, LocalTime>>()
        for (r in headerIndex + 1 until rows.size) {
            rows[r].asSequence()
                .filter { cell -> cell.columns.none { it in dayByColumn } }
                .firstNotNullOfOrNull { extractTimeRange(it.text) }
                ?.let { slots[r] = it }
        }

        val events = mutableListOf<CalendarEvent>()
        val open = HashMap<Int, OpenCourse>()

        fun close(column: Int) {
            val course = open.remove(column) ?: return
            val start = slots[course.startRow]?.first ?: return
            val end = slots[course.endRow]?.second ?: return
            events += lessons(course.lines, course.day, start, end, semester, sourceUrl, termStart)
        }

        for (r in headerIndex + 1 until rows.size) {
            if (r !in slots) continue
            for (cell in rows[r]) {
                val column = cell.startColumn
                val day = dayByColumn[column] ?: continue
                when (cell.verticalMerge) {
                    VerticalMerge.CONTINUE -> open[column]?.endRow = r
                    VerticalMerge.RESTART -> {
                        close(column)
                        if (cell.lines.isNotEmpty()) open[column] = OpenCourse(r, r, cell.lines, day)
                    }
                    VerticalMerge.NONE -> {
                        close(column)
                        if (cell.lines.isNotEmpty()) {
                            open[column] = OpenCourse(r, r, cell.lines, day)
                            close(column)
                        }
                    }
                }
            }
        }
        open.keys.toList().forEach(::close)
        return events
    }

    /** Logical grid column -> weekday, for the header cells that name a day. */
    private fun dayColumns(cells: List<GridCell>): Map<Int, DayOfWeek> {
        val result = HashMap<Int, DayOfWeek>()
        for (cell in cells) {
            val text = cell.text
            // Day headers are short; dated headers belong to exam grids, not weekly timetables.
            if (text.length > MAX_DAY_HEADER_LENGTH || DATE_PATTERN.containsMatchIn(text)) continue
            val day = GreekText.dayOfWeek(text) ?: continue
            for (column in cell.columns) result[column] = day
        }
        return result
    }

    // endregion

    // region Row layout

    private fun parseRows(
        rows: List<List<GridCell>>,
        semester: String,
        initialDay: DayOfWeek?,
        sourceUrl: String?,
        termStart: LocalDate,
        out: MutableList<CalendarEvent>
    ): DayOfWeek? {
        var currentDay = initialDay
        for (cells in rows) {
            val texts = cells.map { it.text }.filter { it.isNotBlank() }
            if (texts.isEmpty()) continue
            val rowText = texts.joinToString(" ")
            val rowDay = texts.firstNotNullOfOrNull { text ->
                GreekText.dayOfWeek(text)?.takeIf { text.length <= MAX_DAY_HEADER_LENGTH }
            }
            val day = rowDay ?: currentDay
            val time = extractTimeRange(rowText)
            if (day != null && time != null) {
                val title = extractCourseTitle(texts)
                if (title.isNotBlank()) {
                    out += weeklyDates(day, termStart).map { date ->
                        CalendarEvent(
                            title = title,
                            location = extractLocation(texts),
                            semester = semester,
                            startTime = date.atTime(time.first),
                            endTime = date.atTime(time.second),
                            type = inferLessonType(title),
                            sourceUrl = sourceUrl,
                            isSynced = true,
                            feed = EventFeed.TIMETABLE
                        )
                    }
                }
            }
            if (rowDay != null) currentDay = rowDay
        }
        return currentDay
    }

    private fun extractCourseTitle(cells: List<String>): String =
        cells.firstOrNull { cell ->
            val folded = GreekText.fold(cell)
            cell.length > 3 &&
                GreekText.dayOfWeek(cell) == null &&
                extractTimeRange(cell) == null &&
                NON_TITLE_KEYWORDS.none { folded.contains(it) }
        }?.collapseWhitespace().orEmpty()

    private fun extractLocation(cells: List<String>): String {
        cells.firstOrNull { isLocationLine(it) }?.let { return it.collapseWhitespace() }
        return ""
    }

    // endregion

    // region Cell content

    /**
     * Splits a timetable cell into (title, instructor, location). Cells list the course title
     * first (possibly over several lines), then the instructor(s), then the room.
     */
    fun parseCellContent(lines: List<String>): Triple<String, String, String> {
        val cleaned = lines.map { it.collapseWhitespace() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return Triple("", "", "")

        val instructorStart = (1 until cleaned.size).firstOrNull { isInstructorLine(cleaned[it]) }
        val titleLines: List<String>
        val instructorLines: List<String>
        val locationLines: List<String>
        if (instructorStart != null) {
            var instructorEnd = instructorStart
            while (instructorEnd < cleaned.size && isInstructorLine(cleaned[instructorEnd])) instructorEnd++
            titleLines = cleaned.subList(0, instructorStart)
            instructorLines = cleaned.subList(instructorStart, instructorEnd)
            locationLines = cleaned.subList(instructorEnd, cleaned.size)
        } else {
            val locationStart = (1 until cleaned.size).firstOrNull { isLocationLine(cleaned[it]) }
            titleLines = if (locationStart != null) cleaned.subList(0, locationStart) else cleaned
            instructorLines = emptyList()
            locationLines = if (locationStart != null) cleaned.subList(locationStart, cleaned.size) else emptyList()
        }

        val title = joinLines(titleLines).replace(" - ", "-")
        val instructor = instructorLines.joinToString(", ")
        val location = balanceGuillemets(joinLines(locationLines))
        return Triple(title, instructor, location)
    }

    private fun lessons(
        lines: List<String>,
        day: DayOfWeek,
        start: LocalTime,
        end: LocalTime,
        semester: String,
        sourceUrl: String?,
        termStart: LocalDate
    ): List<CalendarEvent> {
        if (!end.isAfter(start)) return emptyList()
        val (title, instructor, location) = parseCellContent(lines)
        if (title.isBlank()) return emptyList()
        val description = if (instructor.isNotBlank()) "$INSTRUCTOR_LABEL $instructor" else ""
        val type = inferLessonType(title)
        return weeklyDates(day, termStart).map { date ->
            CalendarEvent(
                title = title,
                description = description,
                location = location,
                semester = semester,
                startTime = date.atTime(start),
                endTime = date.atTime(end),
                type = type,
                sourceUrl = sourceUrl,
                isSynced = true,
                feed = EventFeed.TIMETABLE
            )
        }
    }

    private fun weeklyDates(day: DayOfWeek, termStart: LocalDate): List<LocalDate> {
        val first = termStart.with(TemporalAdjusters.nextOrSame(day))
        return (0 until weeksPerTerm)
            .map { first.plusWeeks(it.toLong()) }
            .filterNot(AcademicCalendar::isNoLectureDay)
    }

    // endregion

    /** Semester key announced by a heading ("Γ΄ εξάμηνο", "3ο ΕΞΑΜΗΝΟ", "...Παιδαγωγικής Επάρκειας"). */
    fun detectSemester(text: String): String? {
        // Headings are short; long paragraphs are notes that may mention other semesters.
        if (text.isBlank() || text.length > MAX_HEADING_LENGTH) return null
        val folded = GreekText.fold(text).collapseWhitespace()
        if ((folded.contains("ΠΑΙΔΑΓΩΓΙΚ") && folded.contains("ΕΠΑΡΚΕΙ")) || PDE_TOKEN.containsMatchIn(folded)) {
            return CalendarEvent.SEMESTER_PDE
        }
        val canonical = GreekText.toGreekLookalikes(folded).replace(GreekText.PRIME_CHARS, "'")
        NUMERAL_SEMESTER.find(canonical)?.let { match ->
            GreekText.semesterFromNumeral(match.groupValues[2])?.let { return it }
        }
        DIGIT_SEMESTER.find(canonical)?.let { return it.groupValues[2] }
        SEMESTER_DIGIT.find(canonical)?.let { return it.groupValues[1] }
        return null
    }

    fun detectDayOfWeek(text: String): DayOfWeek? = GreekText.dayOfWeek(text)

    companion object {
        /** Teaching weeks generated for a term (13 lecture weeks plus the Christmas / Easter breaks). */
        const val TERM_WEEKS = 16

        internal const val INSTRUCTOR_LABEL = "Διδάσκων:"

        private const val HEADER_SEARCH_ROWS = 3
        private const val MAX_DAY_HEADER_LENGTH = 24
        private const val MAX_HEADING_LENGTH = 160

        private val TIME_RANGE = Regex("(\\d{1,2}[:.]\\d{2}|\\b\\d{1,2}\\b)(?:-|–|—|to)(\\d{1,2}[:.]\\d{2}|\\b\\d{1,2}\\b)")
        private val DATE_PATTERN = Regex("\\d{1,2}\\s*[./-]\\s*\\d{1,2}\\s*[./-]\\s*\\d{2,4}")
        private val PDE_TOKEN = Regex("(^|[^\\p{L}])(ΠΔΕ|PDE)([^\\p{L}]|$)")
        private val NUMERAL_SEMESTER = Regex("(^|[^Α-Ω])(ΣΤ|[ΑΒΓΔΕΖΗ])\\s*'?\\s*ΕΞΑΜΗΝ")
        private val DIGIT_SEMESTER = Regex("(^|[^0-9])([1-8])\\s*(?:ΟΥ|Ο)?\\s*ΕΞΑΜΗΝ")
        private val SEMESTER_DIGIT = Regex("ΕΞΑΜΗΝΟ\\s*([1-8])(?![0-9])")
        private val INSTRUCTOR_LINE = Regex("^(?:\\p{Lu}\\p{Ll}?\\.\\s?){1,3}\\s?\\p{Lu}\\p{Ll}")
        private val ROOM_CODE = Regex("^[ΑΒΓΔΕ]\\d{1,2}([\\s,/–-]+[ΑΒΓΔΕ]?\\d{1,2})*$")
        private val LOCATION_KEYWORDS = listOf(
            "ΑΜΦΙΘΕΑΤΡ", "ΑΜΦ.", "ΕΡΓ.", "ΕΡΓΑΣΤΗΡ", "ΑΙΘΟΥΣ", "ΦΙΛΙΠΠΟΣ", "ΠΕΤΣΑΛΝΙΚ", "ROOM", "LAB "
        )
        private val NON_TITLE_KEYWORDS = listOf("ΑΙΘΟΥΣ", "ΑΜΦΙΘΕΑΤΡ", "ROOM", "ΗΜΕΡΑ", "ΩΡΑ")
        private val LAB_SUFFIX = Regex("[-–]\\s*ΑΠ(?![\\p{L}])")

        private fun extractTimeRange(text: String): Pair<LocalTime, LocalTime>? {
            val normalized = text.replace(Regex("\\s+"), "")
            val match = TIME_RANGE.find(normalized) ?: return null
            val start = parseFlexibleTime(match.groupValues[1]) ?: return null
            val end = parseFlexibleTime(match.groupValues[2]) ?: return null
            return start to end
        }

        private fun parseFlexibleTime(raw: String): LocalTime? {
            val clean = raw.trim().replace('.', ':')
            return try {
                if (clean.contains(':')) {
                    val (hour, minute) = clean.split(':')
                    LocalTime.of(hour.toInt(), minute.toInt())
                } else {
                    LocalTime.of(clean.toInt(), 0)
                }
            } catch (_: Exception) {
                null
            }
        }

        private fun isInstructorLine(line: String): Boolean =
            INSTRUCTOR_LINE.containsMatchIn(line) && !isLocationLine(line)

        private fun isLocationLine(line: String): Boolean {
            val folded = GreekText.fold(line)
            if (LOCATION_KEYWORDS.any { folded.contains(it) }) return true
            return ROOM_CODE.matches(GreekText.toGreekLookalikes(folded).trim())
        }

        /** Joins wrapped cell lines, gluing words that were hyphenated across a line break. */
        private fun joinLines(lines: List<String>): String {
            val builder = StringBuilder()
            for (line in lines) {
                if (builder.isNotEmpty() && !(builder.endsWith("-") || builder.endsWith("–"))) builder.append(' ')
                builder.append(line)
            }
            return builder.toString().collapseWhitespace()
        }

        /** Drops a stray closing «» quote (e.g. "Β11»") and closes an unterminated one. */
        private fun balanceGuillemets(text: String): String {
            val opens = text.count { it == '«' }
            val closes = text.count { it == '»' }
            val result = when {
                opens == 0 && closes > 0 -> text.replace("»", "")
                opens > closes -> text + "»".repeat(opens - closes)
                else -> text
            }
            return result.trim()
        }

        internal fun inferLessonType(title: String): EventType {
            val folded = GreekText.fold(title)
            val isLab = folded.contains("ΕΡΓΑΣΤΗΡ") ||
                folded.contains("(ΑΠ)") ||
                LAB_SUFFIX.containsMatchIn(folded) ||
                folded.contains("(Ε)") ||
                folded.contains("(E)") ||
                folded.contains("LAB")
            return if (isLab) EventType.LAB else EventType.LECTURE
        }
    }
}
