package org.kastoriadev.unicalendar.ui.calendar.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScheduleDialog(
    onDismiss: () -> Unit,
    onSyncConfirm: (url: String, targetSemester: String?) -> Unit,
    onFileSelected: (uri: Uri, targetSemester: String?) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf("ALL") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val sem = if (selectedSemester == "ALL") null else selectedSemester
            onFileSelected(uri, sem)
        }
    }

    val semesterOptions = listOf(
        "ALL" to "All Semesters (Import Everything)",
        "1" to "1ο Εξάμηνο (Semester 1)",
        "2" to "2ο Εξάμηνο (Semester 2)",
        "3" to "3ο Εξάμηνο (Semester 3)",
        "4" to "4ο Εξάμηνο (Semester 4)",
        "5" to "5ο Εξάμηνο (Semester 5)",
        "6" to "6ο Εξάμηνο (Semester 6)",
        "7" to "7ο Εξάμηνο (Semester 7)",
        "8" to "8ο Εξάμηνο (Semester 8)",
        "PDE" to "ΠΔΕ (Παιδαγωγική & Διδακτική Επάρκεια)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Sync University Schedule") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Supports department .docx timetable files, announcement links, or .ics feeds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Semester Filter Dropdown
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = semesterOptions.firstOrNull { it.first == selectedSemester }?.second ?: selectedSemester,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by Semester") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        for ((key, label) in semesterOptions) {
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedSemester = key
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        errorMessage = null
                    },
                    label = { Text("Schedule Web URL") },
                    placeholder = { Text("https://cs.uowm.gr/.../program.docx") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Direct local file import button (.docx or .ics)
                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/msword",
                                "text/calendar",
                                "*/*"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Choose Local .docx / .ics File")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                        errorMessage = "Please enter a valid HTTP/HTTPS URL"
                        return@Button
                    }
                    val targetSem = if (selectedSemester == "ALL") null else selectedSemester
                    onSyncConfirm(url.trim(), targetSem)
                }
            ) {
                Text("Fetch & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
