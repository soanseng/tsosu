package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository

class ToggleTaskDoneUseCase(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String): Result<Task> {
        return taskRepository.toggleDone(taskId)
    }
}
