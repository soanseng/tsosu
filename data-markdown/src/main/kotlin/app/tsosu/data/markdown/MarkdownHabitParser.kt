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
    /**
     * Routine grouping per habit id, when derivable (index section headings or
     * per-habit notes); empty for vaults that carry neither.
     */
    val routineTimeByHabitId: Map<String, RoutineTime> = emptyMap(),
    /** Parsed per-habit notes with their routine grouping; empty for the legacy index fallback. */
    val parsedNotes: List<Pair<app.tsosu.data.markdown.habitnote.ParsedHabitNote, RoutineTime?>> = emptyList(),
    /** Habit id → project title from note frontmatter (project: "..."). */
    val projectNameByHabitId: Map<String, String> = emptyMap(),
)

class MarkdownHabitParser {
    private val habitLineRegex = Regex("""^- \[ ] (.+)$""")
    // Generated index lines use a bare dash (no checkbox state for habits).
    private val bareHabitLineRegex = Regex("""^- ([^\[].*)$""")
    private val idRegex = Regex("""<!-- id:(\S+) -->""")
    private val wikilinkRegex = Regex("""\[\[habits/[^\]]*\]\]""")
    private val tinyRegex = Regex("""\(tiny: ([^)]+)\)""")
    private val frequencyWeekdaysRegex = Regex("""\uD83D\uDD01weekdays""")
    private val frequencyCustomRegex = Regex("""\uD83D\uDD01(\d+)x/week""")
    private val frequencyDailyRegex = Regex("""\uD83D\uDD01daily""")
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
        val routineByHabitId = mutableMapOf<String, RoutineTime>()
        var insideFrontmatter = false
        var frontmatterClosed = false
        var currentHabitId: String? = null
        var currentRoutine: RoutineTime? = null
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

            // Track section heading → routine for index lines
            if (trimmed.startsWith("## ")) {
                currentRoutine = when {
                    trimmed.contains("Morning") -> RoutineTime.MORNING
                    trimmed.contains("Anytime") -> RoutineTime.AFTERNOON
                    trimmed.contains("Evening") -> RoutineTime.EVENING
                    else -> null
                }
                continue
            }

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
                ?: bareHabitLineRegex.find(trimmed)
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

                // Clean title: strip all metadata markers, wikilink, and id comment
                val title = rawContent
                    .replace(idRegex, "")
                    .replace(wikilinkRegex, "")
                    .replace(tinyRegex, "")
                    .replace(frequencyCustomRegex, "")
                    .replace(frequencyWeekdaysRegex, "")
                    .replace(frequencyDailyRegex, "")
                    .replace(energyHighRegex, "")
                    .replace(energyMediumRegex, "")
                    .replace(energyLowRegex, "")
                    .replace(Regex("\\s+"), " ")
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
                currentRoutine?.let { routineByHabitId[id] = it }
                currentHabitId = id
                positionCounter += 1.0
            } else {
                // Non-habit, non-completion line — reset current habit
                if (!line.isBlank()) {
                    currentHabitId = null
                }
            }
        }

        return ParsedHabits(habits, completions, routineTimeByHabitId = routineByHabitId)
    }
}
