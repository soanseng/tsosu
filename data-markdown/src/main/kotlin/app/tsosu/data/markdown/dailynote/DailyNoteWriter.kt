package app.tsosu.data.markdown.dailynote

import app.tsosu.domain.model.Task
import kotlinx.datetime.LocalDate

class DailyNoteWriter {

    fun write(date: LocalDate, recurringTasks: List<Task>, completedTaskIds: Set<String>): String = buildString {
        appendLine("---")
        appendLine("date: $date")
        appendLine("---")
        appendLine()
        appendLine("## Habits")
        for (task in recurringTasks.sortedBy { it.position }) {
            val checked = if (task.id in completedTaskIds) "x" else " "
            appendLine("- [$checked] ${task.title} #habit <!-- id:${task.id} -->")
        }
    }

    fun filename(date: LocalDate): String = "$date.md"
}
