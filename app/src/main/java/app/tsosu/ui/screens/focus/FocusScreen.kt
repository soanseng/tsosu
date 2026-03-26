package app.tsosu.ui.screens.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.ui.components.KonfettiOverlay
import app.tsosu.ui.components.ProgressCard
import app.tsosu.ui.components.TaskListItem

@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {},
    isVaultConfigured: Boolean = true,
    onSelectFolder: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val konfettiTrigger = remember { mutableIntStateOf(0) }
    var noDateExpanded by rememberSaveable { mutableStateOf(false) }

    KonfettiOverlay(konfettiTrigger)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isVaultConfigured) {
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                stringResource(R.string.focus_vault_setup_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.focus_vault_setup_desc),
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
            Spacer(Modifier.height(8.dp))
            ProgressCard(
                completedCount = state.completedCount,
                totalCount = state.totalCount,
                totalMinutes = state.totalEstimatedMinutes,
                streakDays = 0,
            )
        }

        if (state.isFiltered) {
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Default.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.focus_filter_active),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (state.focusTasks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.focus_today), style = MaterialTheme.typography.titleMedium)
            }
            items(state.focusTasks, key = { it.id }) { task ->
                TaskListItem(
                    task = task,
                    onToggleDone = { id ->
                        viewModel.onToggleDone(id)
                        konfettiTrigger.intValue++
                    },
                    onStatusChange = { id, status ->
                        viewModel.setStatus(id, status)
                    },
                    onClick = { onTaskClick(it.id) },
                    onPostpone = { id -> viewModel.postponeTask(id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        val otherAndInbox = state.otherTasks + state.inboxTasks
        if (otherAndInbox.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { noDateExpanded = !noDateExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.focus_other_tasks),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${otherAndInbox.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .let { mod ->
                                mod.padding(horizontal = 8.dp, vertical = 2.dp)
                            },
                    )
                    Icon(
                        imageVector = if (noDateExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (noDateExpanded) {
                items(otherAndInbox, key = { it.id }) { task ->
                    TaskListItem(
                        task = task,
                        onToggleDone = { id ->
                            viewModel.onToggleDone(id)
                            konfettiTrigger.intValue++
                        },
                        onStatusChange = { id, status ->
                            viewModel.setStatus(id, status)
                        },
                        onClick = { onTaskClick(it.id) },
                        onPostpone = { id -> viewModel.postponeTask(id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        if (state.focusTasks.isEmpty() && state.otherTasks.isEmpty() && state.inboxTasks.isEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.focus_empty_no_tasks),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.focus_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}
