package app.tsosu.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.tsosu.data.local.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleReminders() {
        val now = System.currentTimeMillis()
        val tasks = taskDao.getAllTasks().first()

        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(zone).date

        for (task in tasks) {
            if (task.status >= 4) continue // skip done/cancelled
            val reminderMinutes = task.reminderTimeMinutes ?: continue
            task.dueDate ?: continue // skip tasks without a due date

            val reminderTime = LocalTime(reminderMinutes / 60, reminderMinutes % 60)
            val triggerMillis = today.atTime(reminderTime)
                .toInstant(zone)
                .toEpochMilliseconds()

            if (triggerMillis > now) {
                reminderScheduler.schedule(task.id, triggerMillis)
            }
        }
    }
}
