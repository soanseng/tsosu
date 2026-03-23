package app.tsosu.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskTest {

    @Test
    fun `default task has TODO status and null optional fields`() {
        val task = Task(title = "Test task")

        assertEquals(TaskStatus.TODO, task.status)
        assertFalse(task.done)
        assertNull(task.scheduledDate)
        assertNull(task.startDate)
        assertNull(task.reminderTime)
        assertNull(task.completedDate)
        assertNull(task.cancelledDate)
        assertNull(task.recurrenceRule)
    }

    @Test
    fun `done computed property delegates to status isDone`() {
        val todoTask = Task(title = "Todo", status = TaskStatus.TODO)
        assertFalse(todoTask.done)

        val doneTask = Task(title = "Done", status = TaskStatus.DONE)
        assertTrue(doneTask.done)

        val inProgressTask = Task(title = "WIP", status = TaskStatus.IN_PROGRESS)
        assertFalse(inProgressTask.done)

        val cancelledTask = Task(title = "Cancelled", status = TaskStatus.CANCELLED)
        assertFalse(cancelledTask.done)
    }

    @Test
    fun `task with all new fields`() {
        val scheduled = LocalDateTime(2026, 3, 25, 9, 0)
        val start = LocalDateTime(2026, 3, 25, 10, 0)
        val reminder = LocalTime(8, 30)
        val completed = LocalDateTime(2026, 3, 25, 12, 0)
        val cancelled = LocalDateTime(2026, 3, 26, 9, 0)
        val recurrence = "FREQ=DAILY;INTERVAL=1"

        val task = Task(
            title = "Full task",
            status = TaskStatus.DONE,
            scheduledDate = scheduled,
            startDate = start,
            reminderTime = reminder,
            completedDate = completed,
            cancelledDate = cancelled,
            recurrenceRule = recurrence,
        )

        assertEquals(TaskStatus.DONE, task.status)
        assertTrue(task.done)
        assertEquals(scheduled, task.scheduledDate)
        assertEquals(start, task.startDate)
        assertEquals(reminder, task.reminderTime)
        assertEquals(completed, task.completedDate)
        assertEquals(cancelled, task.cancelledDate)
        assertEquals(recurrence, task.recurrenceRule)
    }
}
