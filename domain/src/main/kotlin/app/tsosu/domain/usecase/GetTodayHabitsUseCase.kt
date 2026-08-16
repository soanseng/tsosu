package app.tsosu.domain.usecase

import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class HabitWithStatus(
    val habit: Habit,
    val isCompletedToday: Boolean,
)

class GetTodayHabitsUseCase(
    private val habitRepository: HabitRepository,
    private val today: () -> LocalDate = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    },
) {
    operator fun invoke(): Flow<List<HabitWithStatus>> {
        return combine(
            habitRepository.getActiveHabits(),
            habitRepository.getTodayCompletions(),
        ) { habits, completions ->
            val completedIds = completions.map(HabitCompletion::habitId).toSet()
            val isWeekend = today().dayOfWeek.ordinal in 5..6 // kotlinx-datetime 0.6: MONDAY=0..SUNDAY=6
            habits
                .filter { !(isWeekend && it.frequency == HabitFrequency.WEEKDAYS) }
                .map { habit ->
                    HabitWithStatus(
                        habit = habit,
                        isCompletedToday = habit.id in completedIds,
                    )
                }
        }
    }
}
