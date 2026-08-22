package app.tsosu.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.R
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.SearchQueryParser
import app.tsosu.ui.components.TaskListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    data class SearchState(val query: String = "", val results: List<Task> = emptyList())

    val state = combine(query, query.debounce(200).flatMapLatest { q ->
        if (q.isBlank()) flowOf(emptyList()) else taskRepository.searchTasks(q)
    }) { q, results -> SearchState(q, results) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    fun onQueryChange(value: String) {
        query.value = value
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    onTaskClick: (Task) -> Unit,
    onToggleDone: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var hintDismissed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
        )
        Spacer(Modifier.height(8.dp))

        if (!hintDismissed && state.query.isBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("status:todo", "due:<=7d", "due:overdue", "#tag").forEach { hint ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.onQueryChange(hint) },
                        label = { Text(hint) },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        Text(
            stringResource(R.string.search_result_count, state.results.size),
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(4.dp))

        LazyColumn {
            items(state.results, key = { it.id }) { task ->
                TaskListItem(
                    task = task,
                    onToggleDone = onToggleDone,
                    onClick = onTaskClick,
                )
            }
        }
    }
}
