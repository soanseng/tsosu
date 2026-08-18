package app.tsosu.ui.screens.inbox

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.ui.components.TaskListItem

@Composable
fun InboxScreen(
    viewModel: InboxViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {},
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val staleIds by viewModel.staleIds.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text(stringResource(R.string.inbox_title), style = MaterialTheme.typography.headlineMedium)
        }

        if (staleIds.isNotEmpty()) {
            item(key = "stale-cleanup") {
                var dismissed by remember { mutableStateOf(false) }
                if (!dismissed) {
                    StaleCleanupCard(
                        count = staleIds.size,
                        onCleanUp = {
                            viewModel.cleanUpStale()
                            dismissed = true
                        },
                        onLater = { dismissed = true },
                    )
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Text(
                    "Inbox zero!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "New tasks without a due date appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(tasks, key = { it.id }) { task ->
            TaskListItem(
                task = task,
                onToggleDone = { viewModel.toggleDone(it) },
                onStatusChange = { id, status -> viewModel.setStatus(id, status) },
                onClick = { onTaskClick(it.id) },
            )
        }
    }
}

/** No-shame clean-up suggestion for tasks that have been untouched for a long time. */
@Composable
fun StaleCleanupCard(
    count: Int,
    onCleanUp: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.inbox_stale_title, count),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.inbox_stale_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onLater) {
                    Text(stringResource(R.string.inbox_stale_later))
                }
                Button(onClick = onCleanUp) {
                    Text(stringResource(R.string.inbox_stale_clean))
                }
            }
        }
    }
}
