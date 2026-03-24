package app.tsosu.ui.screens.upcoming

import androidx.compose.foundation.ExperimentalFoundationApi
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
import app.tsosu.ui.components.TaskListItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UpcomingScreen(
    viewModel: UpcomingViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showKonfetti = remember { mutableStateOf(false) }

    KonfettiOverlay(showKonfetti)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (state.groups.isEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Nothing scheduled ahead.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Add due dates to your tasks to see them here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.groups.forEach { group ->
            stickyHeader(key = group.label) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(group.tasks, key = { it.id }) { task ->
                TaskListItem(
                    task = task,
                    onToggleDone = { id ->
                        viewModel.toggleDone(id)
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

        item { Spacer(Modifier.height(80.dp)) }
    }
}
