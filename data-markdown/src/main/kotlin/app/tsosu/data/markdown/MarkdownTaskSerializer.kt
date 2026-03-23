package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
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
        // Checkbox
        if (task.done) {
            append("- [x] ")
        } else {
            append("- [ ] ")
        }

        // Title
        append(task.title)

        // Completion date (only for done tasks, derived from updatedAt)
        if (task.done) {
            val localDate = task.updatedAt
                .toLocalDateTime(TimeZone.UTC)
                .date
            append(" \u2705 $localDate")
        }

        // Due date
        if (task.dueDate != null) {
            append(" \uD83D\uDCC5 ${task.dueDate!!.date}")
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
