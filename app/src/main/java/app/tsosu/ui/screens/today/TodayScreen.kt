package app.tsosu.ui.screens.today

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.ui.components.KonfettiOverlay
import app.tsosu.ui.components.TaskListItem

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val konfettiTrigger = remember { mutableIntStateOf(0) }

    KonfettiOverlay(konfettiTrigger)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (state.overdue.isEmpty() && state.today.isEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.today_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.overdue.isNotEmpty()) {
            item(key = "overdue-header") {
                Text(
                    text = stringResource(R.string.today_overdue),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(state.overdue, key = { "overdue-${it.id}" }) { task ->
                TaskListItem(
                    task = task,
                    onToggleDone = { id ->
                        viewModel.toggleDone(id)
                        konfettiTrigger.intValue++
                    },
                    onStatusChange = { id, status ->
                        viewModel.setStatus(id, status)
                    },
                    onClick = { onTaskClick(it.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (state.today.isNotEmpty()) {
            item(key = "today-header") {
                Text(
                    text = stringResource(R.string.nav_today),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(state.today, key = { it.id }) { task ->
                TaskListItem(
                    task = task,
                    onToggleDone = { id ->
                        viewModel.toggleDone(id)
                        konfettiTrigger.intValue++
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
