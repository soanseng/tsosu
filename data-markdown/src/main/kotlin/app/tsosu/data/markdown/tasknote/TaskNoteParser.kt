package app.tsosu.data.markdown.tasknote

import app.tsosu.data.markdown.YamlFrontmatterParser
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.model.RoutineTime
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime

data class ParsedTaskNote(
    val task: Task,
    val projectName: String?,
)

class TaskNoteParser {

    private val yamlHelper = YamlFrontmatterParser()

    fun parse(content: String): ParsedTaskNote {
        val doc = yamlHelper.parse(content)
        val fm = doc.frontmatter

        val id = fm["id"] ?: error("TaskNote missing id")
        val status = fm["status"]?.let { parseStatus(it) } ?: TaskStatus.TODO
        val priority = fm["priority"]?.let { parsePriority(it) } ?: Priority.NONE
        val energy = fm["energy"]?.let { parseEnergy(it) } ?: EnergyLevel.MEDIUM

        // Extract title from first H1 heading in body
        val bodyLines = doc.body.trim().lines()
        val h1Idx = bodyLines.indexOfFirst { it.startsWith("# ") }
        val title = if (h1Idx >= 0) bodyLines[h1Idx].removePrefix("# ").trim() else "Untitled"
        val description = if (h1Idx >= 0) {
            bodyLines.drop(h1Idx + 1).joinToString("\n").trim()
        } else {
            doc.body.trim()
        }

        val task = Task(
            id = id,
            title = title,
            description = description,
            status = status,
            priority = priority,
            dueDate = fm["due"]?.let { LocalDate.parse(it).atTime(0, 0) },
            scheduledDate = fm["scheduled"]?.let { LocalDate.parse(it).atTime(0, 0) },
            startDate = fm["start"]?.let { LocalDate.parse(it).atTime(0, 0) },
            reminderTime = fm["reminder"]?.let { parseTime(it) },
            completedDate = fm["completed"]?.let { LocalDate.parse(it).atTime(0, 0) },
            cancelledDate = fm["cancelled"]?.let { LocalDate.parse(it).atTime(0, 0) },
            energyLevel = energy,
            recurrenceRule = fm["recurrence"],
            tinyVersion = fm["tiny"],
            routineTime = fm["routine"]?.let { parseRoutineTime(it) },
            completions = fm["completions"].orEmpty()
                .split(",")
                .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
                .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() },
            createdAt = fm["created"]?.let {
                LocalDate.parse(it).atStartOfDayIn(TimeZone.UTC)
            } ?: Clock.System.now(),
            updatedAt = Clock.System.now(),
        )

        return ParsedTaskNote(task, fm["project"])
    }

    private fun parseStatus(s: String): TaskStatus = when (s.lowercase()) {
        "todo" -> TaskStatus.TODO
        "in_progress", "in-progress" -> TaskStatus.IN_PROGRESS
        "on_hold", "on-hold" -> TaskStatus.ON_HOLD
        "planned" -> TaskStatus.PLANNED
        "done" -> TaskStatus.DONE
        "cancelled" -> TaskStatus.CANCELLED
        else -> TaskStatus.TODO
    }

    private fun parsePriority(s: String): Priority = when (s.lowercase()) {
        "urgent", "highest" -> Priority.URGENT
        "high" -> Priority.HIGH
        "medium" -> Priority.MEDIUM
        "low" -> Priority.LOW
        else -> Priority.NONE
    }

    private fun parseEnergy(s: String): EnergyLevel = when (s.lowercase()) {
        "high" -> EnergyLevel.HIGH
        "medium" -> EnergyLevel.MEDIUM
        "low" -> EnergyLevel.LOW
        else -> EnergyLevel.MEDIUM
    }

    private fun parseTime(s: String): LocalTime {
        val parts = s.split(":")
        return LocalTime(parts[0].toInt(), parts[1].toInt())
    }

    private fun parseRoutineTime(s: String): RoutineTime? = when (s.lowercase()) {
        "morning" -> RoutineTime.MORNING
        "afternoon" -> RoutineTime.AFTERNOON
        "evening" -> RoutineTime.EVENING
        else -> null
    }
}
