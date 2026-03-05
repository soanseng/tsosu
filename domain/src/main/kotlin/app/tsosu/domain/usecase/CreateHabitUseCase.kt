package app.tsosu.domain.usecase

import app.tsosu.domain.model.Habit
import app.tsosu.domain.repository.HabitRepository

class CreateHabitUseCase(
    private val habitRepository: HabitRepository,
) {
    suspend operator fun invoke(habit: Habit): Result<Habit> {
        require(habit.title.isNotBlank()) { "Habit title must not be blank" }
        return habitRepository.createHabit(habit)
    }
}
