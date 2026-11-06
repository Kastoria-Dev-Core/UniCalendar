package org.kastoriadev.unicalendar.data.preferences

import android.content.Context
import android.content.SharedPreferences
import org.kastoriadev.unicalendar.domain.model.AppLanguage
import org.kastoriadev.unicalendar.domain.model.DisplayLayoutMode
import org.kastoriadev.unicalendar.domain.model.ThemeMode

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selectedSemester: String?
        get() = prefs.getString(KEY_SELECTED_SEMESTER, null)
        set(value) {
            prefs.edit().putString(KEY_SELECTED_SEMESTER, value).apply()
        }

    var includePde: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_PDE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_INCLUDE_PDE, value).apply()
        }

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, value).apply()
        }

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
            return try {
                val mode = ThemeMode.valueOf(name)
                if (mode == ThemeMode.SYSTEM) ThemeMode.LIGHT else mode
            } catch (e: Exception) {
                ThemeMode.LIGHT
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    var displayLayoutMode: DisplayLayoutMode
        get() {
            val name = prefs.getString(KEY_DISPLAY_LAYOUT_MODE, DisplayLayoutMode.SPLIT.name) ?: DisplayLayoutMode.SPLIT.name
            return try {
                DisplayLayoutMode.valueOf(name)
            } catch (e: Exception) {
                DisplayLayoutMode.SPLIT
            }
        }
        set(value) {
            prefs.edit().putString(KEY_DISPLAY_LAYOUT_MODE, value.name).apply()
        }

    var appLanguage: AppLanguage
        get() {
            val name = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.GREEK.name) ?: AppLanguage.GREEK.name
            return try {
                val lang = AppLanguage.valueOf(name)
                if (lang == AppLanguage.SYSTEM) AppLanguage.GREEK else lang
            } catch (e: Exception) {
                AppLanguage.GREEK
            }
        }
        set(value) {
            prefs.edit().putString(KEY_APP_LANGUAGE, value.name).apply()
        }

    /** Epoch millis of the last successful university sync, or 0 when never synced. */
    var lastSyncEpochMillis: Long
        get() = prefs.getLong(KEY_LAST_SYNC_MILLIS, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_SYNC_MILLIS, value).apply()
        }

    /** Name of the timetable document imported by the last sync. */
    var lastSyncLabel: String?
        get() = prefs.getString(KEY_LAST_SYNC_LABEL, null)
        set(value) {
            prefs.edit().putString(KEY_LAST_SYNC_LABEL, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "unicalendar_user_prefs"
        private const val KEY_SELECTED_SEMESTER = "key_selected_semester"
        private const val KEY_INCLUDE_PDE = "key_include_pde"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "key_has_completed_onboarding"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DISPLAY_LAYOUT_MODE = "key_display_layout_mode"
        private const val KEY_APP_LANGUAGE = "key_app_language"
        private const val KEY_LAST_SYNC_MILLIS = "key_last_sync_millis"
        private const val KEY_LAST_SYNC_LABEL = "key_last_sync_label"
    }
}
