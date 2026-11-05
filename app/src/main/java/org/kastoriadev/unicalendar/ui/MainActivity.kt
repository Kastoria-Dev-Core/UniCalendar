package org.kastoriadev.unicalendar.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import org.kastoriadev.unicalendar.UniCalendarApp
import org.kastoriadev.unicalendar.domain.model.ThemeMode
import org.kastoriadev.unicalendar.ui.calendar.CalendarScreen
import org.kastoriadev.unicalendar.ui.calendar.CalendarViewModel
import org.kastoriadev.unicalendar.ui.settings.SettingsScreen
import org.kastoriadev.unicalendar.ui.theme.UniCalendarTheme
import org.kastoriadev.unicalendar.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private val viewModel: CalendarViewModel by viewModels {
        val app = application as UniCalendarApp
        CalendarViewModel.Factory(
            repository = app.eventRepository,
            syncManager = app.syncManager,
            userPreferences = app.userPreferences
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current
            val localizedContext = remember(uiState.appLanguage, context) {
                LocaleHelper.applyLanguage(context, uiState.appLanguage)
            }

            val isDarkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                UniCalendarTheme(darkTheme = isDarkTheme) {
                    if (uiState.showSettingsScreen) {
                        BackHandler {
                            viewModel.dismissSettings()
                        }
                        SettingsScreen(
                            themeMode = uiState.themeMode,
                            displayLayoutMode = uiState.displayLayoutMode,
                            appLanguage = uiState.appLanguage,
                            onBack = viewModel::dismissSettings,
                            onThemeModeChange = viewModel::setThemeMode,
                            onDisplayLayoutModeChange = viewModel::setDisplayLayoutMode,
                            onLanguageChange = viewModel::setAppLanguage,
                            onClearAllSchedules = viewModel::clearAllSchedules
                        )
                    } else {
                        CalendarScreen(
                            uiState = uiState,
                            onViewModeChange = viewModel::setViewMode,
                            onSemesterFilterChange = viewModel::setSemesterFilter,
                            onOpenSettings = viewModel::openSettings,
                            onDismissSettings = viewModel::dismissSettings,
                            onThemeModeChange = viewModel::setThemeMode,
                            onLanguageChange = viewModel::setAppLanguage,
                            onDisplayLayoutModeChange = viewModel::setDisplayLayoutMode,
                            onDismissSemesterPrompt = viewModel::dismissSemesterPrompt,
                            onSemesterConfirmed = viewModel::onSemesterConfirmed,
                            onDateSelected = viewModel::onDateSelected,
                            onDismissDayDetailsSheet = viewModel::dismissDayDetailsSheet,
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
                            onSyncFromGitHub = viewModel::syncFromGitHub,
                            onFileSelected = ::handleFileSelected,
                            onDismissSyncMessage = viewModel::dismissSyncMessage
                        )
                    }
                }
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
