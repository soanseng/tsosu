package app.tsosu.ui.screens.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTaskSheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, priority: Priority, energy: EnergyLevel, estimatedMinutes: Int?, dueDate: LocalDateTime?) -> Unit,
) {
    val haptic = rememberHaptic()
    var title by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.NONE) }
    var selectedEnergy by remember { mutableStateOf(EnergyLevel.MEDIUM) }
    var estimatedMinutes by remember { mutableIntStateOf(0) }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text("New Task", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("What needs doing?") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(12.dp))

        Text("Priority", style = MaterialTheme.typography.labelLarge)
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

        Text("Energy", style = MaterialTheme.typography.labelLarge)
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

        Text("Time estimate", style = MaterialTheme.typography.labelLarge)
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

        Text("Due date", style = MaterialTheme.typography.labelLarge)

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val todayDate = today.date
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = dueDate?.date == todayDate,
                onClick = {
                    haptic.tick()
                    dueDate = LocalDateTime(todayDate, LocalTime(0, 0))
                },
                label = { Text("Today") },
            )
            FilterChip(
                selected = dueDate?.date == todayDate.plus(1, DateTimeUnit.DAY),
                onClick = {
                    haptic.tick()
                    val tomorrowDate = todayDate.plus(1, DateTimeUnit.DAY)
                    dueDate = LocalDateTime(tomorrowDate, LocalTime(0, 0))
                },
                label = { Text("Tomorrow") },
            )
            FilterChip(
                selected = dueDate?.date == todayDate.plus(7, DateTimeUnit.DAY),
                onClick = {
                    haptic.tick()
                    val nextWeekDate = todayDate.plus(7, DateTimeUnit.DAY)
                    dueDate = LocalDateTime(nextWeekDate, LocalTime(0, 0))
                },
                label = { Text("Next Week") },
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    dueDate?.let { "${it.monthNumber}/${it.dayOfMonth}/${it.year}" }
                        ?: "No date",
                )
            }
            if (dueDate != null) {
                IconButton(onClick = { dueDate = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear date")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    haptic.confirm()
                    onAdd(title, selectedPriority, selectedEnergy, estimatedMinutes.takeIf { it > 0 }, dueDate)
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add Task")
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
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
