package org.kastoriadev.unicalendar.util

import android.content.Context
import android.content.res.Configuration
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

    fun applyLanguage(context: Context, appLanguage: AppLanguage): Context {
        val locale = getLocale(appLanguage)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }
}
