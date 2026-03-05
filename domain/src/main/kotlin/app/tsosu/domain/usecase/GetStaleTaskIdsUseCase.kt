package app.tsosu.domain.usecase

import app.tsosu.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class GetStaleTaskIdsUseCase(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(olderThanDays: Int = 14): Flow<List<String>> {
        return taskRepository.getStaleTaskIds(olderThanDays)
    }
}
