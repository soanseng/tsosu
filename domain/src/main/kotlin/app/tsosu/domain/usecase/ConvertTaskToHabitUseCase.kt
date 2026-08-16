package app.tsosu.domain.usecase

import app.tsosu.domain.model.Habit
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first

class ConvertTaskToHabitUseCase(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
) {
    suspend operator fun invoke(taskId: String): Result<Habit> {
        val task = taskRepository.getTask(taskId).first()
            ?: return Result.failure(IllegalArgumentException("Task not found: $taskId"))
        if (task.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Task title must not be blank"))
        }

        val habit = Habit(
            title = task.title,
            tinyVersion = task.description.takeIf { it.isNotBlank() },
            energyLevel = task.energyLevel,
        )
        val created = habitRepository.createHabit(habit)
        if (created.isFailure) return created

        taskRepository.deleteTask(taskId).getOrElse { error ->
            return Result.failure(error)
        }
        return created
    }
}
