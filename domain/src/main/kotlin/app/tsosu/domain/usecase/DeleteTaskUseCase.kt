package app.tsosu.domain.usecase

import app.tsosu.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first

class DeleteTaskUseCase(
    private val taskRepository: TaskRepository,
    private val calendarCoordinator: TaskCalendarCoordinator? = null,
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        val task = taskRepository.getTask(taskId).first()
        val result = taskRepository.deleteTask(taskId)
        if (result.isSuccess) {
            task?.let { calendarCoordinator?.removeEvent(it) }
        }
        return result
    }
}
