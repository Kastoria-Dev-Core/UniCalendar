package org.kastoriadev.unicalendar.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kastoriadev.unicalendar.data.sync.ScheduleSyncManager
import org.kastoriadev.unicalendar.domain.model.AppLanguage
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.DisplayLayoutMode
import org.kastoriadev.unicalendar.domain.model.EventFeed
import org.kastoriadev.unicalendar.domain.model.ThemeMode
import org.kastoriadev.unicalendar.domain.repository.EventRepository
import org.kastoriadev.unicalendar.ui.calendar.CalendarViewModel
import org.kastoriadev.unicalendar.ui.calendar.ErrorReport
import org.kastoriadev.unicalendar.ui.calendar.MessageAction
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeEventRepository : EventRepository {
        val eventsFlow = MutableStateFlow<List<CalendarEvent>>(emptyList())

        override fun getAllEvents(): Flow<List<CalendarEvent>> = eventsFlow
        override suspend fun getEventById(id: Long): CalendarEvent? = eventsFlow.value.firstOrNull { it.id == id }
        override suspend fun insertEvent(event: CalendarEvent): Long {
            eventsFlow.value = (eventsFlow.value.filterNot { it.id == event.id } + event).sortedBy { it.startTime }
            return event.id
        }
        override suspend fun insertEvents(events: List<CalendarEvent>): List<Long> = events.map { insertEvent(it) }
        override suspend fun updateEvent(event: CalendarEvent) {
            insertEvent(event)
        }
        override suspend fun updateNotes(id: Long, notes: String) {
            eventsFlow.value = eventsFlow.value.map { if (it.id == id) it.copy(notes = notes) else it }
        }
        override suspend fun deleteEvent(event: CalendarEvent) = deleteEventById(event.id)
        override suspend fun deleteEventById(id: Long) {
            eventsFlow.value = eventsFlow.value.filterNot { it.id == id }
        }
        override suspend fun replaceSyncedEvents(
            feed: EventFeed,
            semester: String?,
            from: LocalDateTime,
            to: LocalDateTime,
            events: List<CalendarEvent>
        ): Int = events.size
        override suspend fun clearAllEvents() {
            eventsFlow.value = emptyList()
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun semesterFiltering_isolatesEachSemesterCalendar() = runTest {
        val repo = FakeEventRepository()
        val syncManager = ScheduleSyncManager(repo)
        val targetDate = LocalDate.of(2026, 11, 3)

        val sampleEvents = listOf(
            CalendarEvent(
                id = 1,
                title = "Ηλεκτρονική",
                semester = "1",
                startTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(9, 0)),
                endTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(11, 0))
            ),
            CalendarEvent(
                id = 2,
                title = "Μεταγλωττιστές",
                semester = "3",
                startTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(11, 0)),
                endTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(13, 0))
            ),
            CalendarEvent(
                id = 3,
                title = "Αλγόριθμοι Μηχανικής Μάθησης",
                semester = "7",
                startTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(9, 0)),
                endTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(11, 0))
            ),
            CalendarEvent(
                id = 4,
                title = "Εισαγωγή στην Παιδαγωγική",
                semester = "PDE",
                startTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(17, 0)),
                endTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(20, 0))
            ),
            CalendarEvent(
                id = 5,
                title = "Personal Assignment Study",
                semester = "",
                startTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(14, 0)),
                endTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(15, 0))
            )
        )

        repo.eventsFlow.value = sampleEvents

        val viewModel = CalendarViewModel(repo, syncManager)
        viewModel.onDateSelected(targetDate)
        advanceUntilIdle()

        // 1. By default with no semester set (or ALL): all 5 events visible
        assertEquals(5, viewModel.uiState.value.allEvents.size)

        // 2. Select Semester 1 (without PDE)
        viewModel.onSemesterConfirmed(semester = "1", includePde = false)
        advanceUntilIdle()

        val sem1DayEvents = viewModel.uiState.value.selectedDateEvents
        val sem1Titles = sem1DayEvents.map { it.title }
        assertTrue("Sem 1 has Ηλεκτρονική", sem1Titles.contains("Ηλεκτρονική"))
        assertTrue("Sem 1 has personal study", sem1Titles.contains("Personal Assignment Study"))
        assertFalse("Sem 1 does not have Μεταγλωττιστές", sem1Titles.contains("Μεταγλωττιστές"))
        assertFalse("Sem 1 does not have ML", sem1Titles.contains("Αλγόριθμοι Μηχανικής Μάθησης"))
        assertFalse("Sem 1 does not have PDE", sem1Titles.contains("Εισαγωγή στην Παιδαγωγική"))

        // 3. Select Semester 1 WITH PDE included
        viewModel.onSemesterConfirmed(semester = "1", includePde = true)
        advanceUntilIdle()

        val sem1WithPdeTitles = viewModel.uiState.value.selectedDateEvents.map { it.title }
        assertTrue("Sem 1 has Ηλεκτρονική", sem1WithPdeTitles.contains("Ηλεκτρονική"))
        assertTrue("Sem 1 + PDE has PDE course", sem1WithPdeTitles.contains("Εισαγωγή στην Παιδαγωγική"))
        assertFalse("Sem 1 + PDE does not have Sem 3", sem1WithPdeTitles.contains("Μεταγλωττιστές"))

        // 4. Switch calendar to Semester 3
        viewModel.setSemesterFilter("3")
        advanceUntilIdle()

        val sem3Titles = viewModel.uiState.value.selectedDateEvents.map { it.title }
        assertTrue("Sem 3 has Μεταγλωττιστές", sem3Titles.contains("Μεταγλωττιστές"))
        assertFalse("Sem 3 does not have Ηλεκτρονική", sem3Titles.contains("Ηλεκτρονική"))
        assertFalse("Sem 3 does not have ML", sem3Titles.contains("Αλγόριθμοι Μηχανικής Μάθησης"))

        // 5. Switch to PDE only
        viewModel.setSemesterFilter("PDE")
        advanceUntilIdle()

        val pdeOnlyTitles = viewModel.uiState.value.selectedDateEvents.map { it.title }
        assertTrue("PDE calendar has PDE course", pdeOnlyTitles.contains("Εισαγωγή στην Παιδαγωγική"))
        assertFalse("PDE calendar does not have Ηλεκτρονική", pdeOnlyTitles.contains("Ηλεκτρονική"))
        assertFalse("PDE calendar does not have Μεταγλωττιστές", pdeOnlyTitles.contains("Μεταγλωττιστές"))
    }

    @Test
    fun appearanceSettings_andLayoutModes_toggleCorrectly() = runTest {
        val repo = FakeEventRepository()
        val syncManager = ScheduleSyncManager(repo)
        val viewModel = CalendarViewModel(repo, syncManager)

        // Default layout is SPLIT, theme is LIGHT
        assertEquals(DisplayLayoutMode.SPLIT, viewModel.uiState.value.displayLayoutMode)
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.showSettingsScreen)

        // Open settings
        viewModel.openSettings()
        assertTrue(viewModel.uiState.value.showSettingsScreen)

        // Change theme to DARK
        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        // Change layout to FULLSCREEN
        viewModel.setDisplayLayoutMode(DisplayLayoutMode.FULLSCREEN)
        assertEquals(DisplayLayoutMode.FULLSCREEN, viewModel.uiState.value.displayLayoutMode)

        // In FULLSCREEN mode, selecting a date opens the Day Details sheet
        viewModel.onDateSelected(LocalDate.of(2026, 11, 4))
        assertTrue(viewModel.uiState.value.showDayDetailsSheet)

        // Dismiss sheet
        viewModel.dismissDayDetailsSheet()
        assertFalse(viewModel.uiState.value.showDayDetailsSheet)

        // Switch back to SPLIT mode
        viewModel.setDisplayLayoutMode(DisplayLayoutMode.SPLIT)
        viewModel.onDateSelected(LocalDate.of(2026, 11, 5))
        // In SPLIT mode, showDayDetailsSheet stays false because details are shown below
        assertFalse(viewModel.uiState.value.showDayDetailsSheet)

        // Close settings
        viewModel.dismissSettings()
        assertFalse(viewModel.uiState.value.showSettingsScreen)
    }

    @Test
    fun languageSettings_toggleCorrectly() = runTest {
        val repo = FakeEventRepository()
        val syncManager = ScheduleSyncManager(repo)
        val viewModel = CalendarViewModel(repo, syncManager)

        assertEquals(AppLanguage.GREEK, viewModel.uiState.value.appLanguage)

        viewModel.setAppLanguage(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.appLanguage)

        viewModel.setAppLanguage(AppLanguage.GREEK)
        assertEquals(AppLanguage.GREEK, viewModel.uiState.value.appLanguage)
    }

    @Test
    fun today_movesTheCalendarWithoutOpeningDayDetails() = runTest {
        val repo = FakeEventRepository()
        val viewModel = CalendarViewModel(repo, ScheduleSyncManager(repo))
        viewModel.setDisplayLayoutMode(DisplayLayoutMode.FULLSCREEN)
        viewModel.onDateSelected(LocalDate.of(2026, 1, 15))
        viewModel.dismissDayDetailsSheet()

        viewModel.onTodaySelected()

        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
        assertFalse(viewModel.uiState.value.showDayDetailsSheet)
    }

    @Test
    fun deleteEvent_canBeUndoneWithNotesIntact() = runTest {
        val repo = FakeEventRepository()
        val start = LocalDateTime.of(2026, 11, 3, 9, 0)
        val lesson = CalendarEvent(id = 7, title = "Φυσική", notes = "Κεφάλαιο 2", startTime = start, endTime = start.plusHours(2))
        repo.eventsFlow.value = listOf(lesson)
        val viewModel = CalendarViewModel(repo, ScheduleSyncManager(repo))
        advanceUntilIdle()

        viewModel.deleteEvent(lesson)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.allEvents.isEmpty())
        assertEquals(MessageAction.UNDO_DELETE, viewModel.uiState.value.userMessage?.action)

        viewModel.onMessageAction(MessageAction.UNDO_DELETE)
        advanceUntilIdle()
        assertEquals(listOf(lesson), viewModel.uiState.value.allEvents)
    }

    @Test
    fun importFile_showsUnexpectedErrorsInADialogInsteadOfCrashing() = runTest {
        val repo = FakeEventRepository()
        val viewModel = CalendarViewModel(repo, ScheduleSyncManager(repo, ioDispatcher = testDispatcher))

        viewModel.importFile(openStream = { throw StackOverflowError("parser recursion") }, fileName = { "timetable.docx" })
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSyncing)
        assertEquals(ErrorReport.Kind.SYNC, state.errorReport?.kind)
        assertTrue(state.errorReport!!.details.startsWith("java.lang.StackOverflowError: parser recursion"))
        // The dialog explains it; no snackbar repeats the failure.
        assertEquals(null, state.userMessage)

        viewModel.onErrorReportDismissed()
        assertEquals(null, viewModel.uiState.value.errorReport)
    }

    @Test
    fun semesterFilter_handlesMultiSemesterEventsAndPdeToggle() = runTest {
        val repo = FakeEventRepository()
        val date = LocalDate.of(2026, 9, 2)
        repo.eventsFlow.value = listOf(
            CalendarEvent(id = 1, title = "Τεχνολογία Πολυμέσων", semester = "5,6", startTime = date.atTime(15, 0), endTime = date.atTime(17, 0)),
            CalendarEvent(id = 2, title = "Εκπαιδευτική Ψυχολογία", semester = "PDE", startTime = date.atTime(9, 0), endTime = date.atTime(11, 0))
        )
        val viewModel = CalendarViewModel(repo, ScheduleSyncManager(repo))
        viewModel.onDateSelected(date)
        advanceUntilIdle()

        fun visibleTitles() = viewModel.uiState.value.selectedDateEvents.map { it.title }

        viewModel.setSemesterFilter("6")
        assertEquals(listOf("Τεχνολογία Πολυμέσων"), visibleTitles())
        viewModel.setSemesterFilter("7")
        assertTrue(visibleTitles().isEmpty())

        viewModel.setIncludePde(true)
        assertEquals(listOf("Εκπαιδευτική Ψυχολογία"), visibleTitles())
        viewModel.setIncludePde(false)
        assertTrue(visibleTitles().isEmpty())
    }

    @Test
    fun clearAllSchedules_wipesAllEventsAndSelectedSemester() = runTest {
        val repo = FakeEventRepository()
        val syncManager = ScheduleSyncManager(repo)
        val targetDate = LocalDate.of(2026, 11, 3)

        val sampleEvents = listOf(
            CalendarEvent(
                id = 1,
                title = "Ηλεκτρονική",
                semester = "1",
                startTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(9, 0)),
                endTime = LocalDateTime.of(targetDate, java.time.LocalTime.of(11, 0))
            )
        )
        repo.eventsFlow.value = sampleEvents

        val viewModel = CalendarViewModel(repo, syncManager)
        advanceUntilIdle()

        viewModel.setSemesterFilter("1")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.allEvents.size)
        assertEquals("1", viewModel.uiState.value.selectedSemester)

        viewModel.clearAllSchedules()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.allEvents.isEmpty())
        assertTrue(viewModel.uiState.value.monthEvents.isEmpty())
        assertTrue(viewModel.uiState.value.selectedDateEvents.isEmpty())
        assertEquals(null, viewModel.uiState.value.selectedSemester)
    }
}
