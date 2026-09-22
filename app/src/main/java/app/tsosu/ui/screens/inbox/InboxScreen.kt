package app.tsosu.ui.screens.inbox

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateMapOf
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
    isVaultConfigured: Boolean = true,
    onSelectFolder: () -> Unit = {},
)
{
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val groups by viewModel.uiState.collectAsStateWithLifecycle()
    val staleIds by viewModel.staleIds.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    var selectionMode by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Categories start folded: the loose list is what needs triage today.
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.bulk_delete_confirm_title)) },
            text = { Text(stringResource(R.string.bulk_delete_confirm_body, selectedIds.size)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmDelete = false
                    viewModel.bulkDelete()
                }) {
                    Text(stringResource(R.string.bulk_delete))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.bulk_cancel))
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectionMode) {
            // Bulk action bar
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.bulk_selected_count, selectedIds.size),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        OutlinedButton(onClick = { viewModel.bulkComplete() }) {
                            Text(stringResource(R.string.bulk_complete))
                        }
                        OutlinedButton(onClick = { viewModel.bulkSomeday() }) {
                            Text(stringResource(R.string.bulk_someday))
                        }
                        OutlinedButton(onClick = { confirmDelete = true }) {
                            Text(stringResource(R.string.bulk_delete))
                        }
                        Button(onClick = {
                            selectionMode = false
                            viewModel.clearSelection()
                        }) {
                            Text(stringResource(R.string.bulk_cancel))
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!isVaultConfigured) {
                item(key = "vault-setup") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = stringResource(R.string.vault_setup_title),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    stringResource(R.string.vault_setup_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.vault_setup_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onSelectFolder) {
                                Text(stringResource(R.string.settings_select_folder))
                            }
                        }
                    }
                }
            }
            item {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.inbox_title), style = MaterialTheme.typography.headlineMedium)
                    TextButton(onClick = {
                        if (selectionMode) {
                            viewModel.clearSelection()
                        }
                        selectionMode = !selectionMode
                    }) {
                        Text(
                            if (selectionMode) {
                                stringResource(R.string.bulk_cancel)
                            } else {
                                stringResource(R.string.bulk_select)
                            },
                        )
                    }
                }
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
                        text = stringResource(R.string.inbox_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.inbox_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(groups.uncategorized, key = { it.id }) { task ->
                InboxTaskRow(
                    task = task,
                    selectionMode = selectionMode,
                    selected = task.id in selectedIds,
                    onToggleSelection = viewModel::toggleSelection,
                    onToggleDone = viewModel::toggleDone,
                    onStatusChange = viewModel::setStatus,
                    onTaskClick = onTaskClick,
                )
            }

            // Filed-but-undated tasks stay in the inbox, folded under their
            // category so the loose list stays the star of the screen.
            groups.categories.forEach { group ->
                val expanded = expandedCategories[group.project.id] == true
                item(key = "fold-${group.project.id}") {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedCategories[group.project.id] = !expanded
                            }
                            .padding(top = 10.dp, bottom = 4.dp),
                    ) {
                        Text(
                            text = "📁 ${group.project.title}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = group.tasks.size.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = if (expanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = stringResource(R.string.task_view),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (expanded) {
                    items(group.tasks, key = { it.id }) { task ->
                        InboxTaskRow(
                            task = task,
                            selectionMode = selectionMode,
                            selected = task.id in selectedIds,
                            onToggleSelection = viewModel::toggleSelection,
                            onToggleDone = viewModel::toggleDone,
                            onStatusChange = viewModel::setStatus,
                            onTaskClick = onTaskClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxTaskRow(
    task: app.tsosu.domain.model.Task,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: (String) -> Unit,
    onToggleDone: (String) -> Unit,
    onStatusChange: (String, app.tsosu.domain.model.TaskStatus) -> Unit,
    onTaskClick: (String) -> Unit,
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        if (selectionMode) {
            androidx.compose.material3.Checkbox(
                checked = selected,
                onCheckedChange = { onToggleSelection(task.id) },
            )
        }
        TaskListItem(
            task = task,
            onToggleDone = { if (!selectionMode) onToggleDone(it) },
            onStatusChange = { id, status -> if (!selectionMode) onStatusChange(id, status) },
            onClick = {
                if (selectionMode) {
                    onToggleSelection(task.id)
                } else {
                    onTaskClick(task.id)
                }
            },
            modifier = Modifier.weight(1f),
        )
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
