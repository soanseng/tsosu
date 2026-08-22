package app.tsosu.notification

import app.tsosu.domain.usecase.DigestFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import app.tsosu.data.local.mapper.toDomain
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.TaskDao

/** Feeds the daily digest with today's tasks and habit completion counts. */
@Singleton
class DigestData @Inject constructor(
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
) {
    suspend fun buildDigestContent(): DigestFormatter.DigestContent {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val start = today.atStartOfDayIn(tz).toEpochMilliseconds()
        val end = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz).toEpochMilliseconds() - 1

        val tasks = taskDao.getTodayTasks(start, end).first()
            .map { it.toDomain() }
        val habits = habitDao.getActiveHabitsSync()
        val completedToday = habitDao.getCompletionsForDate(today.toEpochDays().toLong()).first()

        return DigestFormatter.build(
            todayTasks = tasks,
            habitsCompleted = habits.count { h -> completedToday.any { it.habitId == h.id } },
            habitsTotal = habits.size,
        )
    }
}
