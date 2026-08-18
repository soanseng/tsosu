package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository

class ToggleTaskDoneUseCase(
    private val taskRepository: TaskRepository,
    private val calendarCoordinator: TaskCalendarCoordinator? = null,
) {
    suspend operator fun invoke(taskId: String): Result<Task> {
        val result = taskRepository.toggleDone(taskId)
        result.getOrNull()?.let { task ->
            calendarCoordinator?.let { coordinator ->
                if (task.status.isTerminal) {
                    coordinator.removeEvent(task)
                } else {
                    // Un-done, or a recurring occurrence reset to its next date.
                    coordinator.syncTask(task)
                }
            }
        }
        return result
    }
}
