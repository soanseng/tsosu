package app.tsosu.ui.screens.taskdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.TaskStatus
import app.tsosu.ui.components.displayName
import app.tsosu.ui.components.icon
import app.tsosu.ui.components.iconTint
import app.tsosu.ui.util.rememberHaptic
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private enum class DatePickerTarget {
    DUE, SCHEDULED, START
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    taskId: String,
    onDismiss: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = rememberHaptic()

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    LaunchedEffect(state.saved, state.deleted, state.converted) {
        if (state.saved || state.deleted || state.converted) onDismiss()
    }

    if (state.task == null) return

    var datePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.task_detail_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text(stringResource(R.string.task_detail_field_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text(stringResource(R.string.task_detail_field_description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )

        Spacer(Modifier.height(12.dp))

        // Status chips
        Text(stringResource(R.string.task_detail_status), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TaskStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.status == status,
                    onClick = {
                        haptic.tick()
                        viewModel.onStatusChange(status)
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = status.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (state.status == status) {
                                    status.iconTint()
                                } else {
                                    Color.Unspecified
                                },
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(status.displayName())
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.task_detail_priority), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Priority.entries.forEach { p ->
                FilterChip(
                    selected = state.priority == p,
                    onClick = {
                        haptic.tick()
                        viewModel.onPriorityChange(p)
                    },
                    label = {
                        Text(
                            text = p.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (state.priority == p) Color(p.color) else Color.Unspecified,
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.task_detail_energy), style = MaterialTheme.typography.labelLarge)
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

        Text(stringResource(R.string.task_detail_time_estimate), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 5, 15, 30, 60).forEach { minutes ->
                FilterChip(
                    selected = state.estimatedMinutes == minutes,
                    onClick = {
                        haptic.tick()
                        viewModel.onEstimatedMinutesChange(minutes)
                    },
                    label = { Text(if (minutes == 0) stringResource(R.string.task_detail_time_none) else "${minutes}m") },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Due date
        Text(stringResource(R.string.task_detail_due_date), style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { datePickerTarget = DatePickerTarget.DUE }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    state.dueDate?.let { "${it.monthNumber}/${it.dayOfMonth}/${it.year}" }
                        ?: stringResource(R.string.task_detail_no_date),
                )
            }
            if (state.dueDate != null) {
                IconButton(onClick = { viewModel.onDueDateChange(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_detail_clear_date))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Scheduled date
        Text(stringResource(R.string.task_detail_scheduled_date), style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { datePickerTarget = DatePickerTarget.SCHEDULED }) {
                Icon(Icons.Default.EventAvailable, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    state.scheduledDate?.let { "${it.monthNumber}/${it.dayOfMonth}/${it.year}" }
                        ?: stringResource(R.string.task_detail_no_date),
                )
            }
            if (state.scheduledDate != null) {
                IconButton(onClick = { viewModel.onScheduledDateChange(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_detail_clear_date))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Start date
        Text(stringResource(R.string.task_detail_start_date), style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { datePickerTarget = DatePickerTarget.START }) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    state.startDate?.let { "${it.monthNumber}/${it.dayOfMonth}/${it.year}" }
                        ?: stringResource(R.string.task_detail_no_date),
                )
            }
            if (state.startDate != null) {
                IconButton(onClick = { viewModel.onStartDateChange(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_detail_clear_date))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Reminder time
        Text(stringResource(R.string.task_detail_reminder_time), style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.AccessTime, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    state.reminderTime?.let {
                        "%02d:%02d".format(it.hour, it.minute)
                    } ?: stringResource(R.string.task_detail_no_reminder),
                )
            }
            if (state.reminderTime != null) {
                IconButton(onClick = { viewModel.onReminderTimeChange(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_detail_clear_time))
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
            Text(stringResource(R.string.task_detail_save))
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                haptic.confirm()
                viewModel.convertToHabit()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.title.isNotBlank(),
        ) {
            Icon(Icons.Default.Loop, contentDescription = null)
            Spacer(Modifier.padding(start = 4.dp))
            Text(stringResource(R.string.task_convert_to_habit))
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.padding(start = 4.dp))
            Text(stringResource(R.string.task_detail_delete))
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.task_detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.task_detail_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.reject()
                    viewModel.delete()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.task_detail_delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.task_detail_cancel))
                }
            },
        )
    }

    // Date picker dialog (shared for due/scheduled/start)
    if (datePickerTarget != null) {
        val initialMillis = when (datePickerTarget) {
            DatePickerTarget.DUE -> state.dueDate
            DatePickerTarget.SCHEDULED -> state.scheduledDate
            DatePickerTarget.START -> state.startDate
            null -> null
        }?.let {
            it.date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
        )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val ldt = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                        when (datePickerTarget) {
                            DatePickerTarget.DUE -> viewModel.onDueDateChange(ldt)
                            DatePickerTarget.SCHEDULED -> viewModel.onScheduledDateChange(ldt)
                            DatePickerTarget.START -> viewModel.onStartDateChange(ldt)
                            null -> {}
                        }
                    }
                    datePickerTarget = null
                }) {
                    Text(stringResource(R.string.task_detail_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTarget = null }) {
                    Text(stringResource(R.string.task_detail_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog for reminder
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.reminderTime?.hour ?: 9,
            initialMinute = state.reminderTime?.minute ?: 0,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Reminder time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onReminderTimeChange(
                        LocalTime(timePickerState.hour, timePickerState.minute),
                    )
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.task_detail_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.task_detail_cancel))
                }
            },
        )
    }
}
