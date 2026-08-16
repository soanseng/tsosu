package app.tsosu.data.markdown.habitnote

import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.model.RoutineTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HabitNoteSerializer {

    fun serialize(
        habit: Habit,
        completions: List<HabitCompletion>,
        routineTime: RoutineTime? = null,
    ): String = buildString {
        // YAML frontmatter
        appendLine("---")
        appendLine("id: ${habit.id}")
        appendLine("frequency: ${frequencyValue(habit.frequency)}")
        if (habit.frequency == HabitFrequency.CUSTOM) {
            appendLine("target_days: ${habit.targetDaysPerWeek}")
        }
        if (routineTime != null) {
            appendLine("routine: ${routineTime.name.lowercase()}")
        }
        habit.reminderTime?.let { reminder ->
            appendLine("reminder: \"%02d:%02d\"".format(reminder.hour, reminder.minute))
        }
        appendLine("energy: ${habit.energyLevel.name.lowercase()}")
        habit.tinyVersion?.let { tiny ->
            appendLine("tiny: \"$tiny\"")
        }
        appendLine("color: \"${habit.color}\"")
        appendLine("archived: ${habit.isArchived}")
        appendLine("created: ${habit.createdAt.toLocalDateTime(TimeZone.UTC).date}")
        appendLine("---")

        // Body: title as H1
        appendLine()
        appendLine("# ${habit.title}")

        // Completions section (only if non-empty)
        val sorted = completions.sortedByDescending { it.date }
        if (sorted.isNotEmpty()) {
            appendLine()
            appendLine("## Completions")
            for (completion in sorted) {
                appendLine("- \u2705 ${completion.date}")
            }
        }
    }

    fun slugify(title: String): String {
        return title
            .lowercase()
            .replace(Regex("[^\\w\\s\\u4e00-\\u9fff\\u3040-\\u309f\\u30a0-\\u30ff-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    private fun frequencyValue(frequency: HabitFrequency): String = when (frequency) {
        HabitFrequency.DAILY -> "daily"
        HabitFrequency.WEEKDAYS -> "weekdays"
        HabitFrequency.CUSTOM -> "custom"
    }
}
