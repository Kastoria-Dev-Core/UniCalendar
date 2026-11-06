package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.ui.common.eventTypeColor
import org.kastoriadev.unicalendar.ui.common.uppercaseFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Responsive WeekGrid that fits all 7 days (Monday–Sunday) across the screen
 * with ZERO horizontal scrolling in both full screen and split screen modes.
 */
@Composable
fun WeekGrid(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    eventsMap: Map<LocalDate, List<CalendarEvent>>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false
) {
    val today = LocalDate.now()
    val weekDays = calculateWeekDays(weekStart)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        weekDays.forEach { date ->
            if (isFullScreen) {
                FullScreenWeekDayColumn(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    events = eventsMap[date].orEmpty(),
                    onClick = { onDateClick(date) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                SplitScreenWeekDayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    events = eventsMap[date].orEmpty(),
                    onClick = { onDateClick(date) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Compact 7-day selector cell used in Split Screen mode.
 * Fits all 7 days cleanly above the LessonNotesSection without horizontal scrolling.
 */
@Composable
private fun SplitScreenWeekDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercaseFor(locale)
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)

    val backgroundColor = when {
        isSelected -> colors.primaryContainer.copy(alpha = 0.5f)
        isToday -> colors.surfaceVariant.copy(alpha = 0.6f)
        else -> colors.surface
    }
    val borderColor = when {
        isSelected -> colors.primary
        isToday -> colors.primary.copy(alpha = 0.7f)
        else -> colors.outlineVariant
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .height(76.dp)
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(width = if (isSelected) 2.dp else 1.dp, color = borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Day name (e.g., ΔΕΥ / MON)
            Text(
                text = dayName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = if (isSelected || isToday) colors.primary else colors.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            // Date number in circle badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isToday) colors.primary
                        else if (isSelected) colors.primary.copy(alpha = 0.15f)
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) colors.onPrimary else if (isSelected) colors.primary else colors.onSurface
                )
            }

            // Event indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(5.dp)
            ) {
                if (events.isNotEmpty()) {
                    events.map { it.type }.distinct().take(3).forEach { type ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(eventTypeColor(type))
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }
        }
    }
}

/**
 * Fullscreen vertical day column. Stretches to the pane height and fits all 7 days
 * across the screen width with a compact event card list.
 */
@Composable
private fun FullScreenWeekDayColumn(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercaseFor(locale)
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)

    val backgroundColor = when {
        isSelected -> colors.primaryContainer.copy(alpha = 0.35f)
        isToday -> colors.surfaceVariant.copy(alpha = 0.45f)
        else -> colors.surface
    }
    val borderColor = when {
        isSelected -> colors.primary
        isToday -> colors.primary.copy(alpha = 0.7f)
        else -> colors.outlineVariant
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(width = if (isSelected) 2.dp else 1.dp, color = borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            // Header: Day abbreviation & date number
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected || isToday) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isToday) colors.primary
                            else if (isSelected) colors.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) colors.onPrimary else if (isSelected) colors.primary else colors.onSurface
                    )
                }
            }

            HorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.6f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
            )

            // Scrollable events list for this day
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 2.dp)
                ) {
                    for (event in events) {
                        FullScreenWeekEventCard(event = event)
                    }
                }
            }
        }
    }
}

/**
 * Micro event card designed to fit legibly inside a 7-column fullscreen weekly grid.
 */
@Composable
private fun FullScreenWeekEventCard(
    event: CalendarEvent,
    modifier: Modifier = Modifier
) {
    val typeColor = eventTypeColor(event.type)
    val shape = RoundedCornerShape(4.dp)
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(typeColor.copy(alpha = 0.13f))
            .border(0.5.dp, typeColor.copy(alpha = 0.35f), shape)
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        // Start time
        Text(
            text = event.startTime.format(TIME_FORMATTER),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, lineHeight = 10.sp),
            fontWeight = FontWeight.Bold,
            color = typeColor,
            maxLines = 1
        )

        // Event title
        Text(
            text = event.title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = colors.onSurface
        )

        // Location (if present)
        if (event.location.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.padding(top = 1.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(9.dp)
                )
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 9.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

fun calculateWeekDays(weekStart: LocalDate): List<LocalDate> {
    val monday = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { monday.plusDays(it.toLong()) }
}
