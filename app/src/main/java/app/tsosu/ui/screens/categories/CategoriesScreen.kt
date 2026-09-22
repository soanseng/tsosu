package app.tsosu.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.ui.components.KonfettiOverlay
import app.tsosu.ui.components.TaskListItem

/**
 * Categories view: every open task grouped by its project (category), with an
 * "uncategorized" bucket last. Groups render expanded — it is the one place
 * that answers "what is in this category?" without extra taps.
 */
@Composable
fun CategoriesScreen(
    onTaskClick: (String) -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CategoriesContent(
        state = state,
        onToggleDone = viewModel::toggleDone,
        onTaskClick = onTaskClick,
    )
}

/** Stateless body — also the surface the screenshot test renders. */
@Composable
fun CategoriesContent(
    state: CategoriesUiState,
    onToggleDone: (String) -> Unit,
    onTaskClick: (String) -> Unit,
) {
    val konfettiTrigger = remember { mutableIntStateOf(0) }

    KonfettiOverlay(konfettiTrigger)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.nav_categories),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        if (state.taskCount == 0) {
            item {
                Text(
                    text = stringResource(R.string.categories_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.groups.forEach { group ->
            val key = group.project?.id ?: UNCATEGORIZED_KEY
            item(key = "header-$key") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = "📁 " + (group.project?.title
                            ?: stringResource(R.string.category_uncategorized)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = group.tasks.size.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(group.tasks, key = { it.id }) { task ->
                TaskListItem(
                    task = task,
                    onToggleDone = { id ->
                        onToggleDone(id)
                        konfettiTrigger.intValue++
                    },
                    onClick = { onTaskClick(it.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

private const val UNCATEGORIZED_KEY = "uncategorized"
