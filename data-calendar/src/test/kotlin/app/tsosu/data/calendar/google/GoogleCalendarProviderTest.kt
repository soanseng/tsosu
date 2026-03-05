package app.tsosu.data.calendar.google

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoogleCalendarProviderTest {

    private val provider = GoogleCalendarProvider()

    @Test
    fun `builds event from task with due date and estimated minutes`() {
        val task = Task(
            id = "abc-123",
            title = "Write report",
            description = "Draft for Monday",
            dueDate = LocalDateTime(2026, 3, 15, 10, 0),
            estimatedMinutes = 45,
        )

        val event = provider.buildEvent(task)

        assertEquals("tsosu-abc-123", event.id)
        assertEquals("Write report", event.summary)
        assertEquals("Draft for Monday", event.description)
        assertNotNull(event.start)
        assertNotNull(event.end)
    }

    @Test
    fun `uses default 60 min duration when no estimate`() {
        val task = Task(
            id = "abc-456",
            title = "Quick task",
            dueDate = LocalDateTime(2026, 3, 15, 14, 0),
            estimatedMinutes = null,
        )

        val event = provider.buildEvent(task)

        assertEquals("tsosu-abc-456", event.id)
        // End should be 60 minutes after start
        val startMillis = event.start.dateTime.value
        val endMillis = event.end.dateTime.value
        assertEquals(60 * 60 * 1000L, endMillis - startMillis)
    }

    @Test
    fun `event id uses tsosu prefix`() {
        val task = Task(
            id = "my-task-id",
            title = "Test",
            dueDate = LocalDateTime(2026, 1, 1, 9, 0),
        )

        val event = provider.buildEvent(task)
        assertEquals("tsosu-my-task-id", event.id)
    }

    @Test
    fun `handles task with empty description`() {
        val task = Task(
            id = "no-desc",
            title = "No description task",
            description = "",
            dueDate = LocalDateTime(2026, 6, 1, 8, 0),
        )

        val event = provider.buildEvent(task)
        assertEquals("No description task", event.summary)
        assertTrue(event.description.isNullOrEmpty())
    }

    @Test
    fun `builds event with correct start time`() {
        val task = Task(
            id = "time-test",
            title = "Morning meeting",
            dueDate = LocalDateTime(2026, 12, 25, 9, 30),
            estimatedMinutes = 30,
        )

        val event = provider.buildEvent(task)
        // Verify the dateTime is set correctly
        assertTrue(event.start.dateTime.toStringRfc3339().contains("2026-12-25"))
    }
}
