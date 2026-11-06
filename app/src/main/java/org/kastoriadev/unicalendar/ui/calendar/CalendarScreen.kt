package org.kastoriadev.unicalendar.ui.calendar

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.DisplayLayoutMode
import org.kastoriadev.unicalendar.domain.model.EventType
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
import org.kastoriadev.unicalendar.ui.common.RotatingSyncLoading
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onSemesterFilterChange: (String?) -> Unit,
    onOpenSettings: () -> Unit,
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
    onSyncFromUniversity: (targetSemester: String?) -> Unit,
    onFileSelected: (uri: Uri, targetSemester: String?) -> Unit,
    onMessageAction: (MessageAction) -> Unit,
    onUserMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val openSyncDialog = { onSetSyncDialogVisible(true) }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message.text.asString(context),
            actionLabel = message.action?.let { context.getString(R.string.action_undo) },
            duration = if (message.action != null) SnackbarDuration.Long else SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            message.action?.let(onMessageAction)
        }
        onUserMessageShown()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { AppBarTitle() },
                actions = {
                    ViewModeToggle(
                        viewMode = uiState.viewMode,
                        onViewModeChange = onViewModeChange,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    if (uiState.isSyncing) {
                        RotatingSyncLoading(
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp)
                        )
                    } else {
                        IconButton(onClick = openSyncDialog) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = stringResource(R.string.action_sync)
                            )
                        }
                    }

                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings)
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
                    contentDescription = stringResource(R.string.action_add_event)
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isFullScreen = uiState.displayLayoutMode == DisplayLayoutMode.FULLSCREEN
            // Wide landscape screens put the calendar and the day's details side by side.
            val sideBySide = !isFullScreen && maxWidth > maxHeight && maxWidth >= 600.dp

            Column(modifier = Modifier.fillMaxSize()) {
                SemesterFilterRow(
                    selectedSemester = uiState.selectedSemester,
                    onSemesterSelected = onSemesterFilterChange
                )

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                val calendarPane: @Composable ColumnScope.(fillHeight: Boolean) -> Unit = { fillHeight ->
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
                                onDateClick = onDateSelected,
                                isFullScreen = isFullScreen,
                                modifier = if (fillHeight) Modifier.weight(1f) else Modifier
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
                                onDateClick = onDateSelected,
                                // In side-by-side mode the day columns stretch to the pane height.
                                isFullScreen = isFullScreen || fillHeight,
                                modifier = if (fillHeight) Modifier.fillMaxWidth().weight(1f) else Modifier
                            )
                        }
                    }
                }

                val detailsPane: @Composable (Modifier) -> Unit = { paneModifier ->
                    when (uiState.viewMode) {
                        CalendarViewMode.MONTH -> EventList(
                            selectedDate = uiState.selectedDate,
                            events = uiState.selectedDateEvents,
                            hasAnyEvents = uiState.hasAnyEvents,
                            onEditNoteClick = onEditNoteClick,
                            onDeleteEvent = onDeleteEvent,
                            onSyncClick = openSyncDialog,
                            modifier = paneModifier
                        )
                        CalendarViewMode.WEEK -> LessonNotesSection(
                            selectedDate = uiState.selectedDate,
                            events = uiState.selectedDateEvents,
                            hasAnyEvents = uiState.hasAnyEvents,
                            onEditNoteClick = onEditNoteClick,
                            onDeleteEvent = onDeleteEvent,
                            onSyncClick = openSyncDialog,
                            modifier = paneModifier
                        )
                    }
                }

                when {
                    isFullScreen -> calendarPane(true)
                    sideBySide -> Row(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            calendarPane(true)
                        }
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        detailsPane(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    else -> {
                        calendarPane(false)
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        detailsPane(Modifier.weight(1f))
                    }
                }
            }
        }

        // Day details (tapping a day in full-screen mode)
        if (uiState.showDayDetailsSheet && uiState.displayLayoutMode == DisplayLayoutMode.FULLSCREEN) {
            DayDetailsSheet(
                selectedDate = uiState.selectedDate,
                events = uiState.selectedDateEvents,
                hasAnyEvents = uiState.hasAnyEvents,
                onEditNoteClick = onEditNoteClick,
                onDeleteEvent = onDeleteEvent,
                onSyncClick = openSyncDialog,
                onDismiss = onDismissDayDetailsSheet
            )
        }

        // Semester prompt (first launch)
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
                lastSyncedAt = uiState.lastSyncedAt,
                lastSyncLabel = uiState.lastSyncLabel,
                onDismiss = { onSetSyncDialogVisible(false) },
                onSyncFromUniversity = onSyncFromUniversity,
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

/** App logo with the name; the name is dropped when the bar is too narrow to fit it. */
@Composable
private fun AppBarTitle() {
    BoxWithConstraints {
        val showName = maxWidth >= 170.dp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = if (showName) null else stringResource(R.string.app_name),
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            if (showName) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Compact week/month switch. Icon-only so it fits next to the actions in any language. */
@Composable
private fun ViewModeToggle(
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        Triple(CalendarViewMode.WEEK, Icons.Default.CalendarViewWeek, stringResource(R.string.view_week)),
        Triple(CalendarViewMode.MONTH, Icons.Default.CalendarMonth, stringResource(R.string.view_month))
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier.height(36.dp)) {
        options.forEachIndexed { index, (mode, icon, label) ->
            ViewModeSegment(
                index = index,
                count = options.size,
                selected = viewMode == mode,
                icon = icon,
                label = label,
                onClick = { onViewModeChange(mode) }
            )
        }
    }
}

@Composable
private fun androidx.compose.material3.SingleChoiceSegmentedButtonRowScope.ViewModeSegment(
    index: Int,
    count: Int,
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    SegmentedButton(
        shape = SegmentedButtonDefaults.itemShape(index = index, count = count),
        onClick = onClick,
        selected = selected,
        colors = SegmentedButtonDefaults.colors(
            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            inactiveContainerColor = MaterialTheme.colorScheme.surface,
            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = SegmentedButtonDefaults.borderStroke(color = MaterialTheme.colorScheme.outlineVariant),
        icon = {},
        modifier = Modifier.width(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp)
        )
    }
}
