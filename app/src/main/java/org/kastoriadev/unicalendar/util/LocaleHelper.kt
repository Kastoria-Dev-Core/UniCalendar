package org.kastoriadev.unicalendar.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import org.kastoriadev.unicalendar.domain.model.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun getLocale(appLanguage: AppLanguage): Locale {
        return when (appLanguage) {
            AppLanguage.SYSTEM -> Locale.getDefault()
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.GREEK -> Locale("el", "GR")
        }
    }

    /**
     * Returns [context] with its resources switched to [appLanguage].
     *
     * Only the resources are replaced. The result still wraps [context], so code that finds the
     * Activity through LocalContext (such as the file picker's result launcher) keeps working; a
     * bare configuration context would hide the Activity and make those callers throw.
     */
    fun applyLanguage(context: Context, appLanguage: AppLanguage): Context {
        val locale = getLocale(appLanguage)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return LocalizedContext(context, context.createConfigurationContext(config).resources)
    }

    private class LocalizedContext(base: Context, private val localized: Resources) : ContextWrapper(base) {
        override fun getResources(): Resources = localized
    }
}
