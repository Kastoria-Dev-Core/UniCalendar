package org.kastoriadev.unicalendar.data.parser

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.kastoriadev.unicalendar.domain.model.EventFeed
import java.io.File

/**
 * Parses real department documents (timetables and exam schedules) from the folder named by the
 * UNICALENDAR_CORPUS_DIR environment variable. The documents are not part of the repository, so
 * the test is skipped when the variable is not set.
 */
class RealDocumentCorpusTest {

    @Test
    fun parsesEveryDocumentInCorpus() {
        val directory = System.getenv("UNICALENDAR_CORPUS_DIR")?.let(::File)
        assumeTrue("UNICALENDAR_CORPUS_DIR not set", directory?.isDirectory == true)

        val parser = ScheduleDocumentParser()
        val files = directory!!.listFiles { file -> file.extension.equals("docx", ignoreCase = true) }!!.sorted()
        assertTrue("Corpus folder has no .docx files", files.isNotEmpty())

        // Written as UTF-8 next to the documents; the console encoding may not handle Greek.
        val report = StringBuilder()
        fun println(line: String) {
            report.appendLine(line)
        }

        for (file in files) {
            val result = parser.parse(file.readBytes(), file.name)
            assertTrue("${file.name}: not parsed", result is ScheduleDocumentParser.Result.Parsed)
            result as ScheduleDocumentParser.Result.Parsed
            val events = result.events
            assertTrue("${file.name}: no events", events.isNotEmpty())
            assertTrue("${file.name}: blank title", events.none { it.title.isBlank() })
            assertTrue("${file.name}: end before start", events.none { !it.endTime.isAfter(it.startTime) })

            println("=== ${file.name}: ${result.feed}, ${events.size} events, " +
                "${events.minOf { it.startTime }.toLocalDate()} .. ${events.maxOf { it.startTime }.toLocalDate()}")
            println("semesters: " + events.groupBy { it.semester }.mapValues { it.value.size }.toSortedMap())

            val samples = if (result.feed == EventFeed.TIMETABLE) {
                events.distinctBy { listOf(it.title, it.semester, it.startTime.dayOfWeek, it.startTime.toLocalTime()) }
            } else {
                events
            }
            for (event in samples.sortedWith(compareBy({ it.semester }, { it.startTime }))) {
                val day = if (result.feed == EventFeed.TIMETABLE) event.startTime.dayOfWeek.toString().take(3) else event.startTime.toLocalDate().toString()
                println(
                    "  [${event.semester}] $day ${event.startTime.toLocalTime()}-${event.endTime.toLocalTime()} " +
                        "${event.type} | ${event.title} | ${event.location} | ${event.description.replace("\n", " / ")}"
                )
            }
        }
        File(directory, "corpus-report.txt").writeText(report.toString(), Charsets.UTF_8)
    }
}
