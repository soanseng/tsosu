package app.tsosu.ui.screens.weeklyreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewScreen(viewModel: WeeklyReviewViewModel = hiltViewModel()) {
    val review by viewModel.review.collectAsStateWithLifecycle()
    val staleTasks by viewModel.staleTasks.collectAsStateWithLifecycle()
    val somedayTasks by viewModel.somedayTasks.collectAsStateWithLifecycle()
    var showWizard by remember { mutableStateOf(false) }

    if (showWizard) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showWizard = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ReviewWizard(
                staleTasks = staleTasks,
                somedayTasks = somedayTasks,
                onKeep = viewModel::keepTask,
                onPark = viewModel::parkTask,
                onDelete = viewModel::deleteTask,
                onPromote = viewModel::promoteTask,
                onDone = { showWizard = false },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.review_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(12.dp))

        androidx.compose.material3.Button(onClick = { showWizard = true }) {
            androidx.compose.material3.Text(stringResource(R.string.review_wizard_start))
        }

        Spacer(Modifier.height(12.dp))

        review?.let { r ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatRow(stringResource(R.string.review_tasks_completed), "${r.tasksCompleted}")
                    StatRow(stringResource(R.string.review_habits_completed), "${r.habitsCompletedTotal}")
                    StatRow(stringResource(R.string.review_focus_days), "${r.focusDaysCompleted}")
                    StatRow(stringResource(R.string.review_time_invested), "${r.totalEstimatedMinutes} min")
                    r.topProject?.let {
                        StatRow(stringResource(R.string.review_top_project), it)
                    }
                    r.longestHabitStreak?.let {
                        StatRow(
                            stringResource(R.string.review_best_streak),
                            "${it.habitTitle}: ${it.currentConsecutiveDays} days",
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.review_celebration),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } ?: Text(stringResource(R.string.review_no_data))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}


@Composable
private fun ReviewWizard(
    staleTasks: List<app.tsosu.domain.model.Task>,
    somedayTasks: List<app.tsosu.domain.model.Task>,
    onKeep: (String) -> Unit,
    onPark: (String) -> Unit,
    onDelete: (String) -> Unit,
    onPromote: (String) -> Unit,
    onDone: () -> Unit,
) {
    var step by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        when (step) {
            0 -> {
                Text(stringResource(R.string.review_wizard_step1_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.review_wizard_step1_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.Button(onClick = { step = 1 }) {
                    Text(stringResource(R.string.review_wizard_next))
                }
            }
            1 -> {
                Text(stringResource(R.string.review_wizard_step2_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (staleTasks.isEmpty()) {
                    Text(stringResource(R.string.review_wizard_all_clear), style = MaterialTheme.typography.bodyMedium)
                }
                staleTasks.take(10).forEach { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(task.title, modifier = Modifier.weight(1f))
                        androidx.compose.material3.TextButton(onClick = { onKeep(task.id) }) {
                            Text(stringResource(R.string.review_wizard_keep))
                        }
                        androidx.compose.material3.TextButton(onClick = { onPark(task.id) }) {
                            Text(stringResource(R.string.review_wizard_someday))
                        }
                        androidx.compose.material3.TextButton(onClick = { onDelete(task.id) }) {
                            Text(stringResource(R.string.review_wizard_delete))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.Button(onClick = { step = 2 }) {
                    Text(stringResource(R.string.review_wizard_next))
                }
            }
            else -> {
                Text(stringResource(R.string.review_wizard_step3_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                if (somedayTasks.isEmpty()) {
                    Text(stringResource(R.string.review_wizard_all_clear), style = MaterialTheme.typography.bodyMedium)
                }
                somedayTasks.take(10).forEach { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(task.title, modifier = Modifier.weight(1f))
                        androidx.compose.material3.TextButton(onClick = { onPromote(task.id) }) {
                            Text(stringResource(R.string.review_wizard_promote))
                        }
                        androidx.compose.material3.TextButton(onClick = { onDelete(task.id) }) {
                            Text(stringResource(R.string.review_wizard_delete))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.Button(onClick = onDone) {
                    Text(stringResource(R.string.review_wizard_finish))
                }
            }
        }
    }
}
