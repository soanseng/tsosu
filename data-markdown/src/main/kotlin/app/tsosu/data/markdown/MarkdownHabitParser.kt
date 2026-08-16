package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.model.RoutineTime
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ParsedHabits(
    val habits: List<Habit>,
    val completions: List<HabitCompletion>,
    /** Parsed per-habit notes with their routine grouping; empty for the legacy index fallback. */
    val parsedNotes: List<Pair<app.tsosu.data.markdown.habitnote.ParsedHabitNote, RoutineTime?>> = emptyList(),
)

class MarkdownHabitParser {

    private val habitLineRegex = Regex("""^- \[ ] (.+)$""")
    private val idRegex = Regex("""<!-- id:(\S+) -->""")
    private val tinyRegex = Regex("""\(tiny: ([^)]+)\)""")
    private val frequencyDailyRegex = Regex("""\uD83D\uDD01daily""")
    private val frequencyWeekdaysRegex = Regex("""\uD83D\uDD01weekdays""")
    private val frequencyCustomRegex = Regex("""\uD83D\uDD01(\d+)x/week""")
    private val energyHighRegex = Regex("""\u26A1high""")
    private val energyMediumRegex = Regex("""\u26A1medium""")
    private val energyLowRegex = Regex("""\u26A1low""")
    private val completionLineRegex = Regex("""^\s+- \u2705 (\d{4}-\d{2}-\d{2})$""")

    @OptIn(ExperimentalUuidApi::class)
    fun parse(markdown: String): ParsedHabits {
        if (markdown.isBlank()) return ParsedHabits(emptyList(), emptyList())

        val lines = markdown.lines()
        val habits = mutableListOf<Habit>()
        val completions = mutableListOf<HabitCompletion>()

        var insideFrontmatter = false
        var frontmatterClosed = false
        var currentHabitId: String? = null
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

            // Skip section headings
            if (trimmed.startsWith("## ")) continue

            // Check for completion line (must come before habit line check)
            val completionMatch = completionLineRegex.find(line)
            if (completionMatch != null && currentHabitId != null) {
                val date = LocalDate.parse(completionMatch.groupValues[1])
                completions.add(
                    HabitCompletion(
                        habitId = currentHabitId,
                        date = date,
                        completedAt = Clock.System.now(),
                    ),
                )
                continue
            }

            // Check for habit line
            val habitMatch = habitLineRegex.find(trimmed)
            if (habitMatch != null) {
                val rawContent = habitMatch.groupValues[1]

                // Extract id
                val idMatch = idRegex.find(rawContent)
                val id = idMatch?.groupValues?.get(1) ?: Uuid.random().toString()

                // Extract tiny version
                val tinyMatch = tinyRegex.find(rawContent)
                val tinyVersion = tinyMatch?.groupValues?.get(1)

                // Extract frequency and targetDaysPerWeek
                val customMatch = frequencyCustomRegex.find(rawContent)
                val frequency: HabitFrequency
                val targetDaysPerWeek: Int

                if (customMatch != null) {
                    frequency = HabitFrequency.CUSTOM
                    targetDaysPerWeek = customMatch.groupValues[1].toInt()
                } else if (frequencyWeekdaysRegex.containsMatchIn(rawContent)) {
                    frequency = HabitFrequency.WEEKDAYS
                    targetDaysPerWeek = 7
                } else {
                    frequency = HabitFrequency.DAILY
                    targetDaysPerWeek = 7
                }

                // Extract energy level
                val energyLevel = when {
                    energyHighRegex.containsMatchIn(rawContent) -> EnergyLevel.HIGH
                    energyLowRegex.containsMatchIn(rawContent) -> EnergyLevel.LOW
                    energyMediumRegex.containsMatchIn(rawContent) -> EnergyLevel.MEDIUM
                    else -> EnergyLevel.LOW
                }

                // Clean title: strip all metadata markers and id comment
                val title = rawContent
                    .replace(idRegex, "")
                    .replace(tinyRegex, "")
                    .replace(frequencyCustomRegex, "")
                    .replace(frequencyWeekdaysRegex, "")
                    .replace(frequencyDailyRegex, "")
                    .replace(energyHighRegex, "")
                    .replace(energyMediumRegex, "")
                    .replace(energyLowRegex, "")
                    .trim()

                val habit = Habit(
                    id = id,
                    title = title,
                    tinyVersion = tinyVersion,
                    frequency = frequency,
                    targetDaysPerWeek = targetDaysPerWeek,
                    energyLevel = energyLevel,
                    position = positionCounter,
                )

                habits.add(habit)
                currentHabitId = id
                positionCounter += 1.0
            } else {
                // Non-habit, non-completion line — reset current habit
                if (!line.isBlank()) {
                    currentHabitId = null
                }
            }
        }

        return ParsedHabits(habits, completions)
    }
}
