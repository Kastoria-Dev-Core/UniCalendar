package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.domain.model.CalendarEvent

@Composable
fun SemesterSelectPromptDialog(
    currentSemester: String?,
    currentIncludePde: Boolean,
    canDismiss: Boolean,
    onDismiss: () -> Unit,
    onSemesterConfirmed: (semester: String, includePde: Boolean) -> Unit
) {
    var selectedSemester by rememberSaveable {
        mutableStateOf(currentSemester?.takeIf { it.isNotBlank() && it != SEMESTER_ALL } ?: "1")
    }
    var includePde by rememberSaveable { mutableStateOf(currentIncludePde) }
    val semesters = semesterChoices()

    AlertDialog(
        onDismissRequest = {
            if (canDismiss) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_select_semester_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.dialog_select_semester_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.selectableGroup()) {
                    semesters.forEach { (key, label) ->
                        val isSelected = selectedSemester.equals(key, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton,
                                    onClick = { selectedSemester = key }
                                )
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Choosing the ΠΔΕ calendar itself already includes its courses.
                val pdeImplied = selectedSemester == CalendarEvent.SEMESTER_PDE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .toggleable(
                            value = includePde || pdeImplied,
                            enabled = !pdeImplied,
                            role = Role.Checkbox,
                            onValueChange = { includePde = it }
                        )
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includePde || pdeImplied,
                        enabled = !pdeImplied,
                        onCheckedChange = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_include_pde),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (pdeImplied) 0.5f else 1f)
                        )
                        Text(
                            text = stringResource(R.string.dialog_select_semester_pde_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSemesterConfirmed(selectedSemester, includePde) }) {
                Text(text = stringResource(R.string.btn_confirm_calendar))
            }
        },
        dismissButton = {
            if (canDismiss) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.btn_cancel))
                }
            }
        }
    )
}
