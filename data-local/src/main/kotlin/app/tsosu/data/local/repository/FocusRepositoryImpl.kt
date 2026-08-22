package app.tsosu.data.local.repository

import app.tsosu.data.local.dao.FocusDao
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.model.DailyFocus
import app.tsosu.domain.model.WeeklyReview
import app.tsosu.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

class FocusRepositoryImpl(
    private val focusDao: FocusDao,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
) : FocusRepository {

    override fun getDailyFocus(date: LocalDate): Flow<DailyFocus?> {
        val dateEpoch = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        return focusDao.getByDate(dateEpoch).map { it?.toDomain() }
    }

    override suspend fun setDailyFocus(date: LocalDate, taskIds: List<String>): Result<DailyFocus> =
        runCatching {
            val focus = DailyFocus(date, taskIds)
            focusDao.insert(focus.toEntity())
            focus
        }

    override fun getWeeklyReview(weekStart: LocalDate): Flow<WeeklyReview> {
        val tz = TimeZone.currentSystemDefault()
        val start = weekStart.atStartOfDayIn(tz).toEpochMilliseconds()
        val weekEnd = weekStart.plus(7, DateTimeUnit.DAY)
        val end = weekEnd.atStartOfDayIn(tz).toEpochMilliseconds() - 1

        return combine(
            taskDao.getTodayTasks(start, end), // reuse for date range
            habitDao.getCompletionsForDate(weekStart.toEpochDays().toLong()), // simplified
            focusDao.getFocusDaysCount(start, end),
        ) { tasks, _, focusDays ->
            val completedTasks = tasks.filter { it.done }
            WeeklyReview(
                weekStart = weekStart,
                tasksCompleted = completedTasks.size,
                habitsCompletedTotal = 0, // simplified
                focusDaysCompleted = focusDays,
                totalEstimatedMinutes = completedTasks.mapNotNull { it.estimatedMinutes }.sum(),
                topProject = null,
                longestHabitStreak = null,
            )
        }
    }
}
