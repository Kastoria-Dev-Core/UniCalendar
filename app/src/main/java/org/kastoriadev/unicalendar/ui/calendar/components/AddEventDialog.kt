package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.domain.model.EventType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
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
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf("09") }
    var startMinute by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("11") }
    var endMinute by remember { mutableStateOf("00") }
    var selectedType by remember { mutableStateOf(EventType.LECTURE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Event for ${selectedDate}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (e.g. Room 102)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = semester,
                    onValueChange = { semester = it },
                    label = { Text("Semester (optional, e.g. 1, 3, 5, PDE)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Time")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { if (it.length <= 2) startHour = it },
                        label = { Text("Start H") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startMinute,
                        onValueChange = { if (it.length <= 2) startMinute = it },
                        label = { Text("Start M") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endHour,
                        onValueChange = { if (it.length <= 2) endHour = it },
                        label = { Text("End H") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endMinute,
                        onValueChange = { if (it.length <= 2) endMinute = it },
                        label = { Text("End M") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(text = "Category")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (type in EventType.values()) {
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) }
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Title cannot be empty"
                        return@Button
                    }
                    val sh = startHour.toIntOrNull() ?: 9
                    val sm = startMinute.toIntOrNull() ?: 0
                    val eh = endHour.toIntOrNull() ?: 11
                    val em = endMinute.toIntOrNull() ?: 0

                    if (sh !in 0..23 || sm !in 0..59 || eh !in 0..23 || em !in 0..59) {
                        errorMessage = "Please enter valid time values (00-23, 00-59)"
                        return@Button
                    }

                    val start = LocalDateTime.of(selectedDate, LocalTime.of(sh, sm))
                    val end = LocalDateTime.of(selectedDate, LocalTime.of(eh, em))

                    onConfirm(title, description, location, semester, start, end, selectedType)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
