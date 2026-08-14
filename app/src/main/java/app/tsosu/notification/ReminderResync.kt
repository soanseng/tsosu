package app.tsosu.notification

import app.tsosu.data.local.dao.TaskDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Reconciles reminder alarms with the current task set after a sync/import
 * brought in (or changed) due dates and reminder times from the markdown vault.
 */
@Singleton
class ReminderResync @Inject constructor(
    private val taskDao: TaskDao,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend fun afterSync() {
        reminderScheduler.rescheduleAll(taskDao.getAllTasks().first())
    }
}
