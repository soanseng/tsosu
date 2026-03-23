package app.tsosu.data.markdown.tasknote

import app.tsosu.data.markdown.YamlFrontmatterParser
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime

/**
 * Parses a TaskNote markdown file into a [ParsedTaskNote].
 *
 * Stub implementation — will be completed in Task 3.
 */
class TaskNoteParser {

    data class ParsedTaskNote(
        val task: Task,
        val projectName: String?,
    )

    private val yamlHelper = YamlFrontmatterParser()

    fun parse(content: String): ParsedTaskNote {
        val doc = yamlHelper.parse(content)
        val fm = doc.frontmatter

        val bodyLines = doc.body.trim().lines()
        val title = bodyLines.firstOrNull()?.removePrefix("# ")?.trim() ?: ""
        val description = bodyLines.drop(1).joinToString("\n").trim()

        val status = fm["status"]?.let { s ->
            TaskStatus.entries.firstOrNull { it.name.equals(s, ignoreCase = true) }
        } ?: TaskStatus.TODO

        val priority = fm["priority"]?.let { p ->
            Priority.entries.firstOrNull { it.name.equals(p, ignoreCase = true) }
        } ?: Priority.NONE

        val energy = fm["energy"]?.let { e ->
            EnergyLevel.entries.firstOrNull { it.name.equals(e, ignoreCase = true) }
        } ?: EnergyLevel.MEDIUM

        val dueDate = fm["due"]?.let { LocalDate.parse(it).atTime(0, 0) }
        val scheduledDate = fm["scheduled"]?.let { LocalDate.parse(it).atTime(0, 0) }
        val startDate = fm["start"]?.let { LocalDate.parse(it).atTime(0, 0) }
        val completedDate = fm["completed"]?.let { LocalDate.parse(it).atTime(0, 0) }
        val cancelledDate = fm["cancelled"]?.let { LocalDate.parse(it).atTime(0, 0) }
        val reminderTime = fm["reminder"]?.let { r ->
            val parts = r.split(":")
            if (parts.size == 2) LocalTime(parts[0].toInt(), parts[1].toInt()) else null
        }

        val estimatedMinutes = fm["estimate"]?.removeSuffix("m")?.toIntOrNull()
        val createdDate = fm["created"]?.let { LocalDate.parse(it) }
        val createdAt = createdDate?.atStartOfDayIn(TimeZone.UTC)
            ?: kotlinx.datetime.Clock.System.now()

        val task = Task(
            id = fm["id"] ?: "",
            title = title,
            description = description,
            status = status,
            priority = priority,
            dueDate = dueDate,
            scheduledDate = scheduledDate,
            startDate = startDate,
            reminderTime = reminderTime,
            completedDate = completedDate,
            cancelledDate = cancelledDate,
            energyLevel = energy,
            estimatedMinutes = estimatedMinutes,
            recurrenceRule = fm["recurrence"],
            createdAt = createdAt,
            updatedAt = createdAt,
        )

        return ParsedTaskNote(
            task = task,
            projectName = fm["project"],
        )
    }
}
