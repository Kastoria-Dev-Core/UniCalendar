package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.ui.common.eventTypeColor
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
    isFullScreen: Boolean = false,
    circleSize: Dp = 48.dp
) {
    if (isFullScreen) {
        FullScreenDayCell(date, isSelected, isToday, isCurrentMonth, events, onDateClick, modifier)
    } else {
        CompactDayCell(date, isSelected, isToday, isCurrentMonth, events, onDateClick, modifier, circleSize)
    }
}

@Composable
private fun CompactDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    events: List<CalendarEvent>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier,
    circleSize: Dp
) {
    val colors = MaterialTheme.colorScheme
    val textColor = when {
        isSelected -> colors.onPrimary
        !isCurrentMonth -> colors.onSurface.copy(alpha = 0.35f)
        isToday -> colors.primary
        else -> colors.onSurface
    }
    // Landscape can shrink cells below the size the number and dots normally need.
    val isSmall = circleSize < 40.dp

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(circleSize - 4.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.primary else Color.Transparent)
                .then(if (isToday && !isSelected) Modifier.border(1.5.dp, colors.primary, CircleShape) else Modifier)
                .clickable(role = Role.Button) { onDateClick(date) }
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = if (isSmall) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                maxLines = 1
            )

            // The dot row is always laid out so day numbers stay aligned across the week.
            val dotSize = if (isSmall) 3.dp else 4.dp
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = if (isSmall) 1.dp else 2.dp)
                    .height(dotSize)
            ) {
                events.map { it.type }.distinct().take(3).forEach { type ->
                    val dotColor = if (isSelected) colors.onPrimary else eventTypeColor(type)
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(dotColor.copy(alpha = if (isCurrentMonth || isSelected) 1f else 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    events: List<CalendarEvent>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val textColor = when {
        isSelected -> colors.onPrimaryContainer
        !isCurrentMonth -> colors.onSurface.copy(alpha = 0.35f)
        isToday -> colors.primary
        else -> colors.onSurface
    }
    val background = when {
        isSelected -> colors.primaryContainer
        isCurrentMonth -> colors.surface
        else -> Color.Transparent
    }
    val border = when {
        isToday -> Modifier.border(1.5.dp, colors.primary, shape)
        isCurrentMonth -> Modifier.border(1.dp, colors.outlineVariant, shape)
        else -> Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(1.5.dp)
            .clip(shape)
            .background(background)
            .then(border)
            .clickable(role = Role.Button) { onDateClick(date) }
            .padding(2.dp)
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            maxLines = 1
        )

        events.take(2).forEach { event ->
            val pillColor = eventTypeColor(event.type)
            val pillShape = RoundedCornerShape(4.dp)
            Text(
                text = event.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = pillColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
                    .clip(pillShape)
                    .background(pillColor.copy(alpha = 0.12f))
                    .border(1.dp, pillColor.copy(alpha = 0.25f), pillShape)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        if (events.size > 2) {
            Text(
                text = "+${events.size - 2}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
