package org.kastoriadev.unicalendar.ui.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import org.kastoriadev.unicalendar.ui.calendar.components.AddEventDialog
import org.kastoriadev.unicalendar.ui.calendar.components.DaysOfWeekHeader
import org.kastoriadev.unicalendar.ui.calendar.components.EventList
import org.kastoriadev.unicalendar.ui.calendar.components.MonthGrid
import org.kastoriadev.unicalendar.ui.calendar.components.MonthHeader
import org.kastoriadev.unicalendar.ui.calendar.components.SyncScheduleDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onSetAddDialogVisible: (Boolean) -> Unit,
    onSetSyncDialogVisible: (Boolean) -> Unit,
    onAddEvent: (
        title: String,
        description: String,
        location: String,
        startTime: java.time.LocalDateTime,
        endTime: java.time.LocalDateTime,
        type: org.kastoriadev.unicalendar.domain.model.EventType
    ) -> Unit,
    onDeleteEvent: (org.kastoriadev.unicalendar.domain.model.CalendarEvent) -> Unit,
    onSyncSchedule: (String) -> Unit,
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
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
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
                                contentDescription = "Sync university schedule"
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
                    .padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            EventList(
                selectedDate = uiState.selectedDate,
                events = uiState.selectedDateEvents,
                onDeleteEvent = onDeleteEvent,
                modifier = Modifier.weight(1f)
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
                onSyncConfirm = onSyncSchedule
            )
        }
    }
}
