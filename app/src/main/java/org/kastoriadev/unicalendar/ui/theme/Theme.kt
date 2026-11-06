package org.kastoriadev.unicalendar.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.kastoriadev.unicalendar.domain.model.EventType

// Every surface role is set explicitly: unset roles fall back to Material's lavender baseline,
// which used to tint dialogs, sheets, menus and snackbars purple.
private val DarkColorScheme = darkColorScheme(
    primary = BlueSecondary,
    onPrimary = SurfaceLight,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = BluePrimary,
    onSecondary = SurfaceLight,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = Color.Transparent,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = BackgroundDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = SurfaceLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = InversePrimaryLight,
    secondary = BlueSecondary,
    onSecondary = SurfaceLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = SurfaceLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceTint = Color.Transparent,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceBright = SurfaceLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

/** Category colours tuned for the active light/dark theme. */
@Immutable
data class EventColors(
    val lecture: Color,
    val lab: Color,
    val exam: Color,
    val deadline: Color,
    val general: Color
) {
    fun forType(type: EventType): Color = when (type) {
        EventType.LECTURE -> lecture
        EventType.LAB -> lab
        EventType.EXAM -> exam
        EventType.DEADLINE -> deadline
        EventType.GENERAL -> general
    }
}

private val LightEventColors = EventColors(ColorLecture, ColorLab, ColorExam, ColorDeadline, ColorGeneral)
private val DarkEventColors = EventColors(ColorLectureDark, ColorLabDark, ColorExamDark, ColorDeadlineDark, ColorGeneralDark)

val LocalEventColors = staticCompositionLocalOf { LightEventColors }

@Composable
fun UniCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalEventColors provides if (darkTheme) DarkEventColors else LightEventColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
