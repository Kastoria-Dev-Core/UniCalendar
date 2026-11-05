package org.kastoriadev.unicalendar.data.preferences

import android.content.Context
import android.content.SharedPreferences

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

    companion object {
        private const val PREFS_NAME = "unicalendar_user_prefs"
        private const val KEY_SELECTED_SEMESTER = "key_selected_semester"
        private const val KEY_INCLUDE_PDE = "key_include_pde"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "key_has_completed_onboarding"
    }
}
