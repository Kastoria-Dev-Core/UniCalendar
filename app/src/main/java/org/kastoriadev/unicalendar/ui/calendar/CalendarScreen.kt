package org.kastoriadev.unicalendar.ui.calendar

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.DisplayLayoutMode
import org.kastoriadev.unicalendar.domain.model.EventType
import org.kastoriadev.unicalendar.domain.model.ThemeMode
import org.kastoriadev.unicalendar.ui.calendar.components.AddEventDialog
import org.kastoriadev.unicalendar.ui.calendar.components.DayDetailsSheet
import org.kastoriadev.unicalendar.ui.calendar.components.DaysOfWeekHeader
import org.kastoriadev.unicalendar.ui.calendar.components.EditNoteDialog
import org.kastoriadev.unicalendar.ui.calendar.components.EventList
import org.kastoriadev.unicalendar.ui.calendar.components.LessonNotesSection
import org.kastoriadev.unicalendar.ui.calendar.components.MonthGrid
import org.kastoriadev.unicalendar.ui.calendar.components.MonthHeader
import org.kastoriadev.unicalendar.ui.calendar.components.SemesterFilterRow
import org.kastoriadev.unicalendar.ui.calendar.components.SemesterSelectPromptDialog
import org.kastoriadev.unicalendar.ui.calendar.components.SyncScheduleDialog
import org.kastoriadev.unicalendar.ui.calendar.components.WeekGrid
import org.kastoriadev.unicalendar.ui.calendar.components.WeekHeader
import org.kastoriadev.unicalendar.ui.settings.SettingsDialog
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onSemesterFilterChange: (String?) -> Unit,
    onOpenSettings: () -> Unit,
    onDismissSettings: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDisplayLayoutModeChange: (DisplayLayoutMode) -> Unit,
    onDismissSemesterPrompt: () -> Unit,
    onSemesterConfirmed: (semester: String, includePde: Boolean) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onDismissDayDetailsSheet: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onTodayClick: () -> Unit,
    onSetAddDialogVisible: (Boolean) -> Unit,
    onSetSyncDialogVisible: (Boolean) -> Unit,
    onEditNoteClick: (CalendarEvent) -> Unit,
    onDismissNoteDialog: () -> Unit,
    onSaveNotes: (Long, String) -> Unit,
    onAddEvent: (
        title: String,
        description: String,
        location: String,
        semester: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        type: EventType
    ) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onSyncFromGitHub: (targetSemester: String?) -> Unit,
    onFileSelected: (uri: Uri, targetSemester: String?) -> Unit,
    onDismissSyncMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.syncMessage) {
        uiState.syncMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onDismissSyncMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "UniCalendar",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    // View Switcher (Week / Month)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .height(36.dp)
                            .padding(end = 4.dp)
                    ) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { onViewModeChange(CalendarViewMode.WEEK) },
                            selected = uiState.viewMode == CalendarViewMode.WEEK,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarViewWeek,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(text = "Week", style = MaterialTheme.typography.labelSmall)
                        }

                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { onViewModeChange(CalendarViewMode.MONTH) },
                            selected = uiState.viewMode == CalendarViewMode.MONTH,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(text = "Month", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Sync Schedule Button
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { onSetSyncDialogVisible(true) }) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Sync schedule"
                            )
                        }
                    }

                    // Settings Gear Button
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onSetAddDialogVisible(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add event"
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Semester Filter Row (+ PDE)
            SemesterFilterRow(
                selectedSemester = uiState.selectedSemester,
                onSemesterSelected = onSemesterFilterChange
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            val isFullScreen = uiState.displayLayoutMode == DisplayLayoutMode.FULLSCREEN

            when (uiState.viewMode) {
                CalendarViewMode.MONTH -> {
                    MonthHeader(
                        currentMonth = uiState.currentMonth,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onTodayClick = onTodayClick
                    )

                    DaysOfWeekHeader()

                    if (isFullScreen) {
                        // Full Screen Calendar: takes full remaining space
                        MonthGrid(
                            currentMonth = uiState.currentMonth,
                            selectedDate = uiState.selectedDate,
                            eventsMap = uiState.monthEvents,
                            onDateClick = onDateSelected,
                            isFullScreen = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Split Screen: calendar on top half, details below
                        MonthGrid(
                            currentMonth = uiState.currentMonth,
                            selectedDate = uiState.selectedDate,
                            eventsMap = uiState.monthEvents,
                            onDateClick = onDateSelected,
                            isFullScreen = false
                        )

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        EventList(
                            selectedDate = uiState.selectedDate,
                            events = uiState.selectedDateEvents,
                            onEditNoteClick = onEditNoteClick,
                            onDeleteEvent = onDeleteEvent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                CalendarViewMode.WEEK -> {
                    WeekHeader(
                        weekStart = uiState.currentWeekStart,
                        onPreviousWeek = onPreviousWeek,
                        onNextWeek = onNextWeek,
                        onTodayClick = onTodayClick
                    )

                    if (isFullScreen) {
                        // Full Screen Week: schedule fills the screen
                        WeekGrid(
                            weekStart = uiState.currentWeekStart,
                            selectedDate = uiState.selectedDate,
                            eventsMap = uiState.monthEvents,
                            onDateClick = onDateSelected,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Split Screen: week schedule on top, lesson notes below
                        WeekGrid(
                            weekStart = uiState.currentWeekStart,
                            selectedDate = uiState.selectedDate,
                            eventsMap = uiState.monthEvents,
                            onDateClick = onDateSelected
                        )

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        LessonNotesSection(
                            selectedDate = uiState.selectedDate,
                            events = uiState.selectedDateEvents,
                            onEditNoteClick = onEditNoteClick,
                            onDeleteEvent = onDeleteEvent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Day Details Sheet (when tapping a day in Full Screen mode)
        if (uiState.showDayDetailsSheet && uiState.displayLayoutMode == DisplayLayoutMode.FULLSCREEN) {
            DayDetailsSheet(
                selectedDate = uiState.selectedDate,
                events = uiState.selectedDateEvents,
                onEditNoteClick = onEditNoteClick,
                onDeleteEvent = onDeleteEvent,
                onDismiss = onDismissDayDetailsSheet
            )
        }

        // Settings Dialog
        if (uiState.showSettingsDialog) {
            SettingsDialog(
                themeMode = uiState.themeMode,
                displayLayoutMode = uiState.displayLayoutMode,
                selectedSemester = uiState.selectedSemester,
                includePde = uiState.includePde,
                onThemeModeChange = onThemeModeChange,
                onDisplayLayoutModeChange = onDisplayLayoutModeChange,
                onSemesterChange = onSemesterConfirmed,
                onDismiss = onDismissSettings
            )
        }

        // Onboarding Semester Prompt Dialog (First launch)
        if (uiState.showSemesterPrompt) {
            SemesterSelectPromptDialog(
                currentSemester = uiState.selectedSemester,
                currentIncludePde = uiState.includePde,
                canDismiss = uiState.canDismissSemesterPrompt,
                onDismiss = onDismissSemesterPrompt,
                onSemesterConfirmed = onSemesterConfirmed
            )
        }

        // Add Event Dialog
        if (uiState.showAddDialog) {
            AddEventDialog(
                selectedDate = uiState.selectedDate,
                onDismiss = { onSetAddDialogVisible(false) },
                onConfirm = onAddEvent
            )
        }

        // Sync Dialog (GitHub Repo automated sync & Local docx/ics file import)
        if (uiState.showSyncDialog) {
            SyncScheduleDialog(
                onDismiss = { onSetSyncDialogVisible(false) },
                onSyncFromGitHub = onSyncFromGitHub,
                onFileSelected = onFileSelected
            )
        }

        // Edit Notes Dialog
        uiState.selectedEventForNotes?.let { event ->
            EditNoteDialog(
                event = event,
                onDismiss = onDismissNoteDialog,
                onSaveNotes = onSaveNotes
            )
        }
    }
}
