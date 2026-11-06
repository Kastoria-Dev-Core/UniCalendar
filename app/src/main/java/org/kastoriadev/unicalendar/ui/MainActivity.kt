package org.kastoriadev.unicalendar.ui

import android.content.ContentResolver
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import org.kastoriadev.unicalendar.UniCalendarApp
import org.kastoriadev.unicalendar.domain.model.ThemeMode
import org.kastoriadev.unicalendar.ui.calendar.CalendarScreen
import org.kastoriadev.unicalendar.ui.calendar.CalendarViewModel
import org.kastoriadev.unicalendar.ui.common.StartupLoadingScreen
import org.kastoriadev.unicalendar.ui.settings.SettingsScreen
import org.kastoriadev.unicalendar.ui.theme.UniCalendarTheme
import org.kastoriadev.unicalendar.ui.theme.windowBackgroundColor
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
        val startsDark = (application as UniCalendarApp).userPreferences.themeMode == ThemeMode.DARK
        applyWindowTheme(startsDark)

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

            // Status/navigation bar icons follow the app's theme, not the system one.
            DisposableEffect(isDarkTheme) {
                applyWindowTheme(isDarkTheme)
                onDispose {}
            }

            LaunchedEffect(Unit) {
                if (uiState.showStartupSplash) {
                    delay(STARTUP_SPLASH_MILLIS)
                    viewModel.onStartupSplashFinished()
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                UniCalendarTheme(darkTheme = isDarkTheme) {
                    AnimatedContent(
                        targetState = uiState.showStartupSplash,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(280))
                        },
                        label = "StartupSplashTransition"
                    ) { showSplash ->
                        if (showSplash) {
                            StartupLoadingScreen()
                        } else {
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
                                    onSyncFromUniversity = viewModel::syncFromUniversity,
                                    onFileSelected = ::importFile,
                                    onDismissSyncMessage = viewModel::dismissSyncMessage
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyWindowTheme(dark: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = if (dark) {
                SystemBarStyle.dark(Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            },
            navigationBarStyle = if (dark) {
                SystemBarStyle.dark(DARK_NAVIGATION_SCRIM)
            } else {
                SystemBarStyle.light(LIGHT_NAVIGATION_SCRIM, DARK_NAVIGATION_SCRIM)
            }
        )
        // Shown before Compose draws its first frame; avoids a white flash in dark theme.
        window.setBackgroundDrawable(ColorDrawable(windowBackgroundColor(dark).toArgb()))
    }

    /** Reads the picked file on a background thread; only the application's resolver is captured. */
    private fun importFile(uri: Uri, targetSemester: String?) {
        val resolver = applicationContext.contentResolver
        viewModel.importFile(
            openStream = { resolver.openInputStream(uri) },
            fileName = { queryFileName(resolver, uri) },
            targetSemester = targetSemester
        )
    }

    private companion object {
        const val STARTUP_SPLASH_MILLIS = 850L

        // Same scrims androidx.activity uses for three-button navigation bars.
        val LIGHT_NAVIGATION_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_NAVIGATION_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

        fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) return cursor.getString(index)
                    }
                }
            }
            return uri.lastPathSegment
        }
    }
}
