package app.tsosu.data.markdown.dailynote

import app.tsosu.domain.model.Habit
import kotlinx.datetime.LocalDate

class DailyNoteWriter {

    fun write(date: LocalDate, habits: List<Habit>, completedHabitIds: Set<String>): String = buildString {
        appendLine("---")
        appendLine("date: $date")
        appendLine("---")
        appendLine()
        appendLine("## Habits")
        for (habit in habits.sortedBy { it.position }) {
            val checked = if (habit.id in completedHabitIds) "x" else " "
            appendLine("- [$checked] ${habit.title} #habit <!-- id:${habit.id} -->")
        }
    }

    fun filename(date: LocalDate): String = "$date.md"
}
