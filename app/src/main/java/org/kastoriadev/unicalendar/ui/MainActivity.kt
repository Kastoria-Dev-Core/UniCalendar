package org.kastoriadev.unicalendar.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
                    onViewModeChange = viewModel::setViewMode,
                    onSemesterFilterChange = viewModel::setSemesterFilter,
                    onDateSelected = viewModel::onDateSelected,
                    onPreviousMonth = viewModel::onPreviousMonth,
                    onNextMonth = viewModel::onNextMonth,
                    onPreviousWeek = viewModel::onPreviousWeek,
                    onNextWeek = viewModel::onNextWeek,
                    onTodayClick = viewModel::onTodaySelected,
                    onSetAddDialogVisible = viewModel::setAddDialogVisible,
                    onSetSyncDialogVisible = viewModel::setSyncDialogVisible,
                    onEditNoteClick = viewModel::onEditNoteClick,
                    onDismissNoteDialog = viewModel::onDismissNoteDialog,
                    onSaveNotes = viewModel::onSaveNotes,
                    onAddEvent = viewModel::addEvent,
                    onDeleteEvent = viewModel::deleteEvent,
                    onSyncSchedule = viewModel::syncSchedule,
                    onFileSelected = ::handleFileSelected,
                    onDismissSyncMessage = viewModel::dismissSyncMessage
                )
            }
        }
    }

    private fun handleFileSelected(uri: Uri, targetSemester: String?) {
        try {
            val fileName = queryFileName(uri)
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                viewModel.syncFromStream(inputStream, fileName, targetSemester)
            }
        } catch (e: Exception) {
            // Error handled by ViewModel
        }
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = cursor.getString(index)
                    }
                }
            }
        }
        return name ?: uri.lastPathSegment
    }
}
