package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ParsedTasks(
    val tasks: List<Task>,
    val projectSections: Map<String, String>, // taskId -> sectionName
)

class MarkdownTaskParser {

    private val taskLineRegex = Regex("""^- \[([ xX/!>\-])] (.+)$""")
    private val idRegex = Regex("""<!-- id:(\S+) -->""")
    private val dueDateRegex = Regex("""\uD83D\uDCC5 (\d{4}-\d{2}-\d{2})""")
    private val completionRegex = Regex("""\u2705 (\d{4}-\d{2}-\d{2})""")
    private val cancelledDateRegex = Regex("""\u274C (\d{4}-\d{2}-\d{2})""")
    private val scheduledDateRegex = Regex("""\u23F3 (\d{4}-\d{2}-\d{2})""")
    private val startDateRegex = Regex("""\uD83D\uDEEB (\d{4}-\d{2}-\d{2})""")
    private val createdDateRegex = Regex("""\u2795 (\d{4}-\d{2}-\d{2})""")
    private val reminderTimeRegex = Regex("""\u23F0 (\d{2}):(\d{2})""")
    private val recurrenceRegex = Regex("""\uD83D\uDD01 ([^⚡😐🪫🍅⏫🔺🔼🔽⏬<]+)""")
    private val energyHighRegex = Regex("""\u26A1high""")
    private val energyMediumRegex = Regex("""\uD83D\uDE10medium""")
    private val energyLowRegex = Regex("""\uD83E\uDEABlow""")
    private val estimateRegex = Regex("""\uD83C\uDF45 (\d+)m""")
    private val priorityHighestRegex = Regex("""\u23EB""")
    private val priorityHighRegex = Regex("""\uD83D\uDD3A""")
    private val priorityMediumRegex = Regex("""\uD83D\uDD3C""")
    private val priorityLowRegex = Regex("""\uD83D\uDD3D""")
    private val priorityLowestRegex = Regex("""\u23EC""")
    private val sectionRegex = Regex("""^## (.+)$""")

