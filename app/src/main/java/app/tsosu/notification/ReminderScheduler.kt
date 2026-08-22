package app.tsosu.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.tsosu.data.local.entity.HabitEntity
import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules (or cancels) both reminder slots for a task: the due reminder
     * and, when the task has a start date, the start reminder at the same
     * wall-clock time on the start date.
     */
    fun schedule(task: Task) {
        val trigger = ReminderTriggerCalculator.triggerMillisFor(task)
        if (trigger != null) schedule(task.id, trigger) else cancel(task.id)

        val startTrigger = ReminderTriggerCalculator.triggerStartMillisFor(task)
        if (startTrigger != null) scheduleStart(task.id, startTrigger) else cancelStart(task.id)
    }
    /**
     * Reconciles alarms with the current task set (e.g. after boot or after an
     * import/sync brought in changed due dates or reminders). Clears stale alarms.
     */
    fun rescheduleAll(tasks: List<TaskEntity>) {
        for (task in tasks) {
            val trigger = ReminderTriggerCalculator.triggerMillisForEntity(task)
            if (trigger != null) schedule(task.id, trigger) else cancel(task.id)

            val startTrigger = ReminderTriggerCalculator.triggerStartMillisForEntity(task)
            if (startTrigger != null) scheduleStart(task.id, startTrigger) else cancelStart(task.id)
        }
    }
    /**
     * Reconciles daily habit reminder alarms with the current habit set.
     * Habits without a reminder (or archived) get their alarm cancelled.
     */
    fun rescheduleHabits(habits: List<HabitEntity>) {
        for (habit in habits) {
            val trigger = ReminderTriggerCalculator.triggerMillisForHabit(
                reminderMinutes = habit.reminderMinutes,
                isArchived = habit.isArchived,
            )
            if (trigger != null) scheduleHabit(habit.id, trigger) else cancelHabit(habit.id)
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

    fun scheduleStart(taskId: String, triggerAtMillis: Long) {
        val pendingIntent = buildStartPendingIntent(taskId)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelStart(taskId: String) {
        alarmManager.cancel(buildStartPendingIntent(taskId))
    }

    fun scheduleHabit(habitId: String, triggerAtMillis: Long) {
        val pendingIntent = buildHabitPendingIntent(habitId)

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancelHabit(habitId: String) {
        val pendingIntent = buildHabitPendingIntent(habitId)
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

    private fun buildStartPendingIntent(taskId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_START_REMINDER
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            "start:$taskId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildHabitPendingIntent(habitId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_HABIT_REMINDER
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getBroadcast(
            context,
            "habit:$habitId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
