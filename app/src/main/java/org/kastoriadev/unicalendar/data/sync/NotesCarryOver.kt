package org.kastoriadev.unicalendar.data.sync

import org.kastoriadev.unicalendar.data.parser.GreekText
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * Re-sync replaces synced events with freshly parsed ones. Personal notes live on the old rows,
 * so they are moved onto the matching new occurrence: same course on the same day (closest start
 * time wins), or failing that, the only new event on that day starting at the same time.
 */
internal object NotesCarryOver {

    fun apply(previous: List<CalendarEvent>, incoming: List<CalendarEvent>): List<CalendarEvent> {
        val withNotes = previous.filter { it.notes.isNotBlank() }
        if (withNotes.isEmpty() || incoming.isEmpty()) return incoming

        val result = incoming.toMutableList()
        val assigned = BooleanArray(result.size)
        val unmatched = mutableListOf<CalendarEvent>()

        val incomingByCourseDay = result.indices.groupBy { CourseDay(titleKey(result[it].title), result[it].startTime.toLocalDate()) }
        for (old in withNotes) {
            val candidates = incomingByCourseDay[CourseDay(titleKey(old.title), old.startTime.toLocalDate())]
                ?.filterNot { assigned[it] }
                .orEmpty()
            val best = candidates.minByOrNull { minutesBetween(result[it].startTime, old.startTime) }
            if (best == null) {
                unmatched += old
            } else {
                assigned[best] = true
                result[best] = result[best].copy(notes = old.notes)
            }
        }

        // Second pass: a renamed course still keeps its slot, so match by exact day and time.
        for (old in unmatched) {
            val sameSlot = result.indices.filter { !assigned[it] && result[it].startTime == old.startTime }
            if (sameSlot.size == 1) {
                assigned[sameSlot[0]] = true
                result[sameSlot[0]] = result[sameSlot[0]].copy(notes = old.notes)
            }
        }
        return result
    }

    private data class CourseDay(val title: String, val date: LocalDate)

    /** Accent/case/spacing-insensitive course key ("Πιθανότητες - Στατιστική" == "πιθανοτητες-στατιστικη"). */
    private fun titleKey(title: String): String =
        GreekText.fold(title)
            .replace(Regex("\\s*[-–—]\\s*"), "-")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun minutesBetween(a: LocalDateTime, b: LocalDateTime): Long =
        abs(Duration.between(a, b).toMinutes())
}
