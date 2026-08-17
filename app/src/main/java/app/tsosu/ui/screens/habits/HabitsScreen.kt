package app.tsosu.ui.screens.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.Routine
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.model.HabitStreakInfo
import app.tsosu.domain.recurrence.RecurrenceParser
import app.tsosu.domain.usecase.HabitWithStatus
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
    onHabitClick: (String) -> Unit = {},
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
            snackbarHostState.showSnackbar(errorMsg)
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { padding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
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

        val routineTimes = listOf(
            RoutineTime.MORNING,
            RoutineTime.AFTERNOON,
            RoutineTime.EVENING,
        )

        routineTimes.forEach { time ->
            val routineIds = state.routines
                .filter { it.timeOfDay == time }
                .map { it.id }
                .toSet()
            val habitsInGroup = state.habits.filter { it.habit.routineId in routineIds }

            if (habitsInGroup.isNotEmpty()) {
                item {
                    Text(
                        text = "${time.emoji} ${
                            when (time) {
                                RoutineTime.MORNING -> stringResource(R.string.habits_morning)
                                RoutineTime.AFTERNOON -> stringResource(R.string.habits_anytime)
                                RoutineTime.EVENING -> stringResource(R.string.habits_evening)
                            }
                        }",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(habitsInGroup, key = { it.habit.id }) { habitWithStatus ->
                    HabitRow(
                        habitWithStatus = habitWithStatus,
                        streakInfo = state.streaks[habitWithStatus.habit.id],
                        onToggle = {
                            haptic.confirm()
                            viewModel.onToggleHabit(habitWithStatus.habit.id)
                        },
                        onOpen = { onHabitClick(habitWithStatus.habit.id) },
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
                    streakInfo = state.streaks[habitWithStatus.habit.id],
                    onToggle = {
                        haptic.confirm()
                        viewModel.onToggleHabit(habitWithStatus.habit.id)
                    },
                    onOpen = { onHabitClick(habitWithStatus.habit.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        val recurring = state.recurringTasks
        if (recurring.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.habits_recurring_section),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(recurring, key = { "recurring-${it.id}" }) { task ->
                RecurringTaskRow(
                    task = task,
                    streak = streakDays(task.completions, today),
                    onToggle = {
                        haptic.confirm()
                        viewModel.onToggleRecurringTask(task.id)
                    },
                    onOpen = { onTaskClick(task.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (state.habits.isEmpty() && state.recurringTasks.isEmpty()) {
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
    }
}

@Composable
private fun HabitRow(
    habitWithStatus: HabitWithStatus,
    streakInfo: HabitStreakInfo?,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (habitWithStatus.isCompletedToday)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        label = "habitCardColor",
    )

    val cornerRadius by animateDpAsState(
        targetValue = 16.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "habitCornerRadius",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = habitWithStatus.isCompletedToday,
                onCheckedChange = { onToggle() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habitWithStatus.habit.title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    streakInfo?.let { info ->
                        if (info.currentConsecutiveDays > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "\uD83D\uDD25 ${info.currentConsecutiveDays}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                habitWithStatus.habit.tinyVersion?.let { tiny ->
                    Text(
                        text = tiny,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                streakInfo?.let { info ->
                    if (info.completedLast7Days > 0 || habitWithStatus.isCompletedToday) {
                        Spacer(Modifier.height(6.dp))
                        WeekProgressBar(
                            completed = info.completedLast7Days,
                            total = 7,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekProgressBar(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(total) { index ->
            val isFilled = index < completed
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isFilled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
fun RecurringTaskRow(
    task: Task,
    streak: Int,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { onToggle() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    task.recurrenceRule?.let {
                        Text(
                            text = "🔁 ${RecurrenceParser.toDisplayLabel(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (streak > 0) {
                        Text(
                            text = stringResource(R.string.habits_recurring_streak, streak),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
