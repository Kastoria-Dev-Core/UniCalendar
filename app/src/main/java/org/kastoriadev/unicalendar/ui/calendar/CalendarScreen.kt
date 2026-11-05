package org.kastoriadev.unicalendar.ui.calendar

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventType
import org.kastoriadev.unicalendar.ui.calendar.components.AddEventDialog
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
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onSemesterFilterChange: (String?) -> Unit,
    onOpenSemesterPrompt: () -> Unit,
    onDismissSemesterPrompt: () -> Unit,
    onSemesterConfirmed: (semester: String, includePde: Boolean) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
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
    onSyncSchedule: (url: String, targetSemester: String?) -> Unit,
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
                    Column(
                        modifier = Modifier.clickable { onOpenSemesterPrompt() }
                    ) {
                        Text(
                            text = "UniCalendar",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        val semLabel = when (uiState.selectedSemester) {
                            null, "ALL" -> "All Semesters"
                            "PDE" -> "ΠΔΕ (Pedagogical Module)"
                            else -> "Semester ${uiState.selectedSemester}" + if (uiState.includePde) " (+ ΠΔΕ)" else ""
                        }
                        Text(
                            text = "Active: $semLabel ▾",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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

                    // Semester Selector Dialog Trigger
                    IconButton(onClick = onOpenSemesterPrompt) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Select active semester"
                        )
                    }

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

            when (uiState.viewMode) {
                CalendarViewMode.MONTH -> {
                    MonthHeader(
                        currentMonth = uiState.currentMonth,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onTodayClick = onTodayClick
                    )

                    DaysOfWeekHeader()

                    MonthGrid(
                        currentMonth = uiState.currentMonth,
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

                    EventList(
                        selectedDate = uiState.selectedDate,
                        events = uiState.selectedDateEvents,
                        onEditNoteClick = onEditNoteClick,
                        onDeleteEvent = onDeleteEvent,
                        modifier = Modifier.weight(1f)
                    )
                }

                CalendarViewMode.WEEK -> {
                    WeekHeader(
                        weekStart = uiState.currentWeekStart,
                        onPreviousWeek = onPreviousWeek,
                        onNextWeek = onNextWeek,
                        onTodayClick = onTodayClick
                    )

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

        if (uiState.showSemesterPrompt) {
            SemesterSelectPromptDialog(
                currentSemester = uiState.selectedSemester,
                currentIncludePde = uiState.includePde,
                canDismiss = uiState.canDismissSemesterPrompt,
                onDismiss = onDismissSemesterPrompt,
                onSemesterConfirmed = onSemesterConfirmed
            )
        }

        if (uiState.showAddDialog) {
            AddEventDialog(
                selectedDate = uiState.selectedDate,
                onDismiss = { onSetAddDialogVisible(false) },
                onConfirm = onAddEvent
            )
        }

        if (uiState.showSyncDialog) {
            SyncScheduleDialog(
                onDismiss = { onSetSyncDialogVisible(false) },
                onSyncConfirm = onSyncSchedule,
                onFileSelected = onFileSelected
            )
        }

        uiState.selectedEventForNotes?.let { event ->
            EditNoteDialog(
                event = event,
                onDismiss = onDismissNoteDialog,
                onSaveNotes = onSaveNotes
            )
        }
    }
}
