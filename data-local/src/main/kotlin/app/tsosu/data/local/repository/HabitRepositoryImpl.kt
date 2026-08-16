package app.tsosu.data.local.repository

import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitStreakInfo
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.usecase.HabitStreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val onHabitChanged: (suspend (habitId: String) -> Unit)? = null,
) : HabitRepository {

    override fun getActiveHabits(): Flow<List<Habit>> =
        habitDao.getActiveHabits().map { it.map { e -> e.toDomain() } }

    override fun getHabitsForRoutine(routineId: String): Flow<List<Habit>> =
        habitDao.getByRoutine(routineId).map { it.map { e -> e.toDomain() } }

    override fun getHabit(habitId: String): Flow<Habit?> =
        habitDao.getById(habitId).map { it?.toDomain() }

    override fun getTodayCompletions(): Flow<List<HabitCompletion>> {
        val today = todayEpoch()
        return habitDao.getCompletionsForDate(today).map { it.map { e -> e.toDomain() } }
    }

    override fun getStreakInfo(habitId: String): Flow<HabitStreakInfo> {
        return combine(
            habitDao.getById(habitId),
            habitDao.getCompletionDates(habitId),
        ) { habit, dateEpochs ->
            val tz = TimeZone.currentSystemDefault()
            val dates = dateEpochs.map {
                Instant.fromEpochMilliseconds(it).toLocalDateTime(tz).date
            }.toSet()
            HabitStreakInfo(
                habitId = habitId,
                habitTitle = habit?.title ?: "",
                completedLast7Days = HabitStreakCalculator.countInWindow(dates, 7),
                completedLast30Days = HabitStreakCalculator.countInWindow(dates, 30),
                currentConsecutiveDays = HabitStreakCalculator.consecutiveDays(dates),
                completionRate = if (dates.isNotEmpty()) {
                    HabitStreakCalculator.countInWindow(dates, 30) / 30f
                } else 0f,
            )
        }
    }

    override fun getAllStreakInfos(): Flow<List<HabitStreakInfo>> {
        return habitDao.getActiveHabits().map { habits ->
            habits.map { habit ->
                getStreakInfo(habit.id).first()
            }
        }
    }

    override suspend fun createHabit(habit: Habit): Result<Habit> = runCatching {
        habitDao.insert(habit.toEntity())
        onHabitChanged?.invoke(habit.id)
        habit
    }

    override suspend fun updateHabit(habit: Habit): Result<Habit> = runCatching {
        habitDao.update(habit.toEntity())
        onHabitChanged?.invoke(habit.id)
        habit
    }

    override suspend fun archiveHabit(habitId: String): Result<Unit> = runCatching {
        habitDao.archive(habitId)
        onHabitChanged?.invoke(habitId)
    }

    override suspend fun completeHabit(habitId: String, date: LocalDate): Result<HabitCompletion> =
        runCatching {
            val now = Clock.System.now()
            val completion = HabitCompletion(habitId, date, now)
            habitDao.insertCompletionOnce(
                habitId = habitId,
                date = completion.toEntity().date,
                completedAt = now.toEpochMilliseconds(),
            )
            onHabitChanged?.invoke(habitId)
            completion
        }

    override suspend fun uncompleteHabit(habitId: String, date: LocalDate): Result<Unit> =
        runCatching {
            val dateEpoch = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            habitDao.deleteCompletion(habitId, dateEpoch)
            onHabitChanged?.invoke(habitId)
        }

    private fun todayEpoch(): Long {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        return today.atStartOfDayIn(tz).toEpochMilliseconds()
    }
}
