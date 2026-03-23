package app.tsosu.data.markdown.index

import app.tsosu.data.markdown.MarkdownTaskSerializer
import app.tsosu.domain.model.Task
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class TaskIndexGenerator {

    private val serializer = MarkdownTaskSerializer()

    fun generate(
        tasks: List<Task>,
        projectNames: Map<String, String>,
        noteFilenames: Map<String, String>,
    ): String = buildString {
        appendFrontmatter()
        appendLine()

        val grouped = tasks.groupBy { it.projectId }

        // Always emit Inbox section first
        appendLine("## Inbox")
        grouped[null]
            ?.sortedBy { it.position }
            ?.forEach { appendLine(formatIndexTask(it, noteFilenames)) }
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
                    .forEach { appendLine(formatIndexTask(it, noteFilenames)) }
                appendLine()
            }
    }

    private fun formatIndexTask(task: Task, noteFilenames: Map<String, String>): String {
        val baseLine = serializer.formatTask(task)
        val slug = noteFilenames[task.id] ?: return baseLine

        // Insert wikilink before the id comment
        val idComment = "<!-- id:${task.id} -->"
        return baseLine.replace(idComment, "[[tasks/$slug]] $idComment")
    }

    private fun StringBuilder.appendFrontmatter() {
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.UTC)
        appendLine("---")
        appendLine("tsosu: v1")
        appendLine("updated: $now")
        appendLine("generated: true")
        append("---")
    }
}
