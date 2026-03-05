package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TodayOverview(
    val tasks: List<Task>,
    val totalEstimatedMinutes: Int,
    val focusCount: Int,
)

class GetTodayOverviewUseCase(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(): Flow<TodayOverview> {
        return taskRepository.getTodayTasks().map { tasks ->
            TodayOverview(
                tasks = tasks,
                totalEstimatedMinutes = tasks.mapNotNull { it.estimatedMinutes }.sum(),
                focusCount = tasks.count { it.isFocus },
            )
        }
    }
}
