package org.kastoriadev.unicalendar.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.kastoriadev.unicalendar.UniCalendarApp
import org.kastoriadev.unicalendar.ui.calendar.CalendarScreen
import org.kastoriadev.unicalendar.ui.calendar.CalendarViewModel
import org.kastoriadev.unicalendar.ui.theme.UniCalendarTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CalendarViewModel by viewModels {
        val app = application as UniCalendarApp
        CalendarViewModel.Factory(app.eventRepository, app.syncManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniCalendarTheme {
                val uiState by viewModel.uiState.collectAsState()

                CalendarScreen(
                    uiState = uiState,
                    onDateSelected = viewModel::onDateSelected,
                    onPreviousMonth = viewModel::onPreviousMonth,
                    onNextMonth = viewModel::onNextMonth,
                    onTodayClick = viewModel::onTodaySelected,
                    onSetAddDialogVisible = viewModel::setAddDialogVisible,
                    onSetSyncDialogVisible = viewModel::setSyncDialogVisible,
                    onAddEvent = viewModel::addEvent,
                    onDeleteEvent = viewModel::deleteEvent,
                    onSyncSchedule = viewModel::syncSchedule,
                    onDismissSyncMessage = viewModel::dismissSyncMessage
                )
            }
        }
    }
}
