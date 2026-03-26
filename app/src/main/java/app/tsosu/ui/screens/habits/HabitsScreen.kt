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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.domain.model.HabitStreakInfo
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.usecase.HabitWithStatus
import app.tsosu.ui.components.KonfettiOverlay
import app.tsosu.ui.util.rememberHaptic

@Composable
fun HabitsScreen(viewModel: HabitsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showKonfetti = remember { mutableStateOf(false) }
    val haptic = rememberHaptic()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMsg = stringResource(R.string.habits_create_failed)

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect {
            snackbarHostState.showSnackbar(errorMsg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.celebrateEvent.collect {
            showKonfetti.value = true
        }
    }

    KonfettiOverlay(showKonfetti)

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
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (state.habits.isEmpty()) {
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
                .clickable(onClick = onToggle)
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
                        if (info.currentConsecutiveDays > 1) {
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
