package app.tsosu.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import app.tsosu.VaultChangeWatcher
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.HabitCompletionEntity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "app.tsosu.ACTION_REMINDER"
        const val ACTION_COMPLETE = "app.tsosu.ACTION_COMPLETE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val ACTION_HABIT_REMINDER = "app.tsosu.ACTION_HABIT_REMINDER"
        const val ACTION_HABIT_COMPLETE = "app.tsosu.ACTION_HABIT_COMPLETE"
        const val EXTRA_HABIT_ID = "extra_habit_id"
    }

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var habitDao: HabitDao
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var watcher: VaultChangeWatcher

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMINDER -> intent.getStringExtra(EXTRA_TASK_ID)?.let { handleReminder(it) }
            ACTION_COMPLETE -> intent.getStringExtra(EXTRA_TASK_ID)?.let { handleComplete(context, it) }
            ACTION_HABIT_REMINDER -> intent.getStringExtra(EXTRA_HABIT_ID)?.let { handleHabitReminder(it) }
            ACTION_HABIT_COMPLETE -> intent.getStringExtra(EXTRA_HABIT_ID)?.let { handleHabitComplete(context, it) }
        }
    }

    private fun handleReminder(taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskDao.getByIdSync(taskId) ?: return@launch
                if (task.status >= 4) return@launch // terminal: no reminder
                notificationHelper.showReminder(
                    taskId = task.id,
                    title = task.title,
                    notificationId = task.id.hashCode(),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleComplete(context: Context, taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                taskDao.setStatus(
                    taskId = taskId,
                    status = 4, // TaskStatus.DONE ordinal
                    completedDate = now,
                    cancelledDate = null,
                    updatedAt = now,
                )
                reminderScheduler.cancel(taskId)
                NotificationManagerCompat.from(context).cancel(taskId.hashCode())
                watcher.pushSoon()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleHabitReminder(habitId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = habitDao.getByIdSync(habitId) ?: return@launch

                // Always perpetuate the daily alarm (skip only when archived/no reminder).
                rescheduleNext(habitId, habit.reminderMinutes, habit.isArchived)
                if (habit.isArchived || habit.reminderMinutes == null) return@launch

                val todayEpoch = todayEpoch()
                val completedToday = habitDao.getCompletionsForDate(todayEpoch).first()
                    .any { it.habitId == habitId }
                if (!completedToday) {
                    notificationHelper.showHabitReminder(
                        habitId = habit.id,
                        title = habit.title,
                        text = habit.tinyVersion,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleHabitComplete(context: Context, habitId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val habit = habitDao.getByIdSync(habitId) ?: return@launch
                val now = Clock.System.now().toEpochMilliseconds()
                habitDao.insertCompletionOnce(
                    habitId = habitId,
                    date = todayEpoch(),
                    completedAt = now,
                )
                NotificationManagerCompat.from(context).cancel(habitId.hashCode())
                rescheduleNext(habitId, habit.reminderMinutes, habit.isArchived)
                watcher.pushSoon()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleNext(habitId: String, reminderMinutes: Int?, isArchived: Boolean) {
        val trigger = ReminderTriggerCalculator.triggerMillisForHabit(
            reminderMinutes = reminderMinutes,
            isArchived = isArchived,
        )
        if (trigger != null) reminderScheduler.scheduleHabit(habitId, trigger)
        else reminderScheduler.cancelHabit(habitId)
    }

    private fun todayEpoch(): Long =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            .atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}
