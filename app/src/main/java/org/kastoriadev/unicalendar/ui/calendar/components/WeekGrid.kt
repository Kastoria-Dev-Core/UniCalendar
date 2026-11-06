package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.ui.common.eventTypeColor
import org.kastoriadev.unicalendar.ui.common.formatTimeRange
import org.kastoriadev.unicalendar.ui.common.uppercaseFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

/**
 * Responsive WeekGrid that displays the week's days as structured boxes in full screen mode,
 * and as a compact 7-day strip in split screen mode. Zero horizontal scrolling.
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

    if (isFullScreen) {
        FullScreenWeekBoxGrid(
            weekDays = weekDays,
            selectedDate = selectedDate,
            today = today,
            eventsMap = eventsMap,
            onDateClick = onDateClick,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            weekDays.forEach { date ->
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
 * Fullscreen weekly view laid out in generous, readable boxed cards.
 * Organizes the week into balanced box rows so there are no narrow, compressed vertical strips.
 */
@Composable
private fun FullScreenWeekBoxGrid(
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    today: LocalDate,
    eventsMap: Map<LocalDate, List<CalendarEvent>>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            // Landscape layout: 2 rows (4 weekdays top, 3 days bottom)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0..3) {
                        val date = weekDays[i]
                        FullScreenDayBox(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            events = eventsMap[date].orEmpty(),
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 4..6) {
                        val date = weekDays[i]
                        FullScreenDayBox(
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
        } else {
            // Portrait layout: 3 rows of wide, readable day boxes
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: Monday & Tuesday
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FullScreenDayBox(
                        date = weekDays[0],
                        isSelected = weekDays[0] == selectedDate,
                        isToday = weekDays[0] == today,
                        events = eventsMap[weekDays[0]].orEmpty(),
                        onClick = { onDateClick(weekDays[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    FullScreenDayBox(
                        date = weekDays[1],
                        isSelected = weekDays[1] == selectedDate,
                        isToday = weekDays[1] == today,
                        events = eventsMap[weekDays[1]].orEmpty(),
                        onClick = { onDateClick(weekDays[1]) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Wednesday & Thursday
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FullScreenDayBox(
                        date = weekDays[2],
                        isSelected = weekDays[2] == selectedDate,
                        isToday = weekDays[2] == today,
                        events = eventsMap[weekDays[2]].orEmpty(),
                        onClick = { onDateClick(weekDays[2]) },
                        modifier = Modifier.weight(1f)
                    )
                    FullScreenDayBox(
                        date = weekDays[3],
                        isSelected = weekDays[3] == selectedDate,
                        isToday = weekDays[3] == today,
                        events = eventsMap[weekDays[3]].orEmpty(),
                        onClick = { onDateClick(weekDays[3]) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Friday (full box) & Weekend (Saturday & Sunday stacked boxes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FullScreenDayBox(
                        date = weekDays[4],
                        isSelected = weekDays[4] == selectedDate,
                        isToday = weekDays[4] == today,
                        events = eventsMap[weekDays[4]].orEmpty(),
                        onClick = { onDateClick(weekDays[4]) },
                        modifier = Modifier.weight(1f)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FullScreenDayBox(
                            date = weekDays[5],
                            isSelected = weekDays[5] == selectedDate,
                            isToday = weekDays[5] == today,
                            events = eventsMap[weekDays[5]].orEmpty(),
                            onClick = { onDateClick(weekDays[5]) },
                            isWeekendCompact = true,
                            modifier = Modifier.weight(1f)
                        )
                        FullScreenDayBox(
                            date = weekDays[6],
                            isSelected = weekDays[6] == selectedDate,
                            isToday = weekDays[6] == today,
                            events = eventsMap[weekDays[6]].orEmpty(),
                            onClick = { onDateClick(weekDays[6]) },
                            isWeekendCompact = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A spacious, structured rectangular Day Box for full-screen weekly mode.
 * Ample room for full course names, start/end times, and classroom locations.
 */
@Composable
private fun FullScreenDayBox(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWeekendCompact: Boolean = false
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayName = date.dayOfWeek.getDisplayName(if (isWeekendCompact) TextStyle.SHORT else TextStyle.FULL, locale).uppercaseFor(locale)
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)

    val backgroundColor = when {
        isSelected -> colors.primaryContainer.copy(alpha = 0.45f)
        isToday -> colors.surfaceVariant.copy(alpha = 0.55f)
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
            .fillMaxSize()
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(width = if (isSelected) 2.dp else 1.dp, color = borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = if (isWeekendCompact) 3.dp else 5.dp)
        ) {
            // Header: Day Name + Date Circle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isWeekendCompact) 10.sp else 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected || isToday) colors.primary else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Box(
                    modifier = Modifier
                        .size(if (isWeekendCompact) 20.dp else 22.dp)
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
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isWeekendCompact) 10.5.sp else 11.5.sp),
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) colors.onPrimary else if (isSelected) colors.primary else colors.onSurface
                    )
                }
            }

            HorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 3.dp)
            )

            // Events List inside the Box
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_lessons),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = colors.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    for (event in events) {
                        BoxEventItem(event = event, isCompact = isWeekendCompact)
                    }
                }
            }
        }
    }
}

/**
 * A horizontal event card with time, title, and location that fits comfortably inside a day box.
 */
@Composable
private fun BoxEventItem(
    event: CalendarEvent,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val typeColor = eventTypeColor(event.type)
    val shape = RoundedCornerShape(5.dp)
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(typeColor.copy(alpha = 0.10f))
            .border(0.5.dp, typeColor.copy(alpha = 0.30f), shape)
            .padding(horizontal = 5.dp, vertical = if (isCompact) 2.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimeRange(event.startTime, event.endTime),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                fontWeight = FontWeight.Bold,
                color = typeColor
            )

            if (event.location.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = event.title,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isCompact) 9.5.sp else 10.5.sp, lineHeight = 12.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = colors.onSurface,
            modifier = Modifier.padding(top = 0.5.dp)
        )
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

fun calculateWeekDays(weekStart: LocalDate): List<LocalDate> {
    val monday = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { monday.plusDays(it.toLong()) }
}
