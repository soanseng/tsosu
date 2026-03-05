package app.tsosu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TaskListItem(
    task: Task,
    onToggleDone: (String) -> Unit,
    onClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onClick(task) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (task.priority != Priority.NONE) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(task.priority.color)),
            )
            Spacer(Modifier.width(4.dp))
        }

        Checkbox(checked = task.done, onCheckedChange = { onToggleDone(task.id) })

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val details = buildDetailString(task)
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue(task)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }

        Text(
            text = task.energyLevel.emoji,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun buildDetailString(task: Task): String {
    val parts = mutableListOf<String>()
    task.dueDate?.let { due ->
        parts += "${due.monthNumber}/${due.dayOfMonth}"
    }
    task.estimatedMinutes?.let { min ->
        parts += "${min}m"
    }
    return parts.joinToString(" · ")
}

private fun isOverdue(task: Task): Boolean {
    val due = task.dueDate ?: return false
    if (task.done) return false
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return due < now
}
