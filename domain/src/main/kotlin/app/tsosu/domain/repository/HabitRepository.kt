package app.tsosu.domain.repository

import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitStreakInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface HabitRepository {
    fun getActiveHabits(): Flow<List<Habit>>
    fun getHabitsForRoutine(routineId: String): Flow<List<Habit>>
    fun getHabit(habitId: String): Flow<Habit?>
    fun getTodayCompletions(): Flow<List<HabitCompletion>>
    fun getStreakInfo(habitId: String): Flow<HabitStreakInfo>
    fun getAllStreakInfos(): Flow<List<HabitStreakInfo>>
    suspend fun createHabit(habit: Habit): Result<Habit>
    suspend fun updateHabit(habit: Habit): Result<Habit>
    suspend fun archiveHabit(habitId: String): Result<Unit>
    suspend fun completeHabit(habitId: String, date: LocalDate): Result<HabitCompletion>
    suspend fun uncompleteHabit(habitId: String, date: LocalDate): Result<Unit>
}
