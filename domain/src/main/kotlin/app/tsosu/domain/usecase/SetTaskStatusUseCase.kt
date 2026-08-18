package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository

class SetTaskStatusUseCase(
    private val taskRepository: TaskRepository,
    private val calendarCoordinator: TaskCalendarCoordinator? = null,
) {
    suspend operator fun invoke(taskId: String, status: TaskStatus): Result<Task> {
        val result = taskRepository.setStatus(taskId, status)
        result.getOrNull()?.let { task ->
            calendarCoordinator?.let { coordinator ->
                if (task.status.isTerminal) {
                    coordinator.removeEvent(task)
                } else {
                    coordinator.syncTask(task)
                }
            }
        }
        return result
    }
}
