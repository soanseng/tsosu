package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository

class CreateTaskUseCase(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(task: Task): Result<Task> {
        require(task.title.isNotBlank()) { "Task title must not be blank" }
        return taskRepository.createTask(task)
    }
}
