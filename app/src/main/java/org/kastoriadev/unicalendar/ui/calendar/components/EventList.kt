package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.ui.common.EmptyDayState
import org.kastoriadev.unicalendar.ui.common.EventTypeTag
import org.kastoriadev.unicalendar.ui.common.eventTypeColor
import org.kastoriadev.unicalendar.ui.common.formatTimeRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Bottom padding so the last card can scroll above the floating action button. */
internal val FabClearance = 88.dp

@Composable
fun EventList(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    hasAnyEvents: Boolean,
    onEditNoteClick: (CalendarEvent) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateHeader = remember(selectedDate, locale) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale))
            .replaceFirstChar { it.titlecase(locale) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateHeader,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tap_lesson_for_notes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (events.isEmpty()) {
            EmptyDayState(
                hasAnyEvents = hasAnyEvents,
                emptyDayText = stringResource(R.string.no_events),
                onSyncClick = onSyncClick
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = FabClearance),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(events, key = { it.id }) { event ->
                    ExpandableEventCard(
                        event = event,
                        onEditNoteClick = { onEditNoteClick(event) },
                        onDeleteClick = { onDeleteEvent(event) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandableEventCard(
    event: CalendarEvent,
    onEditNoteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable(event.id) { mutableStateOf(false) }
    val typeColor = eventTypeColor(event.type)
    val colors = MaterialTheme.colorScheme

    Card(
        onClick = { isExpanded = !isExpanded },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colour accent bar, as tall as the card's header content.
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(typeColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatTimeRange(event.startTime, event.endTime),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.primary,
                            maxLines = 1
                        )

                        EventTypeTag(type = event.type)

                        if (event.isSynced) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = stringResource(R.string.content_desc_synced),
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    if (event.location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(if (isExpanded) R.string.content_desc_collapse else R.string.content_desc_expand),
                        tint = colors.onSurfaceVariant
                    )
                }
            }

            // Expanded details and notes
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    HorizontalDivider(
                        color = colors.outlineVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    if (event.description.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.field_description),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    NotesBlock(notes = event.notes, onClick = onEditNoteClick)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.btn_delete), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesBlock(notes: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.field_personal_notes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
            Text(
                text = stringResource(R.string.btn_edit),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (notes.isNotBlank()) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
        } else {
            Text(
                text = stringResource(R.string.notes_empty_tap),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = colors.onSurfaceVariant
            )
        }
    }
}
