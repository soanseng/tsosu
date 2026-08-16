package app.tsosu.ui.screens.habitdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.ui.util.rememberHaptic
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitDetailSheet(
    habitId: String,
    onDismiss: () -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = rememberHaptic()

    LaunchedEffect(habitId) {
        viewModel.loadHabit(habitId)
    }

    LaunchedEffect(state.saved, state.archived) {
        if (state.saved || state.archived) onDismiss()
    }

    if (state.habit == null) return

    var showTimePicker by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.habit_detail_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text(stringResource(R.string.habit_field_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.error && state.title.isBlank(),
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.tinyVersion,
            onValueChange = viewModel::onTinyVersionChange,
            label = { Text(stringResource(R.string.habit_field_tiny)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.habit_field_routine), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HabitRoutineChoice.entries.forEach { choice ->
                FilterChip(
                    selected = state.routine == choice,
                    onClick = {
                        haptic.tick()
                        viewModel.onRoutineChange(choice)
                    },
                    label = {
                        Text(
                            when (choice) {
                                HabitRoutineChoice.MORNING -> stringResource(R.string.habits_morning)
                                HabitRoutineChoice.ANYTIME -> stringResource(R.string.habits_anytime)
                                HabitRoutineChoice.EVENING -> stringResource(R.string.habits_evening)
                                HabitRoutineChoice.OTHER -> stringResource(R.string.habits_other)
                            },
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.habit_field_frequency), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                HabitFrequency.DAILY to R.string.habit_freq_daily,
                HabitFrequency.WEEKDAYS to R.string.habit_freq_weekdays,
                HabitFrequency.CUSTOM to R.string.habit_freq_custom,
            ).forEach { (frequency, label) ->
                FilterChip(
                    selected = state.frequency == frequency,
                    onClick = {
                        haptic.tick()
                        viewModel.onFrequencyChange(frequency)
                    },
                    label = { Text(stringResource(label)) },
                )
            }
        }

        if (state.frequency == HabitFrequency.CUSTOM) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.habit_target_days), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..7).forEach { days ->
                    FilterChip(
                        selected = state.targetDaysPerWeek == days,
                        onClick = {
                            haptic.tick()
                            viewModel.onTargetDaysChange(days)
                        },
                        label = { Text("$days") },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.habit_field_weekdays), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            (1..7).forEach { day ->
                FilterChip(
                    selected = state.weekdays.isEmpty() || day in state.weekdays,
                    onClick = {
                        haptic.tick()
                        viewModel.onWeekdayToggle(day)
                    },
                    label = { Text(stringResource(weekdayLabel(day))) },
                )
            }
        }
        if (state.weekdays.isEmpty()) {
            Text(
                stringResource(R.string.habit_weekdays_every_day),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.habit_field_project), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = state.selectedProjectId == null,
                onClick = {
                    haptic.tick()
                    viewModel.onProjectChange(null)
                },
                label = { Text(stringResource(R.string.habit_project_none)) },
            )
            state.projects.forEach { project ->
                FilterChip(
                    selected = state.selectedProjectId == project.id,
                    onClick = {
                        haptic.tick()
                        viewModel.onProjectChange(project.id)
                    },
                    label = { Text(project.title) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.habit_field_energy), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyLevel.entries.forEach { level ->
                FilterChip(
                    selected = state.energyLevel == level,
                    onClick = {
                        haptic.tick()
                        viewModel.onEnergyChange(level)
                    },
                    label = { Text("${level.emoji} ${level.name.lowercase()}") },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.habit_field_reminder), style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.AccessTime, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    state.reminderTime?.let { "%02d:%02d".format(it.hour, it.minute) }
                        ?: stringResource(R.string.habit_reminder_none),
                )
            }
            if (state.reminderTime != null) {
                IconButton(onClick = { viewModel.onReminderChange(null) }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.habit_reminder_none),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                haptic.confirm()
                viewModel.save()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.title.isNotBlank(),
        ) {
            Text(stringResource(R.string.habit_save))
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showArchiveConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.Default.Archive, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.habit_archive))
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text(stringResource(R.string.habit_archive_confirm_title)) },
            text = { Text(stringResource(R.string.habit_archive_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.reject()
                    viewModel.archive()
                    showArchiveConfirm = false
                }) {
                    Text(
                        stringResource(R.string.habit_archive),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text(stringResource(R.string.habit_cancel))
                }
            },
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.reminderTime?.hour ?: 8,
            initialMinute = state.reminderTime?.minute ?: 0,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.habit_field_reminder)) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onReminderChange(
                        LocalTime(timePickerState.hour, timePickerState.minute),
                    )
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.habit_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.habit_cancel))
                }
            },
        )
    }
}

@Composable
private fun weekdayLabel(day: Int): Int = when (day) {
    1 -> R.string.weekday_mon
    2 -> R.string.weekday_tue
    3 -> R.string.weekday_wed
    4 -> R.string.weekday_thu
    5 -> R.string.weekday_fri
    6 -> R.string.weekday_sat
    else -> R.string.weekday_sun
}
