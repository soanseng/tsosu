package app.tsosu.ui.screens.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsosu.R
import app.tsosu.domain.model.RoutineTime
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickAddHabitSheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, tinyVersion: String?, routineTime: RoutineTime, reminderTime: LocalTime?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var tinyVersion by remember { mutableStateOf("") }
    var routineTime by remember { mutableStateOf(RoutineTime.AFTERNOON) }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.quick_add_habit_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                if (it.isNotBlank()) titleError = false
            },
            label = { Text(stringResource(R.string.quick_add_habit_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            isError = titleError,
            supportingText = if (titleError) {
                { Text(stringResource(R.string.quick_add_title_required)) }
            } else null,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = tinyVersion,
            onValueChange = { tinyVersion = it },
            label = { Text(stringResource(R.string.quick_add_tiny_version)) },
            supportingText = { Text(stringResource(R.string.quick_add_tiny_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            stringResource(R.string.quick_add_routine),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RoutineTime.entries.forEach { time ->
                FilterChip(
                    selected = routineTime == time,
                    onClick = { routineTime = time },
                    label = {
                        Text(
                            when (time) {
                                RoutineTime.MORNING -> "${time.emoji} ${stringResource(R.string.habits_morning)}"
                                RoutineTime.AFTERNOON -> "${time.emoji} ${stringResource(R.string.habits_anytime)}"
                                RoutineTime.EVENING -> "${time.emoji} ${stringResource(R.string.habits_evening)}"
                            },
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.material3.OutlinedButton(onClick = { showTimePicker = true }) {
                Text(
                    text = stringResource(R.string.quick_add_habit_reminder) + (
                        reminderTime?.let { " %02d:%02d".format(it.hour, it.minute) } ?: ""
                        ),
                )
            }
            if (reminderTime != null) {
                androidx.compose.material3.IconButton(onClick = { reminderTime = null }) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = stringResource(R.string.habit_reminder_none),
                    )
                }
            }
        }

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    onAdd(title, tinyVersion.takeIf { it.isNotBlank() }, routineTime, reminderTime)
                } else {
                    titleError = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.quick_add_submit_habit))
        }
    }

    if (showTimePicker) {
        val timePickerState = androidx.compose.material3.rememberTimePickerState(
            initialHour = reminderTime?.hour ?: 8,
            initialMinute = reminderTime?.minute ?: 0,
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.habit_field_reminder)) },
            text = { androidx.compose.material3.TimePicker(state = timePickerState) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    reminderTime = LocalTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.habit_ok))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.habit_cancel))
                }
            },
        )
    }
}
