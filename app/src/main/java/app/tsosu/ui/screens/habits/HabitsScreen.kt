package app.tsosu.ui.screens.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.usecase.HabitWithStatus
import app.tsosu.ui.components.KonfettiOverlay
import app.tsosu.ui.util.rememberHaptic

@Composable
fun HabitsScreen(viewModel: HabitsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showKonfetti = remember { mutableStateOf(false) }
    val haptic = rememberHaptic()

    KonfettiOverlay(showKonfetti)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.habits_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.habits_done_count, state.completedCount, state.totalCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }

        val routineGroups = mapOf(
            RoutineTime.MORNING to "Morning",
            RoutineTime.AFTERNOON to "Anytime",
            RoutineTime.EVENING to "Evening",
        )

        routineGroups.forEach { (time, label) ->
            val routineIds = state.routines
                .filter { it.timeOfDay == time }
                .map { it.id }
                .toSet()
            val habitsInGroup = state.habits.filter { it.habit.routineId in routineIds }

            if (habitsInGroup.isNotEmpty()) {
                item {
                    Text(
                        text = "${time.emoji} $label",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(habitsInGroup, key = { it.habit.id }) { habitWithStatus ->
                    HabitRow(
                        habitWithStatus = habitWithStatus,
                        onToggle = {
                            haptic.confirm()
                            if (!habitWithStatus.isCompletedToday) {
                                showKonfetti.value = true
                            }
                            viewModel.onToggleHabit(habitWithStatus.habit.id)
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        val unroutinedHabits = state.habits.filter { it.habit.routineId == null }
        if (unroutinedHabits.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.habits_other),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(unroutinedHabits, key = { it.habit.id }) { habitWithStatus ->
                HabitRow(
                    habitWithStatus = habitWithStatus,
                    onToggle = {
                        haptic.confirm()
                        if (!habitWithStatus.isCompletedToday) {
                            showKonfetti.value = true
                        }
                        viewModel.onToggleHabit(habitWithStatus.habit.id)
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (state.habits.isEmpty()) {
            item {
                Text(
                    text = "No habits yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Tap + to create your first habit.\nStart small \u2014 try one you can do in 2 minutes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun HabitRow(
    habitWithStatus: HabitWithStatus,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (habitWithStatus.isCompletedToday)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surface,
        label = "habitCardColor",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = habitWithStatus.isCompletedToday,
                onCheckedChange = { onToggle() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habitWithStatus.habit.title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                habitWithStatus.habit.tinyVersion?.let { tiny ->
                    Text(
                        text = tiny,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
