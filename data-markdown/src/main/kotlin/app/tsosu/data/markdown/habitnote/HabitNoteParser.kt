package app.tsosu.data.markdown.habitnote

import app.tsosu.data.markdown.YamlFrontmatterParser
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.model.RoutineTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

data class ParsedHabitNote(
    val habit: Habit,
    val completions: List<HabitCompletion>,
    val routineTime: RoutineTime? = null,
    val projectName: String? = null,
)
class HabitNoteParser {

    private val yamlParser = YamlFrontmatterParser()
    private val completionRegex = Regex("""^-\s*✅\s*(\d{4}-\d{2}-\d{2})""")
    private val reminderRegex = Regex("""(\d{1,2}):(\d{2})""")

    fun parse(content: String): ParsedHabitNote {
        val doc = yamlParser.parse(content)
        val fm = doc.frontmatter

        val id = fm["id"] ?: error("HabitNote missing required 'id' field")
        val frequency = parseFrequency(fm["frequency"] ?: "daily")
        val targetDays = fm["target_days"]?.toIntOrNull() ?: 7
        val energy = parseEnergy(fm["energy"])
        val tiny = fm["tiny"]
        val color = fm["color"] ?: "#4CAF50"
        val archived = fm["archived"]?.toBooleanStrictOrNull() ?: false
        val created = fm["created"]?.let { LocalDate.parse(it) }
        val routineTime = parseRoutine(fm["routine"])
        val reminderTime = parseReminder(fm["reminder"])
        val projectName = fm["project"]?.trim()?.removeSurrounding("\"")
        val weekdays = parseWeekdays(fm["weekdays"])

        val title = extractTitle(doc.body)

        val habit = Habit(
            id = id,
            title = title,
            tinyVersion = tiny,
            frequency = frequency,
            weekdays = weekdays,
            targetDaysPerWeek = targetDays,
            energyLevel = energy,
            color = color,
            isArchived = archived,
            reminderTime = reminderTime,
            createdAt = created?.atStartOfDayIn(TimeZone.UTC)
                ?: kotlinx.datetime.Clock.System.now(),
        )

        val completions = parseCompletions(doc.body, id)

        return ParsedHabitNote(habit, completions, routineTime, projectName)
    }

    private fun extractTitle(body: String): String {
        for (line in body.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                return trimmed.removePrefix("# ").trim()
            }
        }
        return ""
    }

    private fun parseCompletions(body: String, habitId: String): List<HabitCompletion> {
        val completions = mutableListOf<HabitCompletion>()
        for (line in body.lines()) {
            val match = completionRegex.find(line.trim()) ?: continue
            val date = LocalDate.parse(match.groupValues[1])
            completions += HabitCompletion(
                habitId = habitId,
                date = date,
                completedAt = date.atStartOfDayIn(TimeZone.UTC),
            )
        }
        return completions
    }

    private fun parseFrequency(value: String): HabitFrequency = when (value.lowercase()) {
        "daily" -> HabitFrequency.DAILY
        "weekdays" -> HabitFrequency.WEEKDAYS
        "custom" -> HabitFrequency.CUSTOM
        else -> HabitFrequency.DAILY
    }

    private fun parseEnergy(value: String?): EnergyLevel = when (value?.lowercase()) {
        "low" -> EnergyLevel.LOW
        "medium" -> EnergyLevel.MEDIUM
        "high" -> EnergyLevel.HIGH
        else -> EnergyLevel.LOW
    }

    private fun parseRoutine(value: String?): RoutineTime? = when (value?.lowercase()) {
        "morning" -> RoutineTime.MORNING
        "afternoon" -> RoutineTime.AFTERNOON
        "evening" -> RoutineTime.EVENING
        else -> null
    }

    private fun parseReminder(value: String?): LocalTime? {
        val match = value?.let { reminderRegex.find(it) } ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        return if (hour in 0..23 && minute in 0..59) LocalTime(hour, minute) else null
    }

    private fun parseWeekdays(value: String?): Set<Int> {
        val body = value?.removeSurrounding("[", "]") ?: return emptySet()
        return body.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
    }
}
