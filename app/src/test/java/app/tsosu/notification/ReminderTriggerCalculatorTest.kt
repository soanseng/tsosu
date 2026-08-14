package app.tsosu.notification

import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReminderTriggerCalculatorTest {

    private val zone = TimeZone.UTC
    private val now = Instant.parse("2026-08-14T00:00:00Z").toEpochMilliseconds()

    private fun task(
        status: TaskStatus = TaskStatus.TODO,
        due: LocalDateTime? = LocalDateTime.parse("2026-08-15T00:00"),
        reminder: LocalTime? = LocalTime(9, 0),
    ) = Task(
        id = "t1",
        title = "Task",
        status = status,
        dueDate = due,
        reminderTime = reminder,
    )

    private fun expectedTrigger(date: String, hour: Int, minute: Int): Long =
        Instant.parse("${date}T${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:00Z")
            .toEpochMilliseconds()

    // --- domain Task ---

    @Test
    fun `computes trigger from due date and reminder time`() {
        val trigger = ReminderTriggerCalculator.triggerMillisFor(task(), zone, now)
        assertEquals(expectedTrigger("2026-08-15", 9, 0), trigger)
    }

    @Test
    fun `null when no reminder time`() {
        assertNull(ReminderTriggerCalculator.triggerMillisFor(task(reminder = null), zone, now))
    }

    @Test
    fun `null when no due date`() {
        assertNull(ReminderTriggerCalculator.triggerMillisFor(task(due = null), zone, now))
    }

    @Test
    fun `null when task is done`() {
        assertNull(ReminderTriggerCalculator.triggerMillisFor(task(status = TaskStatus.DONE), zone, now))
    }

    @Test
    fun `null when task is cancelled`() {
        assertNull(
            ReminderTriggerCalculator.triggerMillisFor(task(status = TaskStatus.CANCELLED), zone, now),
        )
    }

    @Test
    fun `null when trigger already passed`() {
        val past = task(due = LocalDateTime.parse("2026-08-10T00:00"), reminder = LocalTime(9, 0))
        assertNull(ReminderTriggerCalculator.triggerMillisFor(past, zone, now))
    }

    // --- entity ---

    private fun entity(
        status: Int = 0,
        dueDateMillis: Long? = Instant.parse("2026-08-15T00:00:00Z").toEpochMilliseconds(),
        reminderMinutes: Int? = 9 * 60,
    ) = TaskEntity(
        id = "t1",
        title = "Task",
        status = status,
        dueDate = dueDateMillis,
        reminderTimeMinutes = reminderMinutes,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun `computes trigger for entity`() {
        val trigger = ReminderTriggerCalculator.triggerMillisForEntity(entity(), zone, now)
        assertEquals(expectedTrigger("2026-08-15", 9, 0), trigger)
    }

    @Test
    fun `entity null when status is terminal`() {
        assertNull(ReminderTriggerCalculator.triggerMillisForEntity(entity(status = 4), zone, now))
        assertNull(ReminderTriggerCalculator.triggerMillisForEntity(entity(status = 5), zone, now))
    }

    @Test
    fun `entity null when reminder or due missing`() {
        assertNull(ReminderTriggerCalculator.triggerMillisForEntity(entity(reminderMinutes = null), zone, now))
        assertNull(ReminderTriggerCalculator.triggerMillisForEntity(entity(dueDateMillis = null), zone, now))
    }

    @Test
    fun `entity null when trigger passed`() {
        val past = entity(
            dueDateMillis = Instant.parse("2026-08-10T00:00:00Z").toEpochMilliseconds(),
        )
        assertNull(ReminderTriggerCalculator.triggerMillisForEntity(past, zone, now))
    }

    @Test
    fun `entity respects minutes-of-day conversion`() {
        // 14:35 as 14*60+35 minutes
        val e = entity(
            dueDateMillis = Instant.parse("2026-08-16T00:00:00Z").toEpochMilliseconds(),
            reminderMinutes = 14 * 60 + 35,
        )
        val trigger = ReminderTriggerCalculator.triggerMillisForEntity(e, zone, now)
        assertEquals(expectedTrigger("2026-08-16", 14, 35), trigger)
    }
}
