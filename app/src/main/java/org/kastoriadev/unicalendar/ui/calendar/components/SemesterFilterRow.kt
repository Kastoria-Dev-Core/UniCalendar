package org.kastoriadev.unicalendar.ui.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.domain.model.CalendarEvent

/** The eight course semesters, in display order. */
internal val CourseSemesterKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8")

/** Semester keys used across the app, in display order: the semesters, then ΠΔΕ. */
internal val SemesterKeys = CourseSemesterKeys + CalendarEvent.SEMESTER_PDE

internal const val SEMESTER_ALL = "ALL"

/** (key, label) pairs for semester pickers. "" means "no semester", [SEMESTER_ALL] means all of them. */
@Composable
internal fun semesterChoices(includeNone: Boolean = false, includeAll: Boolean = false): List<Pair<String, String>> {
    val choices = mutableListOf<Pair<String, String>>()
    if (includeAll) choices += SEMESTER_ALL to stringResource(R.string.sync_all_semesters_label)
    if (includeNone) choices += "" to stringResource(R.string.semester_none)
    for (key in SemesterKeys) choices += key to semesterLabel(key)
    return choices
}

/** Full name of a semester key ("3o Εξάμηνο" / "Semester 3", "ΠΔΕ ..."). */
@Composable
internal fun semesterLabel(key: String?): String = when {
    key.isNullOrBlank() || key.equals(SEMESTER_ALL, ignoreCase = true) -> stringResource(R.string.semester_all)
    key.equals(CalendarEvent.SEMESTER_PDE, ignoreCase = true) -> stringResource(R.string.semester_pde)
    else -> stringResource(R.string.semester_format, key)
}

@Composable
fun SemesterFilterRow(
    selectedSemester: String?,
    onSemesterSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val semesters = buildList {
        add(SEMESTER_ALL to stringResource(R.string.filter_all_courses))
        for (key in SemesterKeys) {
            add(
                key to if (key == CalendarEvent.SEMESTER_PDE) {
                    stringResource(R.string.filter_pde)
                } else {
                    stringResource(R.string.filter_sem_format, key)
                }
            )
        }
    }
    val selectedIndex = semesters.indexOfFirst { (key, _) ->
        if (key == SEMESTER_ALL) selectedSemester.isNullOrBlank() || selectedSemester == SEMESTER_ALL
        else selectedSemester.equals(key, ignoreCase = true)
    }.coerceAtLeast(0)

    val listState = rememberLazyListState()
    // Bring the active semester into view (e.g. "Sem 7" starts off-screen on phones).
    LaunchedEffect(selectedIndex) {
        val visible = listState.layoutInfo.visibleItemsInfo
        val fullyVisible = visible.any { it.index == selectedIndex && it.offset >= 0 &&
            it.offset + it.size <= listState.layoutInfo.viewportEndOffset }
        if (!fullyVisible) listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(semesters, key = { _, item -> item.first }) { index, (key, label) ->
            val isSelected = index == selectedIndex
            FilterChip(
                selected = isSelected,
                onClick = { onSemesterSelected(if (key == SEMESTER_ALL) null else key) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}
