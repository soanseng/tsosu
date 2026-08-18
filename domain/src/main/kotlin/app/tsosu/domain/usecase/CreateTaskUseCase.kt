package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository

class CreateTaskUseCase(
    private val taskRepository: TaskRepository,
    private val calendarCoordinator: TaskCalendarCoordinator? = null,
) {
    suspend operator fun invoke(task: Task): Result<Task> {
        require(task.title.isNotBlank()) { "Task title must not be blank" }
        val result = taskRepository.createTask(task)
        return if (result.isSuccess) {
            val created = result.getOrThrow()
            Result.success(calendarCoordinator?.syncTask(created) ?: created)
        } else {
            result
        }
    }
}
