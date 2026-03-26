package app.tsosu.ui.screens.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddHabitSheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, tinyVersion: String?, routineTime: RoutineTime) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var tinyVersion by remember { mutableStateOf("") }
    var routineTime by remember { mutableStateOf(RoutineTime.AFTERNOON) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    onAdd(title, tinyVersion.takeIf { it.isNotBlank() }, routineTime)
                } else {
                    titleError = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.quick_add_submit_habit))
        }
    }
}
