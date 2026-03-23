package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MarkdownHabitSerializer {

    fun serialize(habits: List<Habit>, completions: List<HabitCompletion>): String = buildString {
        appendFrontmatter()
        appendLine()

        val completionsByHabit = completions.groupBy { it.habitId }
        val grouped = habits.groupBy { it.frequency }

        // Emit sections in order: Daily, Weekdays, Custom — skip empty sections
        for (frequency in listOf(HabitFrequency.DAILY, HabitFrequency.WEEKDAYS, HabitFrequency.CUSTOM)) {
            val sectionHabits = grouped[frequency] ?: continue
            appendLine()
            appendLine("## ${sectionName(frequency)}")
            appendLine()
            sectionHabits.sortedBy { it.position }.forEach { habit ->
                appendLine(formatHabit(habit))
                val habitCompletions = completionsByHabit[habit.id]
                    ?.sortedByDescending { it.date }
                    ?: emptyList()
                habitCompletions.forEach { completion ->
                    appendLine("  - \u2705 ${completion.date}")
                }
            }
        }
    }

    internal fun formatHabit(habit: Habit): String = buildString {
        append("- [ ] ")
        append(habit.title)

        if (habit.tinyVersion != null) {
            append(" (tiny: ${habit.tinyVersion})")
        }

        append(" \uD83D\uDD01${frequencyLabel(habit)}")
        append(" \u26A1${energyLabel(habit.energyLevel)}")
        append(" <!-- id:${habit.id} -->")
    }

    private fun sectionName(frequency: HabitFrequency): String = when (frequency) {
        HabitFrequency.DAILY -> "Daily"
        HabitFrequency.WEEKDAYS -> "Weekdays"
        HabitFrequency.CUSTOM -> "Custom"
    }

    private fun frequencyLabel(habit: Habit): String = when (habit.frequency) {
        HabitFrequency.DAILY -> "daily"
        HabitFrequency.WEEKDAYS -> "weekdays"
        HabitFrequency.CUSTOM -> "${habit.targetDaysPerWeek}x/week"
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
        append("---")
    }
}
