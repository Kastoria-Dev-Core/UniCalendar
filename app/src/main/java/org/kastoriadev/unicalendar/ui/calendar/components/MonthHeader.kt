package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.ui.common.AutoSizeText
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

@Composable
fun MonthHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val monthTitle = remember(currentMonth, locale) { monthTitle(currentMonth, locale) }

    CalendarNavigationHeader(
        title = monthTitle,
        titleStyle = MaterialTheme.typography.headlineMedium,
        previousDescription = stringResource(R.string.action_previous_month),
        nextDescription = stringResource(R.string.action_next_month),
        onPrevious = onPreviousMonth,
        onNext = onNextMonth,
        onTodayClick = onTodayClick,
        modifier = modifier
    )
}

/**
 * "Νοέμβριος 2026": the stand-alone (nominative) month name. The "MMMM" pattern yields the
 * genitive form in Greek ("Νοεμβρίου"), which is only correct next to a day number.
 */
internal fun monthTitle(month: YearMonth, locale: Locale): String {
    val standalone = month.month.getDisplayName(DateTextStyle.FULL_STANDALONE, locale)
    val name = if (standalone.any { it.isLetter() }) standalone else month.month.getDisplayName(DateTextStyle.FULL, locale)
    return "${name.replaceFirstChar { it.titlecase(locale) }} ${month.year}"
}

/** Title + "Today" + previous/next buttons, shared by the month and week headers. */
@Composable
internal fun CalendarNavigationHeader(
    title: String,
    titleStyle: TextStyle,
    previousDescription: String,
    nextDescription: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AutoSizeText(
            text = title,
            style = titleStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            FilledTonalButton(
                onClick = onTodayClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(text = stringResource(R.string.action_today), style = MaterialTheme.typography.labelLarge)
            }

            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = previousDescription
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = nextDescription
                )
            }
        }
    }
}
