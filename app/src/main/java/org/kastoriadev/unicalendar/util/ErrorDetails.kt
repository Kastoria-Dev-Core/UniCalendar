package org.kastoriadev.unicalendar.util

import android.os.Build
import org.kastoriadev.unicalendar.BuildConfig

/** The stack trace, causes included, cut short so it still fits in a dialog. */
fun Throwable.toErrorDetails(maxLength: Int = 6_000): String {
    val trace = stackTraceToString().trim()
    return if (trace.length <= maxLength) trace else trace.take(maxLength).trimEnd() + "\n…"
}

/** App version and device, the first thing needed to make sense of an error report. */
fun deviceSummary(): String {
    val variant = if (BuildConfig.DEBUG) ", debug" else ""
    return "UniCalendar ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}$variant) · " +
        "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}"
}
