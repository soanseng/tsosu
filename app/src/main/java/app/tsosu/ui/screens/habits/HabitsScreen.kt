package app.tsosu.ui.screens.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.domain.model.Task
import app.tsosu.ui.util.recurrenceDisplayLabel
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.recurrence.RecurrenceParser
import app.tsosu.ui.components.KonfettiOverlay
import app.tsosu.ui.util.rememberHaptic
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

@Composable
fun HabitsScreen(
    onTaskClick: (String) -> Unit = {},
    viewModel: HabitsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val freezes by viewModel.freezes.collectAsStateWithLifecycle()
    val konfettiTrigger = remember { mutableIntStateOf(0) }
    val haptic = rememberHaptic()
    val snackbarHostState = remember { SnackbarHostState() }
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val errorMsg = stringResource(R.string.habits_create_failed)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messageEvent.collect { res ->
            snackbarHostState.showSnackbar(context.getString(res))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.celebrateEvent.collect {
            konfettiTrigger.intValue++
        }
    }

    KonfettiOverlay(konfettiTrigger)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.habits_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = { viewModel.buyFreeze() },
                        label = {
                            Text(
                                "❄$freezes · " + stringResource(R.string.habits_buy_freeze),
                            )
                        },
                    )
                }
                Text(
                    text = stringResource(R.string.habits_done_count, state.completedCount, state.totalCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }

            val shieldedFor = state.shieldedByTask
            val routineGroups = listOf(
                RoutineTime.MORNING to R.string.habits_morning,
                RoutineTime.AFTERNOON to R.string.habits_anytime,
                RoutineTime.EVENING to R.string.habits_evening,
            )

            routineGroups.forEach { (time, labelRes) ->
                val inGroup = state.tasks.filter { it.routineTime == time }
                if (inGroup.isNotEmpty()) {
                    item(key = "group-${time.name}") {
                        Text(
                            text = "${time.emoji} ${stringResource(labelRes)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    items(inGroup, key = { it.id }) { task ->
                        RecurringTaskRow(
                            task = task,
                            streak = streakDays(task.completions + (shieldedFor[task.id] ?: emptySet()), today),
                            checked = today in task.completions,
                            onToggle = {
                                haptic.confirm()
                                viewModel.onToggleRecurringTask(task.id)
                            },
                            onOpen = { onTaskClick(task.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            val otherTasks = state.tasks.filter { it.routineTime == null }
            if (otherTasks.isNotEmpty()) {
                item(key = "group-other") {
                    Text(
                        text = stringResource(R.string.habits_other),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(otherTasks, key = { it.id }) { task ->
                    RecurringTaskRow(
                        task = task,
                        streak = streakDays(task.completions + (shieldedFor[task.id] ?: emptySet()), today),
                        checked = today in task.completions,
                        onToggle = {
                            haptic.confirm()
                            viewModel.onToggleRecurringTask(task.id)
                        },
                        onOpen = { onTaskClick(task.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (state.tasks.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.habits_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.habits_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun RecurringTaskRow(
    task: Task,
    streak: Int,
    checked: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (streak > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "🔥 $streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                task.tinyVersion?.let { tiny ->
                    Text(
                        text = tiny,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                task.recurrenceRule?.let {
                    Text(
                        text = "🔁 ${recurrenceDisplayLabel(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Consecutive days of completion ending today (or yesterday if today is not done yet). */
private fun streakDays(completions: List<LocalDate>, today: LocalDate): Int {
    val done = completions.toSet()
    var cursor = today
    if (cursor !in done) cursor = cursor.minus(DatePeriod(days = 1))
    var streak = 0
    while (cursor in done) {
        streak++
        cursor = cursor.minus(DatePeriod(days = 1))
    }
    return streak
}
