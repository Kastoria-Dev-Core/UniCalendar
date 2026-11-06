package org.kastoriadev.unicalendar.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.domain.model.EventType
import org.kastoriadev.unicalendar.ui.theme.LocalEventColors
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "09:00 – 11:00" */
fun formatTimeRange(start: LocalDateTime, end: LocalDateTime): String =
    "${start.format(TIME_FORMATTER)} – ${end.format(TIME_FORMATTER)}"

/** Category colour for the current light/dark theme. */
@Composable
fun eventTypeColor(type: EventType): Color = LocalEventColors.current.forType(type)

/**
 * Upper-case text the way the locale writes it. Greek capitals carry no accents
 * ("ΤΡΙ", not "ΤΡΊ"), which plain [String.uppercase] gets wrong.
 */
fun String.uppercaseFor(locale: Locale): String {
    val upper = uppercase(locale)
    if (locale.language != "el") return upper
    val withoutTonos = Normalizer.normalize(upper, Normalizer.Form.NFD).replace("\u0301", "")
    return Normalizer.normalize(withoutTonos, Normalizer.Form.NFC)
}

/** Small, non-interactive category label ("Lecture", "Exam"...). */
@Composable
fun EventTypeTag(type: EventType, modifier: Modifier = Modifier) {
    val color = eventTypeColor(type)
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = stringResource(type.displayNameRes),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), shape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/**
 * Single-line text that shrinks (down to [minFontSize]) instead of wrapping or clipping,
 * e.g. long Greek month names next to the navigation buttons on narrow phones.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    minFontSize: TextUnit = 13.sp
) {
    var scaledStyle by remember(text, style) { mutableStateOf(style) }
    var ready by remember(text, style) { mutableStateOf(false) }
    Text(
        text = text,
        style = scaledStyle,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        overflow = if (ready) TextOverflow.Ellipsis else TextOverflow.Clip,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth && scaledStyle.fontSize > minFontSize) {
                scaledStyle = scaledStyle.copy(fontSize = scaledStyle.fontSize * 0.92f)
            } else {
                ready = true
            }
        }
    )
}

/**
 * Empty state for a day's list. Before any timetable exists it explains what to do and
 * offers to sync, instead of only saying the day is empty.
 */
@Composable
fun EmptyDayState(
    hasAnyEvents: Boolean,
    emptyDayText: String,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasAnyEvents) Icons.Outlined.EventAvailable else Icons.Outlined.CloudDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasAnyEvents) emptyDayText else stringResource(R.string.empty_no_timetable_title),
            style = if (hasAnyEvents) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
            fontWeight = if (hasAnyEvents) FontWeight.Normal else FontWeight.SemiBold,
            color = if (hasAnyEvents) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (!hasAnyEvents) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.empty_no_timetable_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(onClick = onSyncClick) {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.btn_sync_now))
            }
        }
    }
}
