package org.kastoriadev.unicalendar.data.parser

import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventFeed
import org.kastoriadev.unicalendar.domain.model.EventType
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime

/**
 * Parses the department's exam-period schedule (.docx).
 *
 * The documents list a paragraph per day ("Δευτέρα 19/1/2026") followed by a paragraph per exam:
 * `09:00 – 11:00 Course (Α’)(Β’) (Εμβόλιμη), Ι.Σινάτκας, Β1, Β2`. Separators, instructor format
 * and tag positions differ between exam periods, so each line is split by recognising its parts
 * (semester markers, instructor names) rather than by punctuation. Tables are read row by row.
 */
class DocxExamScheduleParser : ScheduleParser {

    override fun parse(content: String, sourceUrl: String?): List<CalendarEvent> =
        parseStream(ByteArrayInputStream(content.toByteArray(Charsets.ISO_8859_1)), sourceUrl)

    override fun parseStream(stream: InputStream, sourceUrl: String?): List<CalendarEvent> {
        val document = DocxReader.readDocument(stream) ?: return emptyList()
        return parseDocument(document, sourceUrl)
    }

    fun parseDocumentXml(xmlContent: String, sourceUrl: String?): List<CalendarEvent> =
        parseDocument(DocxReader.parseXml(xmlContent.toByteArray(Charsets.UTF_8)), sourceUrl)

    internal fun parseDocument(document: Document, sourceUrl: String?): List<CalendarEvent> =
        parseLines(documentLines(document), sourceUrl)

    /** Parses already extracted schedule lines (one exam or day heading per line). */
    fun parseLines(lines: List<String>, sourceUrl: String?): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        var currentDate: LocalDate? = null

        for (raw in lines) {
            var line = raw.collapseWhitespace()
            if (line.isEmpty()) continue

            DATE_HEADING.find(line)?.let { match ->
                val leadingWord = match.groupValues[1]
                val date = toDate(match.groupValues[2], match.groupValues[3], match.groupValues[4])
                if (date != null && (leadingWord.isEmpty() || GreekText.dayOfWeek(leadingWord) != null)) {
                    currentDate = date
                    line = line.substring(match.range.last + 1).trim()
                }
            }

            val time = LEADING_TIME_RANGE.find(line) ?: continue
            val date = currentDate ?: continue
            val start = toTime(time.groupValues[1], time.groupValues[2]) ?: continue
            val end = toTime(time.groupValues[3], time.groupValues[4])
                ?.takeIf { it.isAfter(start) }
                ?: start.plusHours(DEFAULT_EXAM_HOURS)

            val exam = ExamLineParser.parse(line.substring(time.range.last + 1))
            val description = buildList {
                if (exam.instructors.isNotEmpty()) {
                    val label = if (exam.instructors.size > 1) INSTRUCTORS_LABEL else DocxScheduleParser.INSTRUCTOR_LABEL
                    add("$label ${exam.instructors.joinToString(", ")}")
                }
                addAll(exam.notes)
            }.joinToString("\n")

            for (course in exam.courses) {
                if (course.title.isBlank()) continue
                events += CalendarEvent(
                    title = course.title,
                    description = description,
                    location = exam.rooms,
                    semester = CalendarEvent.formatSemesters(course.semesters),
                    startTime = date.atTime(start),
                    endTime = date.atTime(end),
                    type = EventType.EXAM,
                    sourceUrl = sourceUrl,
                    isSynced = true,
                    feed = EventFeed.EXAMS
                )
            }
        }

        return events.distinctBy { listOf(it.title, it.startTime, it.endTime, it.semester, it.location) }
    }

    /**
     * Paragraph lines and table rows in document order. Line breaks inside a paragraph only start
     * a new line when the next part begins with a time or a date; otherwise it is a wrapped exam.
     */
    private fun documentLines(document: Document): List<String> {
        val lines = mutableListOf<String>()
        for (block in DocxReader.bodyBlocks(document)) {
            if (DocxReader.isParagraph(block)) {
                val parts = DocxReader.paragraphLines(block)
                val merged = mutableListOf<String>()
                for (part in parts) {
                    val startsEntry = LEADING_TIME_RANGE.containsMatchIn(part) || DATE_HEADING.containsMatchIn(part)
                    if (merged.isNotEmpty() && !startsEntry) {
                        merged[merged.lastIndex] = merged.last() + " " + part
                    } else {
                        merged += part
                    }
                }
                lines += merged
            } else {
                for (row in DocxReader.tableRows(block)) {
                    val text = DocxReader.rowCells(row).joinToString("  ") { it.text }.trim()
                    if (text.isNotEmpty()) lines += text
                }
            }
        }
        return lines
    }

    private fun toDate(day: String, month: String, year: String): LocalDate? = try {
        val fullYear = year.toInt().let { if (year.length == 2) 2000 + it else it }
        LocalDate.of(fullYear, month.toInt(), day.toInt())
    } catch (_: Exception) {
        null
    }

    private fun toTime(hour: String, minute: String): LocalTime? = try {
        LocalTime.of(hour.toInt(), minute.toInt())
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val DEFAULT_EXAM_HOURS = 2L
        const val INSTRUCTORS_LABEL = "Διδάσκοντες:"

        /** Optional weekday word followed by a full date: "Δευτέρα 31-08-2026", "19/1/2026". */
        val DATE_HEADING = Regex(
            "^(?:(\\p{L}+)\\s*,?\\s*)?(\\d{1,2})\\s*[./-]\\s*(\\d{1,2})\\s*[./-]\\s*(\\d{4}|\\d{2})(?!\\d)"
        )
        val LEADING_TIME_RANGE = Regex("^(\\d{1,2})[:.](\\d{2})\\s*[-–—]\\s*(\\d{1,2})[:.](\\d{2})")
    }
}

