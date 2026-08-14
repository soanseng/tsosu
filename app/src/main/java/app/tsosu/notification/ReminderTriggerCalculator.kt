package app.tsosu.notification

import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.domain.model.Task
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Pure trigger-time computation for reminder alarms. Returns null when the
 * task should NOT have an alarm (no reminder/due date, terminal status, or the
 * trigger already passed) — the caller then cancels any stale alarm.
 *
 * [zone] and [nowMillis] are injectable for deterministic tests.
 */
object ReminderTriggerCalculator {

    fun triggerMillisFor(
        task: Task,
        zone: TimeZone = TimeZone.currentSystemDefault(),
        nowMillis: Long = System.currentTimeMillis(),
    ): Long? {
        val dueDate = task.dueDate ?: return null
        val reminder = task.reminderTime ?: return null
        if (task.status.isTerminal) return null

        val trigger = dueDate.date.atTime(reminder)
            .toInstant(zone)
            .toEpochMilliseconds()
        return trigger.takeIf { it > nowMillis }
    }

    fun triggerMillisForEntity(
        task: TaskEntity,
        zone: TimeZone = TimeZone.currentSystemDefault(),
        nowMillis: Long = System.currentTimeMillis(),
    ): Long? {
        val reminderMinutes = task.reminderTimeMinutes ?: return null
        val dueDateMillis = task.dueDate ?: return null
        if (task.status >= 4) return null

        val dueDate = Instant.fromEpochMilliseconds(dueDateMillis)
            .toLocalDateTime(zone).date
        val reminder = LocalTime(reminderMinutes / 60, reminderMinutes % 60)
        val trigger = dueDate.atTime(reminder)
            .toInstant(zone)
            .toEpochMilliseconds()
        return trigger.takeIf { it > nowMillis }
    }
}
