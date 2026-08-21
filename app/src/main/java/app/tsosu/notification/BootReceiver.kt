package app.tsosu.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
        )
    }

    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var habitDao: HabitDao
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reminderScheduler.rescheduleAll(taskDao.getAllTasks().first())
                reminderScheduler.rescheduleHabits(habitDao.getActiveHabitsSync())
            } finally {
                pendingResult.finish()
            }
        }
    }
}