/** Splits the text of one exam line (after the time range) into courses, instructors and rooms. */
internal object ExamLineParser {

    data class Course(val title: String, val semesters: List<String>)

    data class Exam(
        val courses: List<Course>,
        val instructors: List<String>,
        val rooms: String,
        val notes: List<String>
    )

    private enum class Kind { SEMESTER, PDE, COUNTS, TAG, OTHER }

    private data class Group(val range: IntRange, val content: String, val kind: Kind)

    fun parse(text: String): Exam {
        val notes = mutableListOf<String>()
        var line = STARRED_NOTE.replace(text) { match ->
            notes += match.groupValues[1].collapseWhitespace()
            " "
        }
        line = line.replace("*", " ").collapseWhitespace()

        val groups = parenthesizedGroups(line)
        groups.filter { it.kind == Kind.TAG }.forEach { notes += it.content.collapseWhitespace() }

        val firstMarker = groups.firstOrNull { it.kind == Kind.SEMESTER || it.kind == Kind.PDE }
        val searchFrom = firstMarker?.range?.last?.plus(1)
            ?: line.indexOf(' ').takeIf { it > 0 }
            ?: line.length
        val split = findInstructorStart(line, searchFrom, groups)
            ?: groups.lastOrNull { it.kind == Kind.SEMESTER || it.kind == Kind.PDE || it.kind == Kind.COUNTS }
                ?.range?.last?.plus(1)
            ?: line.length

        val courses = splitCourses(line.substring(0, split), groups.filter { it.range.last < split })
        val (instructors, rooms) = parseDetails(line.substring(split))
        val cleanNotes = notes
            .filter { it.isNotEmpty() }
            .map { note -> note.replaceFirstChar { it.uppercaseChar() } }
            .distinct()
        return Exam(courses, instructors, rooms, cleanNotes)
    }

    private fun parenthesizedGroups(line: String): List<Group> {
        val groups = mutableListOf<Group>()
        var depth = 0
        var start = -1
        for ((index, char) in line.withIndex()) {
            when (char) {
                '(' -> {
                    if (depth == 0) start = index
                    depth++
                }
                ')' -> if (depth > 0) {
                    depth--
                    if (depth == 0 && start >= 0) {
                        val content = line.substring(start + 1, index)
                        groups += Group(start..index, content, classify(content))
                    }
                }
            }
        }
        return groups
    }

    private fun classify(content: String): Kind {
        val folded = GreekText.fold(content).collapseWhitespace()
        if (COUNTS.matches(folded)) return Kind.COUNTS
        if (PDE_TOKEN.containsMatchIn(folded)) return Kind.PDE
        if (TAG_KEYWORDS.any { folded.contains(it) }) return Kind.TAG
        if (semesterTokens(content)?.isNotEmpty() == true) return Kind.SEMESTER
        return Kind.OTHER
    }

