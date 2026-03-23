package app.tsosu.data.calendar

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.IcsExporter

class IcsExporterImpl(
    private val vEventBuilder: VEventBuilder = VEventBuilder(),
) : IcsExporter {

    override fun exportTasks(tasks: List<Task>): String {
        val events = tasks.mapNotNull { task ->
            val dueDate = task.dueDate ?: return@mapNotNull null
            val reminderMinutes = computeReminderMinutes(task)
            vEventBuilder.buildVEventBody(
                uid = task.id,
                title = task.title,
                description = task.description,
                dueDate = dueDate.toString(),
                estimatedMinutes = task.estimatedMinutes,
                reminderMinutesBefore = reminderMinutes,
            )
        }

        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Tsosu//NONSGML v1//EN")
            for (event in events) {
                append(event)
            }
            appendLine("END:VCALENDAR")
        }
    }

    private fun computeReminderMinutes(task: Task): Int? {
        val reminderTime = task.reminderTime ?: return null
        val dueDate = task.dueDate ?: return null
        val dueMinutes = dueDate.hour * 60 + dueDate.minute
        val reminderMinutes = reminderTime.hour * 60 + reminderTime.minute
        val diff = dueMinutes - reminderMinutes
        return if (diff > 0) diff else null
    }
}
