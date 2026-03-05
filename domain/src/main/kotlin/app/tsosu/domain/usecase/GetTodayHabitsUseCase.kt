package app.tsosu.domain.usecase

import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HabitWithStatus(
    val habit: Habit,
    val isCompletedToday: Boolean,
)

class GetTodayHabitsUseCase(
    private val habitRepository: HabitRepository,
) {
    operator fun invoke(): Flow<List<HabitWithStatus>> {
        return combine(
            habitRepository.getActiveHabits(),
            habitRepository.getTodayCompletions(),
        ) { habits, completions ->
            val completedIds = completions.map(HabitCompletion::habitId).toSet()
            habits.map { habit ->
                HabitWithStatus(
                    habit = habit,
                    isCompletedToday = habit.id in completedIds,
                )
            }
        }
    }
}
