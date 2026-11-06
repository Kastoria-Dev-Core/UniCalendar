package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.EventType
import org.kastoriadev.unicalendar.ui.theme.LocalEventColors
import java.time.LocalDate

@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    events: List<CalendarEvent>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false
) {
    val textColor = when {
        isSelected && !isFullScreen -> MaterialTheme.colorScheme.onPrimary
        isSelected && isFullScreen -> MaterialTheme.colorScheme.onPrimaryContainer
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val shape = if (isFullScreen) RoundedCornerShape(8.dp) else CircleShape
    val cellModifier = if (isFullScreen) {
        val cellBg = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isCurrentMonth -> MaterialTheme.colorScheme.surface
            else -> Color.Transparent
        }
        val cellBorderModifier = when {
            isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
            isCurrentMonth -> Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            else -> Modifier
        }

        modifier
            .fillMaxSize()
            .padding(1.5.dp)
            .clip(shape)
            .background(cellBg)
            .then(cellBorderModifier)
            .clickable { onDateClick(date) }
    } else {
        modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(shape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else Modifier
            )
            .clickable { onDateClick(date) }
    }

    Box(
        modifier = cellModifier,
        contentAlignment = if (isFullScreen) Alignment.TopCenter else Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isFullScreen) Arrangement.Top else Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )

            if (events.isNotEmpty()) {
                if (isFullScreen) {
                    // Full screen: display mini course label pills
                    events.take(2).forEach { event ->
                        val pillColor = getEventTypeColor(event.type)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(pillColor.copy(alpha = 0.12f))
                                .border(1.dp, pillColor.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = pillColor
                            )
                        }
                    }
                    if (events.size > 2) {
                        Text(
                            text = "+${events.size - 2}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    // Split screen: display indicator dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        val distinctTypes = events.map { it.type }.distinct().take(3)
                        for (type in distinctTypes) {
                            val dotColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                getEventTypeColor(type)
                            }
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Category colour for the current light/dark theme. */
@Composable
fun getEventTypeColor(type: EventType): Color = LocalEventColors.current.forType(type)
