package app.tsosu.domain.usecase

import app.tsosu.domain.model.DailyFocus
import app.tsosu.domain.repository.FocusRepository
import app.tsosu.domain.repository.TaskRepository
import kotlinx.datetime.LocalDate

class SetDailyFocusUseCase(
    private val focusRepository: FocusRepository,
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(date: LocalDate, taskIds: List<String>): Result<DailyFocus> {
        require(taskIds.size <= 3) { "Focus 3 allows at most 3 tasks" }
        require(taskIds.isNotEmpty()) { "Must select at least 1 task" }

        taskIds.forEach { id ->
            taskRepository.setFocus(id, true)
        }

        return focusRepository.setDailyFocus(date, taskIds)
    }
}
