package app.tsosu.notification

import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.TaskDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Reconciles reminder alarms with the current task/habit set after a
 * sync/import brought in (or changed) due dates, reminder times, or habit
 * reminders from the markdown vault.
 */
@Singleton
class ReminderResync @Inject constructor(
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend fun afterSync() {
        reminderScheduler.rescheduleAll(taskDao.getAllTasks().first())
        reminderScheduler.rescheduleHabits(habitDao.getActiveHabitsSync())
    }
}
