package app.tsosu.data.markdown.index

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

class HabitIndexGenerator {

    fun generate(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        noteFilenames: Map<String, String> = emptyMap(),
    ): String = buildString {
        appendFrontmatter()

        val completionsByHabit = completions.groupBy { it.habitId }
        val grouped = habits.groupBy { it.frequency }

        for (frequency in listOf(HabitFrequency.DAILY, HabitFrequency.WEEKDAYS, HabitFrequency.CUSTOM)) {
            val sectionHabits = grouped[frequency] ?: continue
            appendLine()
            appendLine("## ${sectionName(frequency)}")
            appendLine()
            sectionHabits.sortedBy { it.position }.forEach { habit ->
                val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
                appendLine(formatHabitLine(habit, habitCompletions, noteFilenames))
            }
        }
    }

    private fun formatHabitLine(
        habit: Habit,
        completions: List<HabitCompletion>,
        noteFilenames: Map<String, String>,
    ): String = buildString {
        append("- ")
        append(habit.title)

        if (habit.frequency == HabitFrequency.CUSTOM) {
            append(" \uD83D\uDD01${habit.targetDaysPerWeek}x/week")
        }

        append(" \u26A1${energyLabel(habit.energyLevel)}")

        if (habit.frequency == HabitFrequency.CUSTOM) {
            val count = countCompletionsInLastWeek(completions)
            append(" $count/${habit.targetDaysPerWeek}")
        }

        val streak = calculateStreak(completions)
        if (streak > 0) {
            append(" \uD83D\uDD25$streak")
        }

        val slug = noteFilenames[habit.id] ?: slugify(habit.title)
        append(" [[habits/$slug]]")
        append(" <!-- id:${habit.id} -->")
    }

    internal fun calculateStreak(completions: List<HabitCompletion>): Int {
        if (completions.isEmpty()) return 0

        val dates = completions.map { it.date }.toSortedSet().sortedDescending()
        val mostRecent = dates.first()
        var streak = 1
        var expected = mostRecent

        for (date in dates.drop(1)) {
            val previousDay = expected.minus(1, DateTimeUnit.DAY)
            if (date == previousDay) {
                streak++
                expected = date
            } else {
                break
            }
        }
        return streak
    }

    private fun countCompletionsInLastWeek(completions: List<HabitCompletion>): Int {
        val today = Clock.System.todayIn(TimeZone.UTC)
        val weekAgo = today.minus(6, DateTimeUnit.DAY) // today minus 6 = 7-day window
        return completions.count { it.date in weekAgo..today }
    }

    private fun slugify(title: String): String {
        return title
            .lowercase()
            .replace(Regex("[^\\w\\s\\u4e00-\\u9fff\\u3040-\\u309f\\u30a0-\\u30ff-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    private fun sectionName(frequency: HabitFrequency): String = when (frequency) {
        HabitFrequency.DAILY -> "Daily"
        HabitFrequency.WEEKDAYS -> "Weekdays"
        HabitFrequency.CUSTOM -> "Custom"
    }

    private fun energyLabel(energy: EnergyLevel): String = when (energy) {
        EnergyLevel.LOW -> "low"
        EnergyLevel.MEDIUM -> "medium"
        EnergyLevel.HIGH -> "high"
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
