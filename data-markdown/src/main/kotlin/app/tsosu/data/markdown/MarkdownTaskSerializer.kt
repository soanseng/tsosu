package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.recurrence.RecurrenceParser
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

    /**
     * One line in Obsidian Tasks default emoji format (field order follows
     * their TaskLayoutComponent enum): description, 🆔, priority, 🔁,
     * ➕ created, 🛫 start, ⏳ scheduled, 📅 due, ❌ cancelled, ✅ done.
     * Tsosu-only trailers (⏰ energy 🍅) are never written; they live in
     * SQLite and in the YAML of the per-task notes under the tasks folder.
     */
    internal fun formatTask(task: Task): String = buildString {
        // Checkbox with extended status marker
        append("- [${task.status.checkboxMarker}] ")

        // Title (may carry #tags — Obsidian parses tags anywhere in the body)
        append(task.title)

        // Obsidian Tasks id
        append(" \uD83C\uDD94 ${task.id}")

        // Priority (NONE is omitted)
        if (task.priority != Priority.NONE) {
            append(" ${task.priority.emoji}")
        }

        // Recurrence as rrule.js English, never raw RRULE
        task.recurrenceRule?.let {
            append(" \uD83D\uDD01 ${RecurrenceParser.toObsidianText(it)}")
        }

        // Created date (always, derived from createdAt)
        val createdDate = task.createdAt
            .toLocalDateTime(TimeZone.UTC)
            .date
        append(" \u2795 $createdDate")

        // Start date
        if (task.startDate != null) {
            append(" \uD83D\uDEEB ${task.startDate!!.date}")
        }

        // Scheduled date
        if (task.scheduledDate != null) {
            append(" \u23F3 ${task.scheduledDate!!.date}")
        }

        // Due date
        if (task.dueDate != null) {
            append(" \uD83D\uDCC5 ${task.dueDate!!.date}")
        }

        // Cancelled date (only for CANCELLED tasks)
        if (task.status == TaskStatus.CANCELLED && task.cancelledDate != null) {
            append(" \u274C ${task.cancelledDate!!.date}")
        }

        // Completion date (only for DONE tasks)
        if (task.status == TaskStatus.DONE) {
            val localDate = (task.completedDate?.date
                ?: task.updatedAt.toLocalDateTime(TimeZone.UTC).date)
            append(" \u2705 $localDate")
        }
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
