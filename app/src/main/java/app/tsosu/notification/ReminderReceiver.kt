package app.tsosu.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import app.tsosu.VaultChangeWatcher
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.GamificationRepository
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "app.tsosu.ACTION_REMINDER"
        const val ACTION_COMPLETE = "app.tsosu.ACTION_COMPLETE"
        const val ACTION_SNOOZE = "app.tsosu.ACTION_SNOOZE"
        const val ACTION_START_REMINDER = "app.tsosu.ACTION_START_REMINDER"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val SNOOZE_MINUTES = 10L
    }

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var watcher: VaultChangeWatcher
    @Inject lateinit var gamificationRepository: GamificationRepository
    @Inject lateinit var setTaskStatus: SetTaskStatusUseCase

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMINDER -> intent.getStringExtra(EXTRA_TASK_ID)?.let { handleReminder(it) }
            ACTION_COMPLETE -> intent.getStringExtra(EXTRA_TASK_ID)?.let { handleComplete(context, it) }
            ACTION_SNOOZE -> intent.getStringExtra(EXTRA_TASK_ID)?.let { handleSnooze(context, it) }
            ACTION_START_REMINDER -> intent.getStringExtra(EXTRA_TASK_ID)?.let { handleStartReminder(it) }
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

    /**
     * Completes the task through the repository (SetTaskStatusUseCase) so the
     * real completion semantics run: recurring tasks reset to TODO with their
     * next occurrence, completions are recorded, energy is awarded once, and
     * the calendar event is synced. A terminal task is a no-op (guards against
     * double-awarding energy on repeated taps of a stale notification).
     */
    private fun handleComplete(context: Context, taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val current = taskDao.getByIdSync(taskId) ?: return@launch
                if (TaskStatus.fromOrdinal(current.status).isTerminal) return@launch
                setTaskStatus(taskId, TaskStatus.DONE).onSuccess { task ->
                    // schedule() arms the next reminder for a recurring task and
                    // cancels for terminal ones.
                    reminderScheduler.schedule(task)
                    NotificationManagerCompat.from(context).cancel(taskId.hashCode())
                    watcher.pushSoon()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Start-time nudge: fires at the task's start date + reminder time. */
    private fun handleStartReminder(taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskDao.getByIdSync(taskId) ?: return@launch
                if (task.status >= 4) return@launch
                notificationHelper.showStartReminder(
                    taskId = task.id,
                    title = task.title,
                    notificationId = "start:${task.id}".hashCode(),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Snoozes the reminder: dismiss now, re-arm the alarm [SNOOZE_MINUTES] later. */
    private fun handleSnooze(context: Context, taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskDao.getByIdSync(taskId) ?: return@launch
                if (TaskStatus.fromOrdinal(task.status).isTerminal) return@launch
                NotificationManagerCompat.from(context).cancel(taskId.hashCode())
                reminderScheduler.schedule(
                    taskId = taskId,
                    triggerAtMillis = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
