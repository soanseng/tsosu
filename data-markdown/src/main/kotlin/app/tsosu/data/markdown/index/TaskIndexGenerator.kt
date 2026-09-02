package app.tsosu.data.markdown.index

import app.tsosu.data.markdown.MarkdownTaskSerializer
import app.tsosu.domain.model.Task

class TaskIndexGenerator {

    private val serializer = MarkdownTaskSerializer()

    fun generate(
        tasks: List<Task>,
        projectNames: Map<String, String>,
        noteFilenames: Map<String, String>,
        conflictIds: Set<String> = emptySet(),
    ): String = buildString {
        appendFrontmatter()
        appendLine()

        val grouped = tasks.groupBy { it.projectId }

        // Always emit Inbox section first
        appendLine("## Inbox")
        grouped[null]
            ?.sortedBy { it.position }
            ?.forEach {
                appendIndexTaskWithHistory(it, noteFilenames, conflictIds)
            }
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
                    .forEach {
                        appendIndexTaskWithHistory(it, noteFilenames, conflictIds)
                    }
                appendLine()
            }
    }

    private fun formatIndexTask(
        task: Task,
        noteFilenames: Map<String, String>,
        conflictIds: Set<String>,
    ): String {
        val conflictMarker = if (task.id in conflictIds) " <!-- conflict -->" else ""
        val line = serializer.formatTask(task) + conflictMarker

        // Wikilink to the per-task note goes into the description, right
        // before the 🆔 field.
        val slug = noteFilenames[task.id] ?: return line
        return line.replaceFirst(" \uD83C\uDD94 ", " [[tasks/$slug]] \uD83C\uDD94 ")
    }

    /**
     * Obsidian keeps one DONE line per completed occurrence of a recurring
     * task; Tsosu's single row would otherwise erase that history on every
     * push. Emit the most recent [HISTORY_LINES] completions as DONE lines
     * above the active line; the importer folds them back idempotently.
     */
    private fun StringBuilder.appendIndexTaskWithHistory(
        task: Task,
        noteFilenames: Map<String, String>,
        conflictIds: Set<String>,
    ) {
        if (task.recurrenceRule != null) {
            task.completions
                .sortedDescending()
                .take(HISTORY_LINES)
                .forEach { date ->
                    val done = task.copy(
                        status = app.tsosu.domain.model.TaskStatus.DONE,
                        completedDate = kotlinx.datetime.LocalDateTime(
                            date,
                            kotlinx.datetime.LocalTime(0, 0),
                        ),
                    )
                    appendLine(serializer.formatTask(done))
                }
        }
        appendLine(formatIndexTask(task, noteFilenames, conflictIds))
    }

    private companion object {
        const val HISTORY_LINES = 10
    }

    private fun StringBuilder.appendFrontmatter() {
        appendLine("---")
        appendLine("tsosu: v1")
        appendLine("generated: true")
        append("---")
    }
}
