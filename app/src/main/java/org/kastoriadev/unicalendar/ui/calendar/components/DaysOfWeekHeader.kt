package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.ui.common.uppercaseFor
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun DaysOfWeekHeader(
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val labels = remember(locale) {
        DayOfWeek.entries.map { it.getDisplayName(TextStyle.SHORT, locale).uppercaseFor(locale) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isWeekend = index >= 5
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isWeekend) 0.6f else 1f)
            )
        }
    }
}
