package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private enum class TimeField { START, END }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        location: String,
        semester: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        type: EventType
    ) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = remember(selectedDate, locale) {
        selectedDate.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", locale))
    }

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var semester by rememberSaveable { mutableStateOf("") }
    var startTime by rememberSaveable { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by rememberSaveable { mutableStateOf(LocalTime.of(11, 0)) }
    var selectedType by rememberSaveable { mutableStateOf(EventType.LECTURE) }
    var showTitleError by rememberSaveable { mutableStateOf(false) }
    var showTimeError by rememberSaveable { mutableStateOf(false) }
    var editingTime by rememberSaveable { mutableStateOf<TimeField?>(null) }
    var semesterMenuExpanded by remember { mutableStateOf(false) }

    val semesterOptions = semesterChoices(includeNone = true)
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_add_title, dateLabel),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) showTitleError = false
                    },
                    label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true,
                    isError = showTitleError,
                    supportingText = if (showTitleError) {
                        { Text(stringResource(R.string.error_empty_title)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeFieldButton(
                        label = stringResource(R.string.field_start_time),
                        value = startTime.format(timeFormatter),
                        onClick = { editingTime = TimeField.START },
                        modifier = Modifier.weight(1f)
                    )
                    TimeFieldButton(
                        label = stringResource(R.string.field_end_time),
                        value = endTime.format(timeFormatter),
                        isError = showTimeError,
                        onClick = { editingTime = TimeField.END },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (showTimeError) {
                    Text(
                        text = stringResource(R.string.error_end_before_start),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(R.string.field_location_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = semesterMenuExpanded,
                    onExpandedChange = { semesterMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = semesterOptions.firstOrNull { it.first == semester }?.second.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.field_semester)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = semesterMenuExpanded,
                        onDismissRequest = { semesterMenuExpanded = false }
                    ) {
                        for ((key, label) in semesterOptions) {
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    semester = key
                                    semesterMenuExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.field_description_input)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.field_category),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (type in EventType.entries) {
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(stringResource(type.displayNameRes)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showTitleError = title.isBlank()
                    showTimeError = !endTime.isAfter(startTime)
                    if (!showTitleError && !showTimeError) {
                        onConfirm(
                            title,
                            description,
                            location,
                            semester,
                            LocalDateTime.of(selectedDate, startTime),
                            LocalDateTime.of(selectedDate, endTime),
                            selectedType
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )

    editingTime?.let { field ->
        TimePickerDialog(
            initialTime = if (field == TimeField.START) startTime else endTime,
            onDismiss = { editingTime = null },
            onConfirm = { picked ->
                if (field == TimeField.START) {
                    // Keep the event's length when its start moves, like other calendar apps.
                    val length = Duration.between(startTime, endTime).takeIf { !it.isNegative && !it.isZero }
                        ?: Duration.ofHours(1)
                    startTime = picked
                    val shiftedEnd = picked.plus(length)
                    endTime = if (shiftedEnd.isAfter(picked)) shiftedEnd else LocalTime.of(23, 59)
                } else {
                    endTime = picked
                }
                showTimeError = !endTime.isAfter(startTime)
                editingTime = null
            }
        )
    }
}

/** A read-only text field that opens a time picker when tapped. */
@Composable
private fun TimeFieldButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = isError,
            label = { Text(label, maxLines = 1) },
            trailingIcon = { Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_pick_time_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.btn_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
