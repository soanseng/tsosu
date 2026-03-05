package app.tsosu.domain.usecase

import app.tsosu.domain.repository.TaskRepository

class DeleteTaskUseCase(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        return taskRepository.deleteTask(taskId)
    }
}
