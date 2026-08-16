package app.tsosu.data.markdown.index

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.model.RoutineTime
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

class HabitIndexGenerator {
    fun generate(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        noteFilenames: Map<String, String> = emptyMap(),
        routineTimeByHabitId: Map<String, RoutineTime> = emptyMap(),
    ): String = buildString {
        appendFrontmatter()

        val completionsByHabit = completions.groupBy { it.habitId }

        for (routine in listOf(RoutineTime.MORNING, RoutineTime.AFTERNOON, RoutineTime.EVENING)) {
            val sectionHabits = habits.filter { routineTimeByHabitId[it.id] == routine }
            if (sectionHabits.isEmpty()) continue
            appendSection(routine, sectionHabits, completionsByHabit, noteFilenames)
        }

        val otherHabits = habits.filter { routineTimeByHabitId[it.id] == null }
        if (otherHabits.isNotEmpty()) {
            appendSection(null, otherHabits, completionsByHabit, noteFilenames)
        }
    }

    private fun StringBuilder.appendSection(
        routine: RoutineTime?,
        habits: List<Habit>,
        completionsByHabit: Map<String, List<HabitCompletion>>,
        noteFilenames: Map<String, String>,
    ) {
        appendLine()
        appendLine("## ${sectionName(routine)}")
        appendLine()
        habits.sortedBy { it.position }.forEach { habit ->
            appendLine(formatHabitLine(habit, completionsByHabit[habit.id] ?: emptyList(), noteFilenames))
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

        habit.reminderTime?.let { reminder ->
            append(" \u23F0 %02d:%02d".format(reminder.hour, reminder.minute))
        }

        append(" \u26A1${energyLabel(habit.energyLevel)}")

        if (habit.frequency == HabitFrequency.CUSTOM) {
            val count = countCompletionsInLastWeek(completions)
            append(" $count/${habit.targetDaysPerWeek}")
        } else {
            val streak = calculateStreak(completions)
            if (streak > 0) {
                append(" \uD83D\uDD25$streak")
            }
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

    private fun sectionName(routine: RoutineTime?): String = when (routine) {
        RoutineTime.MORNING -> "🌅 Morning"
        RoutineTime.AFTERNOON -> "☀\uFE0F Anytime"
        RoutineTime.EVENING -> "🌙 Evening"
        null -> "Other"
    }

    private fun energyLabel(energy: EnergyLevel): String = when (energy) {
        EnergyLevel.LOW -> "low"
        EnergyLevel.MEDIUM -> "medium"
        EnergyLevel.HIGH -> "high"
    }

    private fun StringBuilder.appendFrontmatter() {
        appendLine("---")
        appendLine("tsosu: v1")
        appendLine("generated: true")
        append("---")
    }
}
