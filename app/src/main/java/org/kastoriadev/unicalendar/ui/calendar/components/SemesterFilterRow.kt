package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SemesterFilterRow(
    selectedSemester: String?,
    onSemesterSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val semesters = listOf(
        "ALL" to "All Courses",
        "1" to "Sem 1",
        "2" to "Sem 2",
        "3" to "Sem 3",
        "4" to "Sem 4",
        "5" to "Sem 5",
        "6" to "Sem 6",
        "7" to "Sem 7",
        "8" to "Sem 8",
        "PDE" to "ΠΔΕ"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(semesters) { (key, label) ->
            val isSelected = if (key == "ALL") {
                selectedSemester == null || selectedSemester == "ALL"
            } else {
                selectedSemester.equals(key, ignoreCase = true)
            }

            FilterChip(
                selected = isSelected,
                onClick = {
                    onSemesterSelected(if (key == "ALL") null else key)
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
