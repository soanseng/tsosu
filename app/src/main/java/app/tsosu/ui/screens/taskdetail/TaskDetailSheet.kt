package app.tsosu.ui.screens.taskdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.ui.components.RecurrencePicker
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.model.TaskStatus
import app.tsosu.ui.components.displayName
import app.tsosu.ui.components.icon
import app.tsosu.ui.components.iconTint
import app.tsosu.ui.util.rememberHaptic
import app.tsosu.ui.util.localizedLabel
import app.tsosu.ui.util.localizedName
import app.tsosu.ui.util.extractLinks
import app.tsosu.ui.util.recurrenceDisplayLabel
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

private enum class DatePickerTarget {
    DUE,
    SCHEDULED,
    START,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    taskId: String,
    onDismiss: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = rememberHaptic()

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDismiss()
    }

    if (state.task == null) return

    var datePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showNewCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    if (isEditing) R.string.task_detail_title else R.string.task_detail_view_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            if (isEditing) {
                TextButton(
                    onClick = {
                        // Leaving without saving: re-read the stored task so the
                        // overview never shows discarded edits.
                        viewModel.loadTask(taskId)
                        isEditing = false
                    },
                ) {
                    Text(stringResource(R.string.task_detail_cancel))
                }
            } else {
                TextButton(onClick = { isEditing = true }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.task_edit))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!isEditing) {
            TaskOverview(
                state = state,
                onOpenLink = { url -> runCatching { uriHandler.openUri(url) } },
            )
            CompletionHistorySection(state.completions)
            Spacer(Modifier.height(16.dp))
            return@Column
        }

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text(stringResource(R.string.task_detail_field_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text(stringResource(R.string.task_detail_field_description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )

        Spacer(Modifier.height(12.dp))

        // Category (backed by projects — one per task)
        Text(stringResource(R.string.task_detail_category), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = state.projectId == null,
                onClick = {
                    haptic.tick()
                    viewModel.onCategoryChange(null)
                },
                label = { Text(stringResource(R.string.category_none)) },
            )
            state.projects.forEach { project ->
                FilterChip(
                    selected = state.projectId == project.id,
                    onClick = {
                        haptic.tick()
                        viewModel.onCategoryChange(project.id)
                    },
                    label = { Text(project.title) },
                )
            }
            AssistChip(
                onClick = { showNewCategory = true },
                label = { Text(stringResource(R.string.category_new)) },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Status chips
        Text(stringResource(R.string.task_detail_status), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TaskStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.status == status,
                    onClick = {
                        haptic.tick()
                        viewModel.onStatusChange(status)
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = status.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (state.status == status) {
                                    status.iconTint()
                                } else {
                                    Color.Unspecified
                                },
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(status.displayName())
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.task_detail_priority), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Priority.entries.forEach { p ->
                FilterChip(
                    selected = state.priority == p,
                    onClick = {
                        haptic.tick()
                        viewModel.onPriorityChange(p)
                    },
                    label = {
                        Text(
                            text = p.localizedName(),
                            color = if (state.priority == p) Color(p.color) else Color.Unspecified,
                        )
                    },
                )
            }
        }


        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.task_detail_time_estimate), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 5, 15, 30, 60).forEach { minutes ->
                FilterChip(
                    selected = state.estimatedMinutes == minutes,
                    onClick = {
                        haptic.tick()
                        viewModel.onEstimatedMinutesChange(minutes)
                    },
                    label = { Text(if (minutes == 0) stringResource(R.string.task_detail_time_none) else "${minutes}m") },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Due date
        // Date: the single date on a task — "Next" when it recurs
        val dateLabel = if (state.recurrenceRule == null) {
            R.string.task_detail_due_date
        } else {
            R.string.task_detail_next_due
        }
        Text(stringResource(dateLabel), style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { datePickerTarget = DatePickerTarget.DUE }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    state.dueDate?.let { "${it.monthNumber}/${it.dayOfMonth}/${it.year}" }
                        ?: stringResource(R.string.task_detail_no_date),
                )
            }
            if (state.dueDate != null) {
                IconButton(onClick = { viewModel.onDueDateChange(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_detail_clear_date))
                }
            }
        }

        // Scheduled / start dates (Obsidian ⏳ / 🛫 parity)
        if (state.scheduledDate != null || state.startDate != null) {
            Text(stringResource(R.string.task_detail_scheduled_date), style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { datePickerTarget = DatePickerTarget.SCHEDULED }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.padding(start = 4.dp))
                    Text(
                        state.scheduledDate?.let { "${it.monthNumber}/${it.dayOfMonth}/${it.year}" }
                            ?: stringResource(R.string.task_detail_no_date),
                    )
                }
                IconButton(onClick = { viewModel.onScheduledDateChange(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_detail_clear_date))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.task_detail_start_date), style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { datePickerTarget = DatePickerTarget.START }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.padding(start = 4.dp))
                    Text(
                        state.startDate?.let { "${it.monthNumber}/${it.dayOfMonth}/${it.year}" }
                            ?: stringResource(R.string.task_detail_no_date),
                    )
                }
                IconButton(onClick = { viewModel.onStartDateChange(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_detail_clear_date))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Depends on (Obsidian ⛔) — display only; ids reference other tasks
        if (state.dependsOn.isNotEmpty()) {
            Text(stringResource(R.string.task_detail_depends_on), style = MaterialTheme.typography.labelLarge)
            Text(
                "⛔ " + state.dependsOn.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))

        // Recurrence
        Text(stringResource(R.string.quick_add_recurrence), style = MaterialTheme.typography.labelLarge)
        RecurrencePicker(
            rrule = state.recurrenceRule,
            onRruleChange = viewModel::onRecurrenceRuleChange,
        )

        // Routine time: which Habits-tab group this repeating task belongs to.
        if (state.recurrenceRule != null) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.quick_add_routine), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RoutineTime.entries.forEach { time ->
                    FilterChip(
                        selected = state.routineTime == time,
                        onClick = {
                            haptic.tick()
                            viewModel.onRoutineTimeChange(time)
                        },
                        label = { Text(time.localizedLabel()) },
                    )
                }
                FilterChip(
                    selected = state.routineTime == null,
                    onClick = {
                        haptic.tick()
                        viewModel.onRoutineTimeChange(null)
                    },
                    label = { Text(stringResource(R.string.habits_other)) },
                )
            }
        }

        // Completion history: how many times and when (compact past 5).
        CompletionHistorySection(state.completions)

        Spacer(Modifier.height(8.dp))

        // Reminder time
        Text(stringResource(R.string.task_detail_reminder_time), style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.AccessTime, contentDescription = null)
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    state.reminderTime?.let {
                        "%02d:%02d".format(it.hour, it.minute)
                    } ?: stringResource(R.string.task_detail_no_reminder),
                )
            }
            if (state.reminderTime != null) {
                IconButton(onClick = { viewModel.onReminderTimeChange(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.task_detail_clear_time))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.save() },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.title.isNotBlank(),
        ) {
            Text(stringResource(R.string.task_detail_save))
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.padding(start = 4.dp))
            Text(stringResource(R.string.task_detail_delete))
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showNewCategory) {
        AlertDialog(
            onDismissRequest = { showNewCategory = false },
            title = { Text(stringResource(R.string.category_new)) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(stringResource(R.string.category_name_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        viewModel.createCategory(newCategoryName)
                        newCategoryName = ""
                        showNewCategory = false
                    },
                ) {
                    Text(stringResource(R.string.category_create))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newCategoryName = ""
                    showNewCategory = false
                }) {
                    Text(stringResource(R.string.task_detail_cancel))
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.task_detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.task_detail_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.reject()
                    viewModel.delete()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.task_detail_delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.task_detail_cancel))
                }
            },
        )
    }

    // Date picker dialog (shared for due/scheduled/start)
    if (datePickerTarget != null) {
        val initialMillis = when (datePickerTarget) {
            DatePickerTarget.DUE -> state.dueDate
            DatePickerTarget.SCHEDULED -> state.scheduledDate
            DatePickerTarget.START -> state.startDate
            null -> null
        }?.let {
            it.date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
        )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val ldt = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                        when (datePickerTarget) {
                            DatePickerTarget.DUE -> viewModel.onDueDateChange(ldt)
                            DatePickerTarget.SCHEDULED -> viewModel.onScheduledDateChange(ldt)
                            DatePickerTarget.START -> viewModel.onStartDateChange(ldt)
                            null -> {}
                        }
                    }
                    datePickerTarget = null
                }) {
                    Text(stringResource(R.string.task_detail_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerTarget = null }) {
                    Text(stringResource(R.string.task_detail_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog for reminder
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.reminderTime?.hour ?: 9,
            initialMinute = state.reminderTime?.minute ?: 0,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.task_detail_reminder_time)) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onReminderTimeChange(
                        LocalTime(timePickerState.hour, timePickerState.minute),
                    )
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.task_detail_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.task_detail_cancel))
                }
            },
        )
    }
}

