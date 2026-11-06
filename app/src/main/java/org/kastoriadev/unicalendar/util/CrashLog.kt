package org.kastoriadev.unicalendar.util

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

/**
 * Keeps the reason for a crash so the next launch can show it. The app still closes, since
 * nothing can safely carry on after an uncaught error, but the stack trace can then be read and
 * copied on the phone itself instead of needing a computer and logcat.
 */
object CrashLog {

    private const val FILE_NAME = "last-crash.txt"
    private val TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun install(context: Context) = install(File(context.filesDir, FILE_NAME))

    /** The report of the last crash, if any. It is deleted once read, so it is shown only once. */
    fun consume(context: Context): String? = consume(File(context.filesDir, FILE_NAME))

    internal fun install(file: File, header: () -> String = ::deviceSummary) {
        val systemHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val time = LocalDateTime.now().format(TIMESTAMP)
                file.writeText("${header()}\n$time · thread ${thread.name}\n\n${error.toErrorDetails()}")
            } catch (_: Throwable) {
                // Best effort: the normal crash handling below has to run regardless.
            }
            if (systemHandler != null) systemHandler.uncaughtException(thread, error) else exitProcess(2)
        }
    }

    internal fun consume(file: File): String? {
        if (!file.exists()) return null
        return try {
            file.readText().takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            file.delete()
        }
    }
}
