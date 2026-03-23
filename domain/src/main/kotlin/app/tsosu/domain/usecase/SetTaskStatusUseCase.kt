package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository

class SetTaskStatusUseCase(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String, status: TaskStatus): Result<Task> {
        return taskRepository.setStatus(taskId, status)
    }
}
