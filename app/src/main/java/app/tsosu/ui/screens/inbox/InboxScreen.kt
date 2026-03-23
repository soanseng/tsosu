package app.tsosu.ui.screens.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.ui.components.TaskListItem

@Composable
fun InboxScreen(
    viewModel: InboxViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {},
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text("Inbox", style = MaterialTheme.typography.headlineMedium)
        }
        if (tasks.isEmpty()) {
            item {
                Text(
                    "Inbox zero! Nice work.",
                    style = MaterialTheme.typography.bodyLarge,
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
