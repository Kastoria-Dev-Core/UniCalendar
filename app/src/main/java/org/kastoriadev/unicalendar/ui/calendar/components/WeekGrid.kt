package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.ui.common.eventTypeColor
import org.kastoriadev.unicalendar.ui.common.formatTimeRange
import org.kastoriadev.unicalendar.ui.common.uppercaseFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

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
    val listState = rememberLazyListState()

    // Keep the selected day in view when the week or the selection changes.
    val selectedIndex = weekDays.indexOf(selectedDate)
    LaunchedEffect(selectedDate, weekStart) {
        if (selectedIndex != -1) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(weekDays, key = { it.toString() }) { date ->
            WeekDayColumn(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                events = eventsMap[date].orEmpty(),
                onClick = { onDateClick(date) },
                isFullScreen = isFullScreen
            )
        }
    }
}

@Composable
fun LazyItemScope.WeekDayColumn(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit,
    isFullScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercaseFor(locale)
    val colors = MaterialTheme.colorScheme
    val borderColor = when {
        isSelected -> colors.primary
        isToday -> colors.primary.copy(alpha = 0.7f)
        else -> colors.outlineVariant
    }
    val backgroundColor = when {
        isSelected -> colors.primaryContainer.copy(alpha = 0.35f)
        isToday -> colors.surfaceVariant.copy(alpha = 0.5f)
        else -> colors.surface
    }

    val cardModifier = if (isFullScreen) {
        modifier
            .fillParentMaxHeight()
            .width(185.dp)
    } else {
        modifier
            .width(148.dp)
            .height(260.dp)
    }

    Card(
        onClick = onClick,
        modifier = cardModifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(width = if (isSelected) 2.dp else 1.dp, color = borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize()
        ) {
            // Header: day name and date number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected || isToday) colors.primary else colors.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isToday) colors.primary else colors.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) colors.onPrimary else colors.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_lessons),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Scrollable so a busy day never clips its last lesson.
                val visible = if (isFullScreen) events else events.take(MAX_SPLIT_BADGES)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    for (event in visible) {
                        WeekLessonBadge(event = event, isFullScreen = isFullScreen)
                    }
                    if (events.size > visible.size) {
                        Text(
                            text = stringResource(R.string.more_events_format, events.size - visible.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_SPLIT_BADGES = 3

@Composable
fun WeekLessonBadge(
    event: CalendarEvent,
    isFullScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val typeColor = eventTypeColor(event.type)
    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(typeColor.copy(alpha = 0.10f))
            .border(1.dp, typeColor.copy(alpha = 0.25f), shape)
            .padding(6.dp)
    ) {
        Text(
            text = formatTimeRange(event.startTime, event.endTime),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = typeColor,
            maxLines = 1
        )

        Text(
            text = event.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (isFullScreen) 3 else 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (event.location.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun calculateWeekDays(weekStart: LocalDate): List<LocalDate> {
    val monday = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { monday.plusDays(it.toLong()) }
}
