package app.tsosu.data.markdown.habitnote

import app.tsosu.data.markdown.YamlFrontmatterParser
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

data class ParsedHabitNote(
    val habit: Habit,
    val completions: List<HabitCompletion>,
)

class HabitNoteParser {

    private val yamlParser = YamlFrontmatterParser()
    private val completionRegex = Regex("""^-\s*✅\s*(\d{4}-\d{2}-\d{2})""")

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

        val title = extractTitle(doc.body)

        val habit = Habit(
            id = id,
            title = title,
            tinyVersion = tiny,
            frequency = frequency,
            targetDaysPerWeek = targetDays,
            energyLevel = energy,
            color = color,
            isArchived = archived,
            createdAt = created?.atStartOfDayIn(TimeZone.UTC)
                ?: kotlinx.datetime.Clock.System.now(),
        )

        val completions = parseCompletions(doc.body, id)

        return ParsedHabitNote(habit, completions)
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
}
