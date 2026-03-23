package app.tsosu.domain.usecase

import app.tsosu.domain.repository.IcsExporter
import app.tsosu.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first

class ExportIcsUseCase(
    private val taskRepository: TaskRepository,
    private val icsExporter: IcsExporter,
) {
    suspend operator fun invoke(): Result<String> {
        return try {
            val tasks = taskRepository.getUpcomingTasks(days = 365).first()
            val tasksWithDueDate = tasks.filter { it.dueDate != null }
            if (tasksWithDueDate.isEmpty()) {
                return Result.success("")
            }
            Result.success(icsExporter.exportTasks(tasksWithDueDate))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
