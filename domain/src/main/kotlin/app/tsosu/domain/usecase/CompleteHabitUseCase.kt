package app.tsosu.domain.usecase

import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.repository.HabitRepository
import kotlinx.datetime.LocalDate

class CompleteHabitUseCase(
    private val habitRepository: HabitRepository,
) {
    suspend operator fun invoke(habitId: String, date: LocalDate): Result<HabitCompletion> {
        return habitRepository.completeHabit(habitId, date)
    }
}
