package app.tsosu.ui.screens.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.ui.components.KonfettiOverlay
import app.tsosu.ui.components.ProgressCard
import app.tsosu.ui.components.TaskListItem

@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showKonfetti = remember { mutableStateOf(false) }

    KonfettiOverlay(showKonfetti)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            ProgressCard(
                completedCount = state.completedCount,
                totalCount = state.totalCount,
                totalMinutes = state.totalEstimatedMinutes,
                streakDays = 0,
            )
        }

        if (state.focusTasks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Today's Focus", style = MaterialTheme.typography.titleMedium)
            }
            items(state.focusTasks, key = { it.id }) { task ->
                TaskListItem(
                    task = task,
                    onToggleDone = { id ->
                        viewModel.onToggleDone(id)
                        showKonfetti.value = true
                    },
                    onStatusChange = { id, status ->
                        viewModel.setStatus(id, status)
                    },
                    onClick = { onTaskClick(it.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        val otherAndInbox = state.otherTasks + state.inboxTasks
        if (otherAndInbox.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Other tasks",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(otherAndInbox, key = { it.id }) { task ->
                TaskListItem(
                    task = task,
                    onToggleDone = { id ->
                        viewModel.onToggleDone(id)
                        showKonfetti.value = true
                    },
                    onStatusChange = { id, status ->
                        viewModel.setStatus(id, status)
                    },
                    onClick = { onTaskClick(it.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (state.focusTasks.isEmpty() && state.otherTasks.isEmpty() && state.inboxTasks.isEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Nothing today. You earned a break!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}
