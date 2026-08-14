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
            ?.forEach { appendLine(formatIndexTask(it, noteFilenames, conflictIds)) }
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
                    .forEach { appendLine(formatIndexTask(it, noteFilenames, conflictIds)) }
                appendLine()
            }
    }

    private fun formatIndexTask(
        task: Task,
        noteFilenames: Map<String, String>,
        conflictIds: Set<String>,
    ): String {
        val baseLine = serializer.formatTask(task)
        val conflictMarker = if (task.id in conflictIds) " <!-- conflict -->" else ""

        // Insert wikilink (and conflict marker) before the id comment
        val idComment = "<!-- id:${task.id} -->"
        val slug = noteFilenames[task.id] ?: return baseLine.replace(idComment, "$idComment$conflictMarker")
        return baseLine.replace(idComment, "[[tasks/$slug]] $idComment$conflictMarker")
    }

    private fun StringBuilder.appendFrontmatter() {
        appendLine("---")
        appendLine("tsosu: v1")
        appendLine("generated: true")
        append("---")
    }
}
