package app.tsosu.data.markdown.tasknote

import app.tsosu.data.markdown.YamlFrontmatterParser
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class TaskNoteSerializer {

    private val yamlHelper = YamlFrontmatterParser()

    fun serialize(task: Task, projectName: String? = null): String {
        val frontmatter = buildMap {
            put("id", task.id)
            put("status", task.status.name.lowercase())
            if (task.priority != Priority.NONE) {
                put("priority", task.priority.name.lowercase())
            }
            task.dueDate?.let { put("due", it.date.toString()) }
            task.scheduledDate?.let { put("scheduled", it.date.toString()) }
            task.startDate?.let { put("start", it.date.toString()) }
            task.reminderTime?.let {
                put(
                    "reminder",
                    "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}",
                )
            }
            put("energy", task.energyLevel.name.lowercase())
            task.estimatedMinutes?.let { put("estimate", "${it}m") }
            task.recurrenceRule?.let { put("recurrence", it) }
            projectName?.let { put("project", it) }
            task.completedDate?.let { put("completed", it.date.toString()) }
            task.cancelledDate?.let { put("cancelled", it.date.toString()) }
            put("created", task.createdAt.toLocalDateTime(TimeZone.UTC).date.toString())
        }

        val body = buildString {
            appendLine("# ${task.title}")
            if (task.description.isNotBlank()) {
                appendLine()
                append(task.description)
            }
        }

        return yamlHelper.serialize(frontmatter, body)
    }

    fun slugify(title: String): String {
        return title
            .lowercase()
            .replace(Regex("[^\\w\\s\\u4e00-\\u9fff\\u3040-\\u309f\\u30a0-\\u30ff-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }
}
