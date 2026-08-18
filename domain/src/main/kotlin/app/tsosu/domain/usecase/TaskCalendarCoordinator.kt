package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.CalendarRepository
import app.tsosu.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first

/**
 * Bridges task changes to the configured calendar without ever failing the task
 * operation: dated tasks are upserted as events (the returned event id is stored
 * on the task), tasks that lose their date get their event removed, and terminal
 * or deleted tasks get their event removed. No-ops when no calendar is
 * configured — task flow always stays local-first.
 */
class TaskCalendarCoordinator(
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
) {

    /** Upserts (or removes) the calendar event for [task]; returns the task with the stored event id. */
    suspend fun syncTask(task: Task): Task = runCatching {
        if (!calendarRepository.isConfigured().first()) return task
        if (task.dueDate == null) {
            if (task.calendarEventId != null) {
                calendarRepository.removeCalendarEvent(task.calendarEventId)
                taskRepository.updateTask(task.copy(calendarEventId = null)).getOrDefault(task)
            } else {
                task
            }
        } else {
            val eventId = calendarRepository.syncTaskToCalendar(task).getOrNull() ?: return task
            if (eventId == task.calendarEventId) {
                task
            } else {
                taskRepository.updateTask(task.copy(calendarEventId = eventId)).getOrDefault(task)
            }
        }
    }.getOrDefault(task)

    /** Removes the event of a completed/cancelled/deleted task. */
    suspend fun removeEvent(task: Task) {
        runCatching {
            if (!calendarRepository.isConfigured().first()) return
            calendarRepository.removeCalendarEvent(task.calendarEventId ?: "tsosu-${task.id}")
        }
    }
}
