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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R

@Composable
fun WeeklyReviewScreen(viewModel: WeeklyReviewViewModel = hiltViewModel()) {
    val review by viewModel.review.collectAsStateWithLifecycle()

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
        Spacer(Modifier.height(24.dp))

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
