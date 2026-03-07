package app.tsosu.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.ui.util.rememberHaptic
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun TaskListItem(
    task: Task,
    onToggleDone: (String) -> Unit,
    onClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 16.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cornerRadius",
    )

    val targetColor = when {
        task.done -> MaterialTheme.colorScheme.secondaryContainer
        task.priority != Priority.NONE -> Color(task.priority.color).copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val containerColor by animateColorAsState(
        targetValue = targetColor,
        label = "containerColor",
    )

    Card(
        onClick = {
            haptic.tick()
            onClick(task)
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = {
                    if (!task.done) haptic.confirm() else haptic.tick()
                    onToggleDone(task.id)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                ),
            )

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

            Spacer(Modifier.width(4.dp))

            Text(
                text = task.energyLevel.emoji,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun buildDetailString(task: Task): String {
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
    } ?: run {
        parts += "No date"
    }
    task.estimatedMinutes?.let { min ->
        parts += "${min}m"
    }
    return parts.joinToString(" \u00B7 ")
}

private fun isOverdue(task: Task): Boolean {
    val due = task.dueDate ?: return false
    if (task.done) return false
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return due < now
}
