package app.tsosu.ui.screens.kanban

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun KanbanScreen(
    viewModel: KanbanViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        GroupByTabRow(
            selectedGroupBy = state.groupBy,
            onGroupBySelected = viewModel::setGroupBy,
        )

        KanbanBoard(
            columns = state.columns,
            onTaskClick = onTaskClick,
        )
    }
}

@Composable
private fun GroupByTabRow(
    selectedGroupBy: GroupBy,
    onGroupBySelected: (GroupBy) -> Unit,
) {
    val selectedIndex = GroupBy.entries.indexOf(selectedGroupBy)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 16.dp,
    ) {
        GroupBy.entries.forEach { group ->
            Tab(
                selected = group == selectedGroupBy,
                onClick = { onGroupBySelected(group) },
                text = { Text(group.label) },
            )
        }
    }
}

@Composable
private fun KanbanBoard(
    columns: List<KanbanColumnData>,
    onTaskClick: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(columns, key = { it.title }) { column ->
            KanbanColumn(
                column = column,
                onTaskClick = onTaskClick,
            )
        }
    }
}

@Composable
private fun KanbanColumn(
    column: KanbanColumnData,
    onTaskClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = column.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${column.tasks.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(column.tasks, key = { it.id }) { task ->
                    KanbanCard(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun KanbanCard(
    task: Task,
    onClick: () -> Unit,
) {
    val containerColor = if (task.priority != Priority.NONE) {
        Color(task.priority.color).copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            if (task.priority != Priority.NONE) {
                Text(
                    text = task.priority.emoji,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val details = buildKanbanDetailString(task)
            if (details.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = details,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverdue(task)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

private fun buildKanbanDetailString(task: Task): String {
    val parts = mutableListOf<String>()
    task.dueDate?.let { due ->
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val today = now.date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        val dueDate = due.date
        parts += when (dueDate) {
            today -> "Today"
            tomorrow -> "Tomorrow"
            else -> "${due.monthNumber}/${due.dayOfMonth}"
        }
    }
    task.estimatedMinutes?.let { min ->
        parts += "${min}m"
    }
    parts += task.energyLevel.emoji
    return parts.joinToString(" \u00B7 ")
}

private fun isOverdue(task: Task): Boolean {
    val due = task.dueDate ?: return false
    if (task.status.isTerminal) return false
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return due < now
}
