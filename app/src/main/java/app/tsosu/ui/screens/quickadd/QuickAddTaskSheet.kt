package app.tsosu.ui.screens.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.tsosu.ui.screens.recurrencehelp.RecurrenceHelpSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsosu.R
import app.tsosu.domain.recurrence.RecurrenceParser
import app.tsosu.domain.recurrence.RecurrenceResult
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.ui.util.rememberHaptic
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private enum class RecurrenceOption(val label: String, val rrule: String?) {
    NONE("None", null),
    DAILY("Daily", "RRULE:FREQ=DAILY"),
    WEEKLY("Weekly", "RRULE:FREQ=WEEKLY"),
    CUSTOM("Custom", null),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddTaskSheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, priority: Priority, energy: EnergyLevel, estimatedMinutes: Int?, dueDate: LocalDateTime?, reminderTime: LocalTime?, recurrenceRule: String?) -> Unit,
) {
    val haptic = rememberHaptic()
    val recurrenceParser = remember { RecurrenceParser() }
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var selectedPriority by remember { mutableStateOf(Priority.NONE) }
    var selectedEnergy by remember { mutableStateOf(EnergyLevel.MEDIUM) }
    var estimatedMinutes by remember { mutableIntStateOf(0) }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(null) }
    // True once the user picked a date manually; a "starting <date>" prefill
    // from the title never overwrites a manual pick.
    var datePickedManually by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceOption.NONE) }
    var customRecurrence by remember { mutableStateOf("") }
    var detectedRrule by remember { mutableStateOf<String?>(null) }
    var cleanTitle by remember { mutableStateOf("") }
    var showRecurrenceHelp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.quick_add_task_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { newValue ->
                title = newValue
                if (newValue.isNotBlank()) titleError = false
                // Detect trailing recurrence pattern
                val extraction = recurrenceParser.extractFromTitle(newValue)
                if (extraction.rrule != null) {
                    detectedRrule = extraction.rrule
                    cleanTitle = extraction.title
                    // "starting <date>" prefills the first due date unless the
                    // user already picked one manually.
                    extraction.startDate?.let { start ->
                        if (!datePickedManually) {
                            dueDate = LocalDateTime(start, LocalTime(0, 0))
                        }
                    }
                } else {
                    detectedRrule = null
                    cleanTitle = newValue
                }
            },
            label = { Text(stringResource(R.string.quick_add_task_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = titleError,
            supportingText = if (titleError) {
                { Text(stringResource(R.string.quick_add_title_required)) }
            } else null,
        )

        // Show detected recurrence chip
        val currentDetectedRrule = detectedRrule
        if (currentDetectedRrule != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = true,
                    onClick = {
                        detectedRrule = null
                    },
                    label = {
                        Text("\uD83D\uDD01 ${RecurrenceParser.toDisplayLabel(currentDetectedRrule)}")
                    },
                )
                IconButton(onClick = {
                    detectedRrule = null
                }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.quick_add_clear_recurrence))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.quick_add_priority), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Priority.entries.forEach { p ->
                FilterChip(
                    selected = selectedPriority == p,
                    onClick = {
                        haptic.tick()
                        selectedPriority = p
                    },
                    label = {
                        Text(
                            text = p.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (selectedPriority == p) Color(p.color) else Color.Unspecified,
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.quick_add_energy), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyLevel.entries.forEach { level ->
                FilterChip(
                    selected = selectedEnergy == level,
                    onClick = {
                        haptic.tick()
                        selectedEnergy = level
                    },
                    label = { Text("${level.emoji} ${level.name.lowercase()}") },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.quick_add_time), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 15, 30, 60).forEach { minutes ->
                FilterChip(
                    selected = estimatedMinutes == minutes,
                    onClick = {
                        haptic.tick()
                        estimatedMinutes = minutes
                    },
                    label = { Text("${minutes}m") },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.quick_add_due_date), style = MaterialTheme.typography.labelLarge)

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val todayDate = today.date
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = dueDate?.date == todayDate,
                onClick = {
                    haptic.tick()
                    datePickedManually = true
                    dueDate = LocalDateTime(todayDate, LocalTime(0, 0))
                },
                label = { Text(stringResource(R.string.quick_add_today)) },
            )
            FilterChip(
                selected = dueDate?.date == todayDate.plus(1, DateTimeUnit.DAY),
                onClick = {
                    haptic.tick()
                    datePickedManually = true
                    val tomorrowDate = todayDate.plus(1, DateTimeUnit.DAY)
                    dueDate = LocalDateTime(tomorrowDate, LocalTime(0, 0))
                },
                label = { Text(stringResource(R.string.quick_add_tomorrow)) },
            )
            FilterChip(
                selected = dueDate?.date == todayDate.plus(7, DateTimeUnit.DAY),
                onClick = {
                    haptic.tick()
                    datePickedManually = true
                    val nextWeekDate = todayDate.plus(7, DateTimeUnit.DAY)
                    dueDate = LocalDateTime(nextWeekDate, LocalTime(0, 0))
                },
                label = { Text(stringResource(R.string.quick_add_next_week)) },
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.a11y_pick_date))
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    dueDate?.let { "${it.monthNumber}/${it.dayOfMonth}/${it.year}" }
                        ?: stringResource(R.string.quick_add_no_date),
                )
            }
            if (dueDate != null) {
                IconButton(onClick = {
                    datePickedManually = true
                    dueDate = null
                }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.quick_add_clear_date))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Reminder time
        Text("Reminder", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.a11y_pick_time))
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    reminderTime?.let { "%02d:%02d".format(it.hour, it.minute) }
                        ?: "Add reminder",
                )
            }
            if (reminderTime != null) {
                IconButton(onClick = { reminderTime = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear reminder")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Recurrence
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Recurrence", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { showRecurrenceHelp = true }) {
                Text(stringResource(R.string.recurrence_help_open))
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RecurrenceOption.entries.forEach { option ->
                FilterChip(
                    selected = selectedRecurrence == option,
                    onClick = {
                        haptic.tick()
                        selectedRecurrence = option
                        if (option != RecurrenceOption.CUSTOM) {
                            customRecurrence = ""
                        }
                    },
                    label = { Text(option.label) },
                )
            }
        }
        if (selectedRecurrence == RecurrenceOption.CUSTOM) {
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = customRecurrence,
                onValueChange = { customRecurrence = it },
                label = { Text("e.g. every 2 days") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        if (showRecurrenceHelp) {
            ModalBottomSheet(
                onDismissRequest = { showRecurrenceHelp = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                RecurrenceHelpSheet()
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val finalTitle = if (detectedRrule != null) cleanTitle else title
                if (finalTitle.isNotBlank()) {
                    haptic.confirm()
                    // Priority: detected from title > manual custom > preset
                    val recurrenceRule = detectedRrule ?: when (selectedRecurrence) {
                        RecurrenceOption.CUSTOM -> {
                            val parsed = recurrenceParser.parse(customRecurrence)
                            when (parsed) {
                                is RecurrenceResult.Success -> parsed.rrule
                                is RecurrenceResult.Unrecognized -> customRecurrence.takeIf { it.isNotBlank() }
                            }
                        }
                        else -> selectedRecurrence.rrule
                    }
                    onAdd(
                        finalTitle,
                        selectedPriority,
                        selectedEnergy,
                        estimatedMinutes.takeIf { it > 0 },
                        dueDate,
                        reminderTime,
                        recurrenceRule,
                    )
                    onDismiss()
                } else {
                    titleError = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.quick_add_submit_task))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.let {
                it.date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dueDate = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.quick_add_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.quick_add_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog for reminder
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderTime?.hour ?: 9,
            initialMinute = reminderTime?.minute ?: 0,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Reminder time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    reminderTime = LocalTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.quick_add_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.quick_add_cancel))
                }
            },
        )
    }
}