    @OptIn(ExperimentalUuidApi::class)
    fun parse(markdown: String): ParsedTasks {
        if (markdown.isBlank()) return ParsedTasks(emptyList(), emptyMap())

        val lines = markdown.lines()
        val tasks = mutableListOf<Task>()
        val projectSections = mutableMapOf<String, String>()

        var currentSection: String? = null
        var insideFrontmatter = false
        var frontmatterClosed = false
        var positionCounter = 0.0

        for (line in lines) {
            val trimmed = line.trim()

            // Handle frontmatter
            if (trimmed == "---") {
                if (!insideFrontmatter && !frontmatterClosed) {
                    insideFrontmatter = true
                    continue
                } else if (insideFrontmatter) {
                    insideFrontmatter = false
                    frontmatterClosed = true
                    continue
                }
            }
            if (insideFrontmatter) continue

            // Check for section heading
            val sectionMatch = sectionRegex.find(trimmed)
            if (sectionMatch != null) {
                currentSection = sectionMatch.groupValues[1]
                continue
            }

            // Check for task line
            val taskMatch = taskLineRegex.find(trimmed)
            if (taskMatch != null) {
                val checkboxChar = taskMatch.groupValues[1].first()
                val status = TaskStatus.fromCheckboxChar(checkboxChar)
                val rawContent = taskMatch.groupValues[2]

                // Extract id
                val idMatch = idRegex.find(rawContent)
                val id = idMatch?.groupValues?.get(1) ?: Uuid.random().toString()

                // Extract due date
                val dueDateMatch = dueDateRegex.find(rawContent)
                val dueDate = dueDateMatch?.let {
                    val date = LocalDate.parse(it.groupValues[1])
                    LocalDateTime(date, LocalTime(0, 0))
                }

                // Extract completion date
                val completionDateMatch = completionRegex.find(rawContent)
                val completedDate = if (status == TaskStatus.DONE && completionDateMatch != null) {
                    val date = LocalDate.parse(completionDateMatch.groupValues[1])
                    LocalDateTime(date, LocalTime(0, 0))
                } else {
                    null
                }

                // Extract cancelled date
                val cancelledDateMatch = cancelledDateRegex.find(rawContent)
                val cancelledDate = if (status == TaskStatus.CANCELLED && cancelledDateMatch != null) {
                    val date = LocalDate.parse(cancelledDateMatch.groupValues[1])
                    LocalDateTime(date, LocalTime(0, 0))
                } else {
                    null
                }

                // Extract scheduled date
                val scheduledDateMatch = scheduledDateRegex.find(rawContent)
                val scheduledDate = scheduledDateMatch?.let {
                    val date = LocalDate.parse(it.groupValues[1])
                    LocalDateTime(date, LocalTime(0, 0))
                }

                // Extract start date
                val startDateMatch = startDateRegex.find(rawContent)
                val startDate = startDateMatch?.let {
                    val date = LocalDate.parse(it.groupValues[1])
                    LocalDateTime(date, LocalTime(0, 0))
                }

                // Extract created date (informational, mapped to createdAt via Instant)
                val createdDateMatch = createdDateRegex.find(rawContent)
                val createdAt = createdDateMatch?.let {
                    val date = LocalDate.parse(it.groupValues[1])
                    val ldt = LocalDateTime(date, LocalTime(0, 0))
                    ldt.toInstant(TimeZone.UTC)
                }

                // Extract reminder time
                val reminderTimeMatch = reminderTimeRegex.find(rawContent)
                val reminderTime = reminderTimeMatch?.let {
                    val hour = it.groupValues[1].toInt()
                    val minute = it.groupValues[2].toInt()
                    LocalTime(hour, minute)
                }

                // Extract recurrence rule
                val recurrenceMatch = recurrenceRegex.find(rawContent)
                val recurrenceRule = recurrenceMatch?.groupValues?.get(1)?.trim()

                // Extract energy level
                val energyLevel = when {
                    energyHighRegex.containsMatchIn(rawContent) -> EnergyLevel.HIGH
                    energyLowRegex.containsMatchIn(rawContent) -> EnergyLevel.LOW
                    energyMediumRegex.containsMatchIn(rawContent) -> EnergyLevel.MEDIUM
                    else -> EnergyLevel.MEDIUM
                }

                // Extract estimate
                val estimateMatch = estimateRegex.find(rawContent)
                val estimatedMinutes = estimateMatch?.groupValues?.get(1)?.toIntOrNull()

                // Extract priority
                val priority = when {
                    priorityHighestRegex.containsMatchIn(rawContent) -> Priority.URGENT
                    priorityHighRegex.containsMatchIn(rawContent) -> Priority.HIGH
                    priorityMediumRegex.containsMatchIn(rawContent) -> Priority.MEDIUM
                    priorityLowRegex.containsMatchIn(rawContent) -> Priority.LOW
                    priorityLowestRegex.containsMatchIn(rawContent) -> Priority.NONE
                    else -> Priority.NONE
                }

                // Clean title: strip all metadata markers and id comment
                val title = rawContent
                    .replace(idRegex, "")
                    .replace(dueDateRegex, "")
                    .replace(completionRegex, "")
                    .replace(cancelledDateRegex, "")
                    .replace(scheduledDateRegex, "")
                    .replace(startDateRegex, "")
                    .replace(createdDateRegex, "")
                    .replace(reminderTimeRegex, "")
                    .replace(recurrenceRegex, "")
                    .replace(energyHighRegex, "")
                    .replace(energyMediumRegex, "")
                    .replace(energyLowRegex, "")
                    .replace(estimateRegex, "")
                    .replace(priorityHighestRegex, "")
                    .replace(priorityHighRegex, "")
                    .replace(priorityMediumRegex, "")
                    .replace(priorityLowRegex, "")
                    .replace(priorityLowestRegex, "")
                    .trim()

                val isInbox = currentSection == null || currentSection == "Inbox"

                val task = Task(
                    id = id,
                    title = title,
                    status = status,
                    dueDate = dueDate,
                    completedDate = completedDate,
                    cancelledDate = cancelledDate,
                    scheduledDate = scheduledDate,
                    startDate = startDate,
                    reminderTime = reminderTime,
                    recurrenceRule = recurrenceRule,
                    priority = priority,
                    energyLevel = energyLevel,
                    estimatedMinutes = estimatedMinutes,
                    position = positionCounter,
                    createdAt = createdAt ?: Clock.System.now(),
                )

                tasks.add(task)

                if (!isInbox && currentSection != null) {
                    projectSections[id] = currentSection
                }

                positionCounter += 1.0
            }
        }

        return ParsedTasks(tasks, projectSections)
    }
}