/**
 * Read-only summary shown before any editing: status, the fields that matter,
 * the description, and every link found in that description.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskOverview(
    state: TaskDetailState,
    onOpenLink: (String) -> Unit,
) {
    Text(
        text = state.title,
        style = MaterialTheme.typography.headlineSmall,
        textDecoration = if (state.status.isTerminal) TextDecoration.LineThrough else null,
    )

    Spacer(Modifier.height(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = state.status.icon(),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = state.status.iconTint(),
        )
        Spacer(Modifier.width(6.dp))
        Text(state.status.displayName(), style = MaterialTheme.typography.bodyMedium)
        if (state.priority != Priority.NONE) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.priority.localizedName(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(state.priority.color),
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    val categoryName = state.projects.firstOrNull { it.id == state.projectId }?.title
        ?: stringResource(R.string.category_none)
    OverviewRow(R.string.task_detail_category, categoryName)
    state.dueDate?.let { due ->
        OverviewRow(
            labelRes = if (state.recurrenceRule == null) {
                R.string.task_detail_due_date
            } else {
                R.string.task_detail_next_due
            },
            value = "${due.year}/${due.monthNumber}/${due.dayOfMonth}",
        )
    }
    state.recurrenceRule?.let {
        OverviewRow(R.string.quick_add_recurrence, recurrenceDisplayLabel(it))
    }
    state.routineTime?.let {
        OverviewRow(R.string.quick_add_routine, it.localizedLabel())
    }
    state.tinyVersion?.takeIf { it.isNotBlank() }?.let {
        OverviewRow(R.string.quick_add_tiny_version, it)
    }
    state.reminderTime?.let {
        OverviewRow(R.string.task_detail_reminder_time, "%02d:%02d".format(it.hour, it.minute))
    }
    if (state.estimatedMinutes > 0) {
        OverviewRow(R.string.task_detail_time_estimate, "${state.estimatedMinutes}m")
    }
    if (state.dependsOn.isNotEmpty()) {
        OverviewRow(R.string.task_detail_depends_on, state.dependsOn.joinToString(", "))
    }

    Spacer(Modifier.height(12.dp))

    Text(
        stringResource(R.string.task_detail_field_description),
        style = MaterialTheme.typography.labelLarge,
    )
    Text(
        text = state.description.ifBlank { stringResource(R.string.task_detail_no_description) },
        style = MaterialTheme.typography.bodyMedium,
        color = if (state.description.isBlank()) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )

    val links = remember(state.description) { extractLinks(state.description) }
    if (links.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.task_detail_links),
            style = MaterialTheme.typography.labelLarge,
        )
        links.forEach { link ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenLink(link.url) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = stringResource(R.string.task_detail_link_open),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = link.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (link.label != link.url) {
                        Text(
                            text = link.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewRow(labelRes: Int, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(84.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

/** How many times, and when — compact past five, as before. */
@Composable
private fun CompletionHistorySection(completions: List<LocalDate>) {
    if (completions.isEmpty()) return
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.task_detail_history),
        style = MaterialTheme.typography.labelLarge,
    )
    Text(
        stringResource(R.string.task_detail_history_count, completions.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    var historyExpanded by remember { mutableStateOf(false) }
    val sortedHistory = completions.sortedDescending()
    val shownHistory = if (historyExpanded) sortedHistory else sortedHistory.take(5)
    shownHistory.forEach { date ->
        Text(
            "${date.year}/${date.monthNumber}/${date.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (sortedHistory.size > 5) {
        TextButton(onClick = { historyExpanded = !historyExpanded }) {
            Text(
                stringResource(
                    if (historyExpanded) {
                        R.string.task_detail_history_collapse
                    } else {
                        R.string.task_detail_history_expand
                    },
                    sortedHistory.size,
                ),
            )
        }
    }
}
