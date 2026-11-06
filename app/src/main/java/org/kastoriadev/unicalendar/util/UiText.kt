package org.kastoriadev.unicalendar.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Text that is resolved against the (possibly in-app localized) context when shown.
 * Format arguments may themselves be [UiText], e.g. "Imported %1$s and %2$s" with plurals.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    class PluralResource(
        @PluralsRes val resId: Int,
        val quantity: Int,
        vararg val args: Any
    ) : UiText()

    @Composable
    fun asString(): String = asString(LocalContext.current)

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(resId, *resolve(context, args))
            is PluralResource -> context.resources.getQuantityString(resId, quantity, *resolve(context, args))
        }
    }

    private fun resolve(context: Context, args: Array<out Any>): Array<Any> =
        Array(args.size) { index ->
            when (val arg = args[index]) {
                is UiText -> arg.asString(context)
                else -> arg
            }
        }
}
