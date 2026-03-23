package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MarkdownTaskSerializer {

    fun serialize(
        tasks: List<Task>,
        projectNames: Map<String, String> = emptyMap(),
    ): String = buildString {
        appendFrontmatter()
        appendLine()

        val grouped = tasks.groupBy { it.projectId }

        // Always emit Inbox section first
        appendLine("## Inbox")
        grouped[null]
            ?.sortedBy { it.position }
            ?.forEach { appendLine(formatTask(it)) }
        appendLine()

        // Emit named project sections in alphabetical order
        grouped
            .filterKeys { it != null }
            .entries
            .sortedBy { (projectId, _) -> projectNames[projectId] ?: projectId }
            .forEach { (projectId, projectTasks) ->
                val sectionName = projectNames[projectId] ?: projectId
                appendLine("## $sectionName")
                projectTasks
                    .sortedBy { it.position }
                    .forEach { appendLine(formatTask(it)) }
                appendLine()
            }
    }

    internal fun formatTask(task: Task): String = buildString {
        // Checkbox with extended status marker
        append("- [${task.status.checkboxMarker}] ")

        // Title
        append(task.title)

        // Completion date (only for DONE tasks)
        if (task.status == TaskStatus.DONE) {
            val localDate = (task.completedDate?.date
                ?: task.updatedAt.toLocalDateTime(TimeZone.UTC).date)
            append(" \u2705 $localDate")
        }

        // Cancelled date (only for CANCELLED tasks)
        if (task.status == TaskStatus.CANCELLED && task.cancelledDate != null) {
            append(" \u274C ${task.cancelledDate!!.date}")
        }

        // Due date
        if (task.dueDate != null) {
            append(" \uD83D\uDCC5 ${task.dueDate!!.date}")
        }

        // Scheduled date
        if (task.scheduledDate != null) {
            append(" \u23F3 ${task.scheduledDate!!.date}")
        }

        // Start date
        if (task.startDate != null) {
            append(" \uD83D\uDEEB ${task.startDate!!.date}")
        }

        // Created date (derived from createdAt)
        val createdDate = task.createdAt
            .toLocalDateTime(TimeZone.UTC)
            .date
        append(" \u2795 $createdDate")

        // Reminder time
        if (task.reminderTime != null) {
            val h = task.reminderTime!!.hour.toString().padStart(2, '0')
            val m = task.reminderTime!!.minute.toString().padStart(2, '0')
            append(" \u23F0 $h:$m")
        }

        // Recurrence rule
        if (task.recurrenceRule != null) {
            append(" \uD83D\uDD01 ${task.recurrenceRule}")
        }

        // Energy level (always emitted)
        when (task.energyLevel) {
            EnergyLevel.HIGH -> append(" \u26A1high")
            EnergyLevel.MEDIUM -> append(" \uD83D\uDE10medium")
            EnergyLevel.LOW -> append(" \uD83E\uDEABlow")
        }

        // Estimated minutes
        if (task.estimatedMinutes != null) {
            append(" \uD83C\uDF45 ${task.estimatedMinutes}m")
        }

        // Priority (NONE is omitted)
        if (task.priority != Priority.NONE) {
            append(" ${task.priority.emoji}")
        }

        // Hidden ID
        append(" <!-- id:${task.id} -->")
    }

    private fun StringBuilder.appendFrontmatter() {
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.UTC)
        appendLine("---")
        appendLine("tsosu: v1")
        appendLine("updated: $now")
        append("---")
    }
}
