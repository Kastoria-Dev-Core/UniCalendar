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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.R

@Composable
fun SemesterFilterRow(
    selectedSemester: String?,
    onSemesterSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val semesters = listOf(
        "ALL" to stringResource(R.string.filter_all_courses),
        "1" to stringResource(R.string.filter_sem_format, "1"),
        "2" to stringResource(R.string.filter_sem_format, "2"),
        "3" to stringResource(R.string.filter_sem_format, "3"),
        "4" to stringResource(R.string.filter_sem_format, "4"),
        "5" to stringResource(R.string.filter_sem_format, "5"),
        "6" to stringResource(R.string.filter_sem_format, "6"),
        "7" to stringResource(R.string.filter_sem_format, "7"),
        "8" to stringResource(R.string.filter_sem_format, "8"),
        "PDE" to stringResource(R.string.filter_pde)
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
