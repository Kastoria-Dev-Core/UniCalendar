package org.kastoriadev.unicalendar.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.kastoriadev.unicalendar.BuildConfig
import org.kastoriadev.unicalendar.R
import org.kastoriadev.unicalendar.domain.model.AppLanguage
import org.kastoriadev.unicalendar.domain.model.CalendarEvent
import org.kastoriadev.unicalendar.domain.model.DisplayLayoutMode
import org.kastoriadev.unicalendar.domain.model.ThemeMode
import org.kastoriadev.unicalendar.ui.calendar.components.SemesterSelectPromptDialog
import org.kastoriadev.unicalendar.ui.calendar.components.semesterLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    displayLayoutMode: DisplayLayoutMode,
    appLanguage: AppLanguage,
    selectedSemester: String?,
    includePde: Boolean,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDisplayLayoutModeChange: (DisplayLayoutMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onSemesterConfirmed: (semester: String, includePde: Boolean) -> Unit,
    onIncludePdeChange: (Boolean) -> Unit,
    onClearAllSchedules: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var showSemesterDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Appearance (theme & calendar layout)
            SectionTitle(stringResource(R.string.tab_appearance))

            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    CardTitle(stringResource(R.string.settings_section_theme))
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.selectableGroup()) {
                        ChoiceRow(
                            selected = themeMode == ThemeMode.LIGHT,
                            label = stringResource(R.string.theme_light),
                            icon = Icons.Default.LightMode,
                            onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                        )
                        ChoiceRow(
                            selected = themeMode == ThemeMode.DARK,
                            label = stringResource(R.string.theme_dark),
                            icon = Icons.Default.DarkMode,
                            onClick = { onThemeModeChange(ThemeMode.DARK) }
                        )
                    }
                }
            }

            SettingsCard {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CardTitle(stringResource(R.string.settings_section_layout))
                    LayoutOption(
                        selected = displayLayoutMode == DisplayLayoutMode.SPLIT,
                        icon = Icons.Default.ViewAgenda,
                        title = stringResource(R.string.layout_split),
                        description = stringResource(R.string.layout_split_desc),
                        onClick = { onDisplayLayoutModeChange(DisplayLayoutMode.SPLIT) }
                    )
                    LayoutOption(
                        selected = displayLayoutMode == DisplayLayoutMode.FULLSCREEN,
                        icon = Icons.Default.ViewCompact,
                        title = stringResource(R.string.layout_fullscreen),
                        description = stringResource(R.string.layout_fullscreen_desc),
                        onClick = { onDisplayLayoutModeChange(DisplayLayoutMode.FULLSCREEN) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Section 2: Semester (the onboarding choice, changeable at any time)
            SectionTitle(stringResource(R.string.settings_section_semester))

            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_current_semester),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = semesterLabel(selectedSemester),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FilledTonalButton(onClick = { showSemesterDialog = true }) {
                            Text(text = stringResource(R.string.btn_change))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(4.dp))

                    val pdeImplied = selectedSemester == CalendarEvent.SEMESTER_PDE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .toggleable(
                                value = includePde || pdeImplied,
                                enabled = !pdeImplied,
                                role = Role.Switch,
                                onValueChange = onIncludePdeChange
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = includePde || pdeImplied,
                            enabled = !pdeImplied,
                            onCheckedChange = null
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Section 3: Language
            SectionTitle(stringResource(R.string.settings_section_language))

            SettingsCard {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .selectableGroup()
                ) {
                    ChoiceRow(
                        selected = appLanguage == AppLanguage.ENGLISH,
                        label = stringResource(R.string.lang_en),
                        onClick = { onLanguageChange(AppLanguage.ENGLISH) }
                    )
                    ChoiceRow(
                        selected = appLanguage == AppLanguage.GREEK,
                        label = stringResource(R.string.lang_el),
                        onClick = { onLanguageChange(AppLanguage.GREEK) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Section 4: Schedule data management
            SectionTitle(stringResource(R.string.settings_section_data))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_clear_all_schedules),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = stringResource(R.string.desc_clear_all_schedules),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { showClearConfirmationDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_clear_all_schedules),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Section 5: About & attributions
            SectionTitle(stringResource(R.string.tab_about))

            SettingsCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = stringResource(R.string.about_built_by),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.about_org_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = stringResource(R.string.about_disclaimer_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = stringResource(R.string.about_libraries_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.about_libraries_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showSemesterDialog) {
        SemesterSelectPromptDialog(
            currentSemester = selectedSemester,
            currentIncludePde = includePde,
            canDismiss = true,
            onDismiss = { showSemesterDialog = false },
            onSemesterConfirmed = { semester, pde ->
                showSemesterDialog = false
                onSemesterConfirmed(semester, pde)
            }
        )
    }

    // Confirmation before wiping all schedules
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_clear_all_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_clear_all_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmationDialog = false
                        onClearAllSchedules()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(R.string.btn_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmationDialog = false }) {
                    Text(text = stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content
    )
}

/** A radio row; the whole row is the touch target. */
@Composable
private fun ChoiceRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LayoutOption(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // Card(onClick) clips the ripple to the rounded shape.
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                this.selected = selected
                role = Role.RadioButton
            },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer.copy(alpha = 0.5f) else colors.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}
