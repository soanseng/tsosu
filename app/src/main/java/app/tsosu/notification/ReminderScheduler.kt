package app.tsosu.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules (or cancels) the reminder for a single domain task.
     * Terminal tasks, tasks without reminder/due date, and past triggers are cancelled.
     */
    fun schedule(task: Task) {
        val dueDate = task.dueDate ?: return cancel(task.id)
        val reminderTime = task.reminderTime ?: return cancel(task.id)
        if (task.status.isTerminal) return cancel(task.id)

        val zone = TimeZone.currentSystemDefault()
        val triggerMillis = dueDate.date.atTime(reminderTime)
            .toInstant(zone)
            .toEpochMilliseconds()

        if (triggerMillis > System.currentTimeMillis()) {
            schedule(task.id, triggerMillis)
        } else {
            cancel(task.id)
        }
    }

    /**
     * Reconciles alarms with the current task set (e.g. after boot or after an
     * import/sync brought in changed due dates or reminders). Clears stale alarms.
     */
    fun rescheduleAll(tasks: List<TaskEntity>) {
        val now = System.currentTimeMillis()
        val zone = TimeZone.currentSystemDefault()

        for (task in tasks) {
            val reminderMinutes = task.reminderTimeMinutes
            val dueDateMillis = task.dueDate
            if (task.status >= 4 || reminderMinutes == null || dueDateMillis == null) {
                cancel(task.id)
                continue
            }

            val taskDueDate = Instant.fromEpochMilliseconds(dueDateMillis)
                .toLocalDateTime(zone).date
            val reminderTime = LocalTime(reminderMinutes / 60, reminderMinutes % 60)
            val triggerMillis = taskDueDate.atTime(reminderTime)
                .toInstant(zone)
                .toEpochMilliseconds()

            if (triggerMillis > now) {
                schedule(task.id, triggerMillis)
            } else {
                cancel(task.id)
            }
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
    }

    fun schedule(taskId: String, triggerAtMillis: Long) {
        val pendingIntent = buildPendingIntent(taskId)

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            // Exact alarms not permitted: degrade to inexact so reminders still fire
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancel(taskId: String) {
        val pendingIntent = buildPendingIntent(taskId)
        alarmManager.cancel(pendingIntent)
    }

    private fun buildPendingIntent(taskId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
