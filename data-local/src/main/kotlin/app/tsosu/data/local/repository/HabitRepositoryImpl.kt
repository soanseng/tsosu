package app.tsosu.data.local.repository

import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitStreakInfo
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.repository.GamificationRepository
import app.tsosu.domain.usecase.HabitStreakCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val onHabitChanged: (suspend (habitId: String) -> Unit)? = null,
    private val gamification: GamificationRepository? = null,
) : HabitRepository {
    override fun getActiveHabits(): Flow<List<Habit>> =
        habitDao.getActiveHabits().map { it.map { e -> e.toDomain() } }

    override fun getHabitsForRoutine(routineId: String): Flow<List<Habit>> =
        habitDao.getByRoutine(routineId).map { it.map { e -> e.toDomain() } }

    override fun getHabit(habitId: String): Flow<Habit?> =
        habitDao.getById(habitId).map { it?.toDomain() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTodayCompletions(): Flow<List<HabitCompletion>> =
        localDayTicker().flatMapLatest { today ->
            habitDao.getCompletionsForDate(today.toEpochDays().toLong())
                .map { it.map { e -> e.toDomain() } }
        }

    override fun getStreakInfo(habitId: String): Flow<HabitStreakInfo> {
        return combine(
            habitDao.getById(habitId),
            habitDao.getCompletionDates(habitId),
            gamification?.shieldedDates(habitId) ?: flowOf(emptyList()),
        ) { habit, dateEpochDays, shieldEpochDays ->
            val dates = dateEpochDays.map { LocalDate.fromEpochDays(it.toInt()) }.toSet()
            val shielded = shieldEpochDays.map { LocalDate.fromEpochDays(it.toInt()) }.toSet()
            HabitStreakInfo(
                habitId = habitId,
                habitTitle = habit?.title ?: "",
                completedLast7Days = HabitStreakCalculator.countInWindow(dates, 7),
                completedLast30Days = HabitStreakCalculator.countInWindow(dates, 30),
                currentConsecutiveDays = HabitStreakCalculator.consecutiveDays(
                    distinctDates = dates + shielded,
                ),
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
            val inserted = habitDao.insertCompletionOnce(
                habitId = habitId,
                date = completion.toEntity().date,
                completedAt = now.toEpochMilliseconds(),
            )
            if (inserted > 0) {
                // First completion of the day earns energy.
                gamification?.awardEnergy(ENERGY_PER_HABIT)
                // A completed habit with a bought freeze auto-bridges its
                // most recent gap (Duolingo-style streak repair).
                autoShieldGap(habitId)
            }
            onHabitChanged?.invoke(habitId)
            completion
        }

    private suspend fun autoShieldGap(habitId: String) {
        val gamification = gamification ?: return
        val dates = habitDao.getCompletionDatesSync(habitId)
            .map { LocalDate.fromEpochDays(it.toInt()) }
            .toSet()
        val gap = HabitStreakCalculator.firstGapBeforeStreak(dates) ?: return
        gamification.shieldGap(habitId, gap.toEpochDays().toLong())
    }

    override suspend fun uncompleteHabit(habitId: String, date: LocalDate): Result<Unit> =
        runCatching {
            habitDao.deleteCompletion(habitId, date.toEpochDays().toLong())
            onHabitChanged?.invoke(habitId)
        }
    private fun localDayTicker(): Flow<LocalDate> = flow {
        while (currentCoroutineContext().isActive) {
            val tz = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
            val today = now.toLocalDateTime(tz).date
            emit(today)
            val nextMidnight = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
            val waitMillis = (nextMidnight - now).inWholeMilliseconds + 50
            delay(waitMillis.coerceAtLeast(1))
        }
    }

    companion object {
        const val ENERGY_PER_HABIT = 5
    }
}
