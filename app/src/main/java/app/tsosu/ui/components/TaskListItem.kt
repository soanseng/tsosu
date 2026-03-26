package app.tsosu.ui.components

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.tsosu.R
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.ui.util.UxHintPreferences
import app.tsosu.ui.util.rememberHaptic
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskListItem(
    task: Task,
    onToggleDone: (String) -> Unit,
    onStatusChange: (String, TaskStatus) -> Unit = { _, _ -> },
    onClick: (Task) -> Unit,
    onPostpone: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHaptic()

    if (task.status.isTerminal || onPostpone == null) {
        TaskListItemContent(
            task = task,
            onToggleDone = onToggleDone,
            onStatusChange = onStatusChange,
            onClick = onClick,
            modifier = modifier,
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.confirm()
                    onToggleDone(task.id)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.confirm()
                    onPostpone(task.id)
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF9800)
                    else -> Color.Transparent
                },
                label = "swipeBg",
            )
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Done
                else -> Icons.Default.Schedule
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                Icon(
                    icon,
                    contentDescription = when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> stringResource(R.string.swipe_done)
                        else -> stringResource(R.string.swipe_postpone)
                    },
                    tint = Color.White,
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        TaskListItemContent(
            task = task,
            onToggleDone = onToggleDone,
            onStatusChange = onStatusChange,
            onClick = onClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskListItemContent(
    task: Task,
    onToggleDone: (String) -> Unit,
    onStatusChange: (String, TaskStatus) -> Unit,
    onClick: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHaptic()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uxHintPreferences = remember { UxHintPreferences(context) }
    val hintShown by uxHintPreferences.statusLongPressHintShown.collectAsState(initial = true)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showStatusMenu by remember { mutableStateOf(false) }

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 16.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cornerRadius",
    )

    val targetColor = when {
        task.status.isTerminal -> MaterialTheme.colorScheme.secondaryContainer
        task.priority != Priority.NONE -> Color(task.priority.color).copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val containerColor by animateColorAsState(
        targetValue = targetColor,
        label = "containerColor",
    )

    val overdue = isOverdue(task)
    val todayStr = stringResource(R.string.detail_today)
    val tomorrowStr = stringResource(R.string.detail_tomorrow)
    val noDateStr = stringResource(R.string.detail_no_date)

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
            StatusIconButton(
                status = task.status,
                onTap = {
                    if (!task.done) haptic.confirm() else haptic.tick()
                    onToggleDone(task.id)
                    if (!hintShown) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.hint_long_press_status),
                            Toast.LENGTH_SHORT,
                        ).show()
                        scope.launch {
                            uxHintPreferences.markStatusLongPressHintShown()
                        }
                    }
                },
                onLongPress = {
                    haptic.longPress()
                    showStatusMenu = true
                },
            )

            DropdownMenu(
                expanded = showStatusMenu,
                onDismissRequest = { showStatusMenu = false },
            ) {
                TaskStatus.entries.forEach { status ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = status.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = status.iconTint(),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(status.displayName())
                            }
                        },
                        onClick = {
                            haptic.tick()
                            showStatusMenu = false
                            onStatusChange(task.id, status)
                        },
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (task.status.isTerminal) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (overdue) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.detail_overdue),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.error,
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
                val details = buildDetailString(task, todayStr, tomorrowStr, noDateStr)
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatusIconButton(
    status: TaskStatus,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics { role = Role.Button }
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = status.icon(),
            contentDescription = status.displayName(),
            tint = status.iconTint(),
            modifier = Modifier.size(24.dp),
        )
    }
}

fun TaskStatus.icon(): ImageVector = when (this) {
    TaskStatus.TODO -> Icons.Default.RadioButtonUnchecked
    TaskStatus.IN_PROGRESS -> Icons.Default.HourglassTop
    TaskStatus.ON_HOLD -> Icons.Default.PauseCircle
    TaskStatus.PLANNED -> Icons.Default.Schedule
    TaskStatus.DONE -> Icons.Default.CheckCircle
    TaskStatus.CANCELLED -> Icons.Default.Cancel
}

@Composable
fun TaskStatus.iconTint(): Color = when (this) {
    TaskStatus.TODO -> MaterialTheme.colorScheme.onSurfaceVariant
    TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
    TaskStatus.ON_HOLD -> MaterialTheme.colorScheme.tertiary
    TaskStatus.PLANNED -> MaterialTheme.colorScheme.secondary
    TaskStatus.DONE -> MaterialTheme.colorScheme.primary
    TaskStatus.CANCELLED -> MaterialTheme.colorScheme.error
}

@Composable
fun TaskStatus.displayName(): String = when (this) {
    TaskStatus.TODO -> stringResource(R.string.status_todo)
    TaskStatus.IN_PROGRESS -> stringResource(R.string.status_in_progress)
    TaskStatus.ON_HOLD -> stringResource(R.string.status_on_hold)
    TaskStatus.PLANNED -> stringResource(R.string.status_planned)
    TaskStatus.DONE -> stringResource(R.string.status_done)
    TaskStatus.CANCELLED -> stringResource(R.string.status_cancelled)
}

private fun buildDetailString(
    task: Task,
    todayStr: String,
    tomorrowStr: String,
    noDateStr: String,
): String {
    val parts = mutableListOf<String>()
    task.dueDate?.let { due ->
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val today = now.date
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        val dueDate = due.date
        parts += when (dueDate) {
            today -> todayStr
            tomorrow -> tomorrowStr
            else -> "${due.monthNumber}/${due.dayOfMonth}"
        }
    } ?: run {
        parts += noDateStr
    }
    task.estimatedMinutes?.let { min ->
        parts += "${min}m"
    }
    return parts.joinToString(" \u00B7 ")
}

private fun isOverdue(task: Task): Boolean {
    val due = task.dueDate ?: return false
    if (task.status.isTerminal) return false
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return due < now
}
