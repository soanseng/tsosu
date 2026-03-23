package app.tsosu.data.markdown.tasknote

import app.tsosu.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskNoteSerializerTest {

    private val serializer = TaskNoteSerializer()

    private val baseTask = Task(
        id = "abc-123",
        title = "Buy Groceries",
        description = "Weekly shopping list",
        status = TaskStatus.TODO,
        priority = Priority.HIGH,
        dueDate = LocalDateTime.parse("2026-03-25T09:00:00"),
        scheduledDate = LocalDateTime.parse("2026-03-24T09:00:00"),
        startDate = LocalDateTime.parse("2026-03-20T08:00:00"),
        reminderTime = LocalTime(14, 30),
        energyLevel = EnergyLevel.MEDIUM,
        estimatedMinutes = 30,
        recurrenceRule = "every week",
        createdAt = Instant.parse("2026-03-18T10:00:00Z"),
        updatedAt = Instant.parse("2026-03-22T14:30:00Z"),
    )

    @Test
    fun `serializes frontmatter with all fields`() {
        val result = serializer.serialize(baseTask, projectName = "Personal")

        assertTrue(result.contains("id: abc-123"))
        assertTrue(result.contains("status: todo"))
        assertTrue(result.contains("priority: high"))
        assertTrue(result.contains("due: 2026-03-25"))
        assertTrue(result.contains("scheduled: 2026-03-24"))
        assertTrue(result.contains("start: 2026-03-20"))
        assertTrue(result.contains("reminder: \"14:30\""))
        assertTrue(result.contains("energy: medium"))
        assertTrue(result.contains("estimate: 30m"))
        assertTrue(result.contains("recurrence: \"every week\""))
        assertTrue(result.contains("project: Personal"))
        assertTrue(result.contains("created: 2026-03-18"))
    }

    @Test
    fun `serializes title as h1 heading`() {
        val result = serializer.serialize(baseTask)
        assertTrue(result.contains("# Buy Groceries"))
    }

    @Test
    fun `serializes description as body`() {
        val result = serializer.serialize(baseTask)
        assertTrue(result.contains("Weekly shopping list"))
    }

    @Test
    fun `omits null fields`() {
        val minimal = Task(
            id = "min-1",
            title = "Simple task",
            createdAt = Instant.parse("2026-03-20T10:00:00Z"),
            updatedAt = Instant.parse("2026-03-20T10:00:00Z"),
        )
        val result = serializer.serialize(minimal)

        assertTrue(result.contains("id: min-1"))
        assertTrue(result.contains("status: todo"))
        assertTrue(!result.contains("due:"))
        assertTrue(!result.contains("scheduled:"))
        assertTrue(!result.contains("reminder:"))
        assertTrue(!result.contains("recurrence:"))
    }

    @Test
    fun `done task includes completed date`() {
        val done = baseTask.copy(
            status = TaskStatus.DONE,
            completedDate = LocalDateTime.parse("2026-03-22T14:30:00"),
        )
        val result = serializer.serialize(done)
        assertTrue(result.contains("status: done"))
        assertTrue(result.contains("completed: 2026-03-22"))
    }

    @Test
    fun `cancelled task includes cancelled date`() {
        val cancelled = baseTask.copy(
            status = TaskStatus.CANCELLED,
            cancelledDate = LocalDateTime.parse("2026-03-22T16:00:00"),
        )
        val result = serializer.serialize(cancelled)
        assertTrue(result.contains("status: cancelled"))
        assertTrue(result.contains("cancelled: 2026-03-22"))
    }

    @Test
    fun `generates slug filename from title`() {
        val slug = serializer.slugify("Buy Groceries for the Week!")
        assertTrue(slug == "buy-groceries-for-the-week")
    }

    @Test
    fun `slug handles unicode and special chars`() {
        val slug = serializer.slugify("\u51A5\u60F3 & Exercise (30m)")
        assertTrue(slug == "\u51A5\u60F3-exercise-30m")
    }
}
