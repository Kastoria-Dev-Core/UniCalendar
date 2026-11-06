package org.kastoriadev.unicalendar.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CrashLogTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var originalHandler: Thread.UncaughtExceptionHandler? = null
    private val reachedSystemHandler = mutableListOf<Throwable>()

    @Before
    fun setUp() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, error -> reachedSystemHandler += error }
    }

    @After
    fun tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
    }

    @Test
    fun crash_isKeptForTheNextLaunchAndStillHandledAsACrash() {
        val file = File(folder.root, "last-crash.txt")
        CrashLog.install(file, header = { "UniCalendar test" })
        val crash = IllegalStateException("No ActivityResultRegistryOwner was provided")

        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), crash)

        // The app must still close as usual; the report is only kept for later.
        assertEquals(listOf<Throwable>(crash), reachedSystemHandler)
        val report = CrashLog.consume(file)!!
        assertTrue(report.startsWith("UniCalendar test\n"))
        assertTrue(report.contains("java.lang.IllegalStateException: No ActivityResultRegistryOwner was provided"))
        assertNull("A report is shown only once", CrashLog.consume(file))
    }
}