    /** Tokens of a semester marker ("Ε’/ΣΤ’" -> [5, 6]), or null when [content] is not one. */
    private fun semesterTokens(content: String): List<String>? {
        val canonical = GreekText.canonical(content)
            .replace(GreekText.PRIME_CHARS, "'")
            .replace(Regex("\\s+'"), "'")
        // "Ε’/ΣΤ’", "Ε’-Ζ’" and "Γ’ ΣΤ’" all list several semesters.
        val tokens = canonical.split(Regex("[/,\\s–—-]+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty() || !tokens.all { SEMESTER_TOKEN.matches(it) }) return null
        return tokens.mapNotNull { GreekText.semesterFromNumeral(it) }
    }

    private fun findInstructorStart(line: String, from: Int, groups: List<Group>): Int? {
        if (from >= line.length) return null
        return sequenceOf(INITIALS_NAME, CAPS_NAME)
            .flatMap { it.findAll(line, from) }
            .filter { match -> groups.none { match.range.first in it.range } }
            .filterNot { isStopWord(it.value) }
            .minOfOrNull { it.range.first }
    }

    /**
     * Two courses examined together are written "A (Ε’) / B (Ζ’)": split only at separators
     * that directly follow a semester marker, so "A / B (Α’)" stays one course with two names.
     */
    private fun splitCourses(coursePart: String, groups: List<Group>): List<Course> {
        val cuts = groups
            .filter { it.kind == Kind.SEMESTER || it.kind == Kind.PDE }
            .mapNotNull { group ->
                if (group.range.last + 1 >= coursePart.length) null
                else COURSE_SEPARATOR.matchAt(coursePart, group.range.last + 1)?.range
            }

        val segments = mutableListOf<IntRange>()
        var segmentStart = 0
        for (cut in cuts.sortedBy { it.first }) {
            segments += segmentStart until cut.first
            segmentStart = cut.last + 1
        }
        segments += segmentStart until coursePart.length

        return segments.mapNotNull { segment ->
            val inside = groups.filter { it.range.first >= segment.first && it.range.last <= segment.last }
            val title = buildTitle(coursePart, segment, inside)
            if (title.isEmpty()) return@mapNotNull null
            val semesters = if (inside.any { it.kind == Kind.PDE }) {
                listOf(CalendarEvent.SEMESTER_PDE)
            } else {
                inside.filter { it.kind == Kind.SEMESTER }.flatMap { semesterTokens(it.content).orEmpty() }.distinct()
            }
            Course(title, semesters)
        }
    }

    private fun buildTitle(text: String, segment: IntRange, groups: List<Group>): String {
        if (segment.isEmpty()) return ""
        val builder = StringBuilder(text.substring(segment.first, segment.last + 1))
        // Remove markers back to front so earlier ranges stay valid.
        for (group in groups.sortedByDescending { it.range.first }) {
            if (group.kind == Kind.OTHER) continue
            builder.delete(group.range.first - segment.first, group.range.last - segment.first + 1)
        }
        return builder.toString()
            .collapseWhitespace()
            .replace(Regex("\\s+,"), ",")
            .trim(' ', ',', ';', '/', '–', '—', '-')
            .trim()
    }

    private fun parseDetails(details: String): Pair<List<String>, String> {
        var rest = details
        // Tags such as "(Εμβόλιμη)" may trail the rooms; they are already collected as notes.
        for (group in parenthesizedGroups(rest).sortedByDescending { it.range.first }) {
            if (group.kind != Kind.OTHER) rest = rest.removeRange(group.range)
        }

        val instructors = mutableListOf<String>()
        var index = 0
        while (index < rest.length) {
            val position = SEPARATORS.matchAt(rest, index)?.range?.last?.plus(1) ?: index
            if (position >= rest.length) break
            val match = INITIALS_NAME.matchAt(rest, position) ?: CAPS_NAME.matchAt(rest, position)
            if (match == null || isStopWord(match.value)) break
            instructors += match.value.collapseWhitespace()
            index = match.range.last + 1
        }

        val rooms = rest.substring(index.coerceAtMost(rest.length))
            .collapseWhitespace()
            .trim(' ', ',', ';', '/', '–', '—', '-')
            .trim()
        return instructors to rooms
    }

    private fun isStopWord(word: String): Boolean = GreekText.fold(word) in STOP_WORDS

    private val STARRED_NOTE = Regex("\\*{2,}\\s*([^*()]+?)\\s*\\*{2,}")
    private val COUNTS = Regex("\\d+(\\s*\\+\\s*\\d+)*")
    private val PDE_TOKEN = Regex("(^|[^\\p{L}])(ΠΔΕ|PDE)([^\\p{L}]|$)")
    private val SEMESTER_TOKEN = Regex("(ΣΤ|Ϛ|[ΑΒΓΔΕΖΗ])'")
    private val TAG_KEYWORDS = listOf("ΕΜΒΟΛΙΜ", "ΠΑΛΑΙΟ", "ΠΠΣ", "ΣΥΝΕΞΕΤΑΣ")
    private val COURSE_SEPARATOR = Regex("\\s*,?\\s*[/–—-]\\s*")
    private val SEPARATORS = Regex("[\\s,;/–—-]+")

    /** "Ι.Σινάτκας", "Γ. Βασιλειάδης", "Β.Μ.Πανάτσα", "Χρ. Χυτήρης", but not the room "Μ.Αμφ.". */
    private val INITIALS_NAME = Regex(
        "(?<![\\p{L}])(?:\\p{Lu}\\p{Ll}?\\.\\s?){1,3}(?!Αμφ|ΑΜΦ|Αιθ|Εργ)\\p{Lu}\\p{Ll}+(?:-\\p{Lu}\\p{Ll}+)?"
    )

    /** Surname in capitals: "ΣΙΝΑΤΚΑΣ", "ΔΟΣΗΣ-ΚΑΡΒΟΥΝΗΣ". */
    private val CAPS_NAME = Regex(
        "(?<![\\p{L}.])[Α-ΩΆΈΉΊΌΎΏA-Z]{4,}(?:-[Α-ΩΆΈΉΊΌΎΏA-Z]{4,})?(?![\\p{L}.\\d])"
    )

    private val STOP_WORDS = setOf(
        "ΑΜΦΙΘΕΑΤΡΟ", "ΕΡΓΑΣΤΗΡΙΟ", "ΑΙΘΟΥΣΑ", "ΑΙΘΟΥΣΕΣ", "ΤΜΗΜΑ", "ΚΤΗΡΙΟ", "ΚΤΙΡΙΟ", "ΣΗΜΕΙΩΣΗ"
    )
}
