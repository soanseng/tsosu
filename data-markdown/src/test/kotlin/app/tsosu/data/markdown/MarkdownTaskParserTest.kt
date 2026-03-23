package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarkdownTaskParserTest {

    private val parser = MarkdownTaskParser()

    @Test
    fun `empty string returns empty list`() {
        val result = parser.parse("")

        assertEquals(0, result.tasks.size)
        assertTrue(result.projectSections.isEmpty())
    }

    @Test
    fun `frontmatter only returns empty list`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(0, result.tasks.size)
    }

    @Test
    fun `single undone task with id comment`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Buy groceries <!-- id:abc-123 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("abc-123", task.id)
        assertEquals("Buy groceries", task.title)
        assertEquals(TaskStatus.TODO, task.status)
        assertEquals(false, task.done)
        assertNull(task.projectId)
        assertTrue(result.projectSections.isEmpty())
    }

    @Test
    fun `done task with completion date`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [x] Buy groceries ✅ 2026-03-22 <!-- id:abc-123 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals(TaskStatus.DONE, task.status)
        assertEquals(true, task.done)
        assertEquals("Buy groceries", task.title)
        assertNotNull(task.completedDate)
        assertEquals(2026, task.completedDate?.year)
        assertEquals(3, task.completedDate?.monthNumber)
        assertEquals(22, task.completedDate?.dayOfMonth)
    }

    @Test
    fun `task with due date`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Buy groceries 📅 2026-03-25 <!-- id:abc-123 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals(2026, task.dueDate?.year)
        assertEquals(3, task.dueDate?.monthNumber)
        assertEquals(25, task.dueDate?.dayOfMonth)
    }

    @Test
    fun `task with all metadata`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Deep work session 📅 2026-04-01 ⚡high 🍅 30m ⏫ <!-- id:task-42 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("Deep work session", task.title)
        assertEquals(EnergyLevel.HIGH, task.energyLevel)
        assertEquals(30, task.estimatedMinutes)
        assertEquals(Priority.URGENT, task.priority)
        assertEquals(2026, task.dueDate?.year)
        assertEquals(4, task.dueDate?.monthNumber)
        assertEquals(1, task.dueDate?.dayOfMonth)
        assertEquals("task-42", task.id)
    }

    @Test
    fun `section heading maps to project`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Inbox task <!-- id:inbox-1 -->

            ## Work
            - [ ] Work task <!-- id:work-1 -->

            ## Home
            - [ ] Home task <!-- id:home-1 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(3, result.tasks.size)

        // Inbox task should NOT appear in projectSections
        assertTrue("inbox-1" !in result.projectSections)

        // Work and Home tasks should be in projectSections
        assertEquals("Work", result.projectSections["work-1"])
        assertEquals("Home", result.projectSections["home-1"])
    }

    @Test
    fun `task without id gets generated UUID`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Mystery task
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("Mystery task", task.title)
        assertTrue(task.id.isNotBlank(), "Should have a generated ID")
        // UUID format: 8-4-4-4-12 hex chars
        assertTrue(
            task.id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")),
            "Generated ID should be a valid UUID, got: ${task.id}"
        )
    }

    @Test
    fun `round-trip serialize then parse preserves fields`() {
        val serializer = MarkdownTaskSerializer()
        val fixedCreatedAt = Instant.parse("2026-03-20T10:00:00Z")
        val fixedUpdatedAt = Instant.parse("2026-03-22T14:30:00Z")

        val original = Task(
            id = "round-trip-1",
            title = "Plan vacation",
            status = TaskStatus.TODO,
            dueDate = LocalDateTime.parse("2026-06-15T09:00:00"),
            priority = Priority.HIGH,
            energyLevel = EnergyLevel.LOW,
            estimatedMinutes = 45,
            projectId = null,
            position = 0.0,
            createdAt = fixedCreatedAt,
            updatedAt = fixedUpdatedAt,
        )

        val markdown = serializer.serialize(listOf(original))
        val parsed = parser.parse(markdown)

        assertEquals(1, parsed.tasks.size)
        val task = parsed.tasks[0]
        assertEquals(original.id, task.id)
        assertEquals(original.title, task.title)
        assertEquals(original.done, task.done)
        assertEquals(original.priority, task.priority)
        assertEquals(original.energyLevel, task.energyLevel)
        assertEquals(original.estimatedMinutes, task.estimatedMinutes)
        assertEquals(original.dueDate?.date, task.dueDate?.date)
    }

    @Test
    fun `position reflects order of appearance`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] First <!-- id:id-1 -->
            - [ ] Second <!-- id:id-2 -->
            - [ ] Third <!-- id:id-3 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(3, result.tasks.size)
        assertTrue(result.tasks[0].position < result.tasks[1].position)
        assertTrue(result.tasks[1].position < result.tasks[2].position)
    }

    @Test
    fun `all energy levels parsed correctly`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] High energy ⚡high <!-- id:e-1 -->
            - [ ] Medium energy 😐medium <!-- id:e-2 -->
            - [ ] Low energy 🪫low <!-- id:e-3 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(3, result.tasks.size)
        assertEquals(EnergyLevel.HIGH, result.tasks[0].energyLevel)
        assertEquals(EnergyLevel.MEDIUM, result.tasks[1].energyLevel)
        assertEquals(EnergyLevel.LOW, result.tasks[2].energyLevel)
    }

    @Test
    fun `all priority levels parsed correctly`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Highest task ⏫ <!-- id:p-1 -->
            - [ ] High task 🔺 <!-- id:p-2 -->
            - [ ] Medium task 🔼 <!-- id:p-3 -->
            - [ ] Low task 🔽 <!-- id:p-4 -->
            - [ ] No priority task <!-- id:p-5 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(5, result.tasks.size)
        assertEquals(Priority.URGENT, result.tasks[0].priority)
        assertEquals(Priority.HIGH, result.tasks[1].priority)
        assertEquals(Priority.MEDIUM, result.tasks[2].priority)
        assertEquals(Priority.LOW, result.tasks[3].priority)
        assertEquals(Priority.NONE, result.tasks[4].priority)
    }

    @Test
    fun `Obsidian Tasks standard priority emoji parsed correctly`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Highest ⏫ <!-- id:p-1 -->
            - [ ] High 🔺 <!-- id:p-2 -->
            - [ ] Medium 🔼 <!-- id:p-3 -->
            - [ ] Low 🔽 <!-- id:p-4 -->
            - [ ] Lowest ⏬ <!-- id:p-5 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(5, result.tasks.size)
        assertEquals(Priority.URGENT, result.tasks[0].priority)
        assertEquals(Priority.HIGH, result.tasks[1].priority)
        assertEquals(Priority.MEDIUM, result.tasks[2].priority)
        assertEquals(Priority.LOW, result.tasks[3].priority)
        assertEquals(Priority.NONE, result.tasks[4].priority)  // LOWEST maps to NONE
    }

    // --- Extended status tests ---

    @Test
    fun `in-progress task parsed from forward slash checkbox`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [/] Working on it <!-- id:s-1 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        assertEquals(TaskStatus.IN_PROGRESS, result.tasks[0].status)
        assertEquals("Working on it", result.tasks[0].title)
    }

    @Test
    fun `on-hold task parsed from exclamation checkbox`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [!] Blocked task <!-- id:s-2 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        assertEquals(TaskStatus.ON_HOLD, result.tasks[0].status)
        assertEquals("Blocked task", result.tasks[0].title)
    }

    @Test
    fun `planned task parsed from greater-than checkbox`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [>] Scheduled task <!-- id:s-3 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        assertEquals(TaskStatus.PLANNED, result.tasks[0].status)
        assertEquals("Scheduled task", result.tasks[0].title)
    }

    @Test
    fun `cancelled task parsed from dash checkbox`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [-] Dropped task ❌ 2026-03-20 <!-- id:s-4 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        assertEquals(TaskStatus.CANCELLED, result.tasks[0].status)
        assertEquals("Dropped task", result.tasks[0].title)
        assertNotNull(result.tasks[0].cancelledDate)
        assertEquals(2026, result.tasks[0].cancelledDate?.year)
        assertEquals(3, result.tasks[0].cancelledDate?.monthNumber)
        assertEquals(20, result.tasks[0].cancelledDate?.dayOfMonth)
    }

    @Test
    fun `cancelled task without cancelled date has null cancelledDate`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [-] Dropped task <!-- id:s-5 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        assertEquals(TaskStatus.CANCELLED, result.tasks[0].status)
        assertNull(result.tasks[0].cancelledDate)
    }

    // --- New date field parser tests ---

    @Test
    fun `scheduled date parsed from hourglass emoji`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Plan meeting ⏳ 2026-04-05 <!-- id:d-1 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("Plan meeting", task.title)
        assertNotNull(task.scheduledDate)
        assertEquals(2026, task.scheduledDate?.year)
        assertEquals(4, task.scheduledDate?.monthNumber)
        assertEquals(5, task.scheduledDate?.dayOfMonth)
    }

    @Test
    fun `start date parsed from airplane emoji`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Begin project 🛫 2026-04-10 <!-- id:d-2 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("Begin project", task.title)
        assertNotNull(task.startDate)
        assertEquals(2026, task.startDate?.year)
        assertEquals(4, task.startDate?.monthNumber)
        assertEquals(10, task.startDate?.dayOfMonth)
    }

    @Test
    fun `created date parsed from plus emoji`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Some task ➕ 2026-03-15 <!-- id:d-3 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("Some task", task.title)
        // createdAt should be parsed as 2026-03-15T00:00:00Z
        assertEquals(Instant.parse("2026-03-15T00:00:00Z"), task.createdAt)
    }

    @Test
    fun `reminder time parsed from alarm clock emoji`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Call dentist ⏰ 14:30 <!-- id:d-4 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("Call dentist", task.title)
        assertNotNull(task.reminderTime)
        assertEquals(14, task.reminderTime?.hour)
        assertEquals(30, task.reminderTime?.minute)
    }

    @Test
    fun `reminder time with leading zeros parsed correctly`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Early task ⏰ 09:05 <!-- id:d-5 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        assertNotNull(result.tasks[0].reminderTime)
        assertEquals(9, result.tasks[0].reminderTime?.hour)
        assertEquals(5, result.tasks[0].reminderTime?.minute)
    }

    @Test
    fun `recurrence rule parsed from repeat emoji`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Water plants 🔁 every week <!-- id:d-6 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("Water plants", task.title)
        assertEquals("every week", task.recurrenceRule)
    }

    @Test
    fun `recurrence rule with complex pattern`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Review 🔁 every 2 weeks on Monday <!-- id:d-7 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        assertEquals("every 2 weeks on Monday", result.tasks[0].recurrenceRule)
    }

    @Test
    fun `task with all new date fields`() {
        val markdown = """
            ---
            tsosu: v1
            updated: 2026-03-23T12:00:00
            ---

            ## Inbox
            - [ ] Full task 📅 2026-04-01 ⏳ 2026-03-28 🛫 2026-03-25 ➕ 2026-03-15 ⏰ 09:00 🔁 every day <!-- id:full-1 -->
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("Full task", task.title)
        assertEquals(2026, task.dueDate?.year)
        assertEquals(4, task.dueDate?.monthNumber)
        assertEquals(1, task.dueDate?.dayOfMonth)
        assertEquals(2026, task.scheduledDate?.year)
        assertEquals(3, task.scheduledDate?.monthNumber)
        assertEquals(28, task.scheduledDate?.dayOfMonth)
        assertEquals(2026, task.startDate?.year)
        assertEquals(3, task.startDate?.monthNumber)
        assertEquals(25, task.startDate?.dayOfMonth)
        assertEquals(Instant.parse("2026-03-15T00:00:00Z"), task.createdAt)
        assertEquals(9, task.reminderTime?.hour)
        assertEquals(0, task.reminderTime?.minute)
        assertEquals("every day", task.recurrenceRule)
    }

    @Test
    fun `round-trip extended statuses and new fields`() {
        val serializer = MarkdownTaskSerializer()
        val fixedCreatedAt = Instant.parse("2026-03-20T10:00:00Z")
        val fixedUpdatedAt = Instant.parse("2026-03-22T14:30:00Z")

        val original = Task(
            id = "rt-ext-1",
            title = "Extended round-trip",
            status = TaskStatus.IN_PROGRESS,
            dueDate = LocalDateTime.parse("2026-05-01T09:00:00"),
            scheduledDate = LocalDateTime.parse("2026-04-28T09:00:00"),
            startDate = LocalDateTime.parse("2026-04-25T09:00:00"),
            reminderTime = LocalTime(8, 0),
            recurrenceRule = "every month",
            priority = Priority.MEDIUM,
            energyLevel = EnergyLevel.HIGH,
            estimatedMinutes = 60,
            projectId = null,
            position = 0.0,
            createdAt = fixedCreatedAt,
            updatedAt = fixedUpdatedAt,
        )

        val markdown = serializer.serialize(listOf(original))
        val parsed = parser.parse(markdown)

        assertEquals(1, parsed.tasks.size)
        val task = parsed.tasks[0]
        assertEquals(original.id, task.id)
        assertEquals(original.title, task.title)
        assertEquals(original.status, task.status)
        assertEquals(original.priority, task.priority)
        assertEquals(original.energyLevel, task.energyLevel)
        assertEquals(original.estimatedMinutes, task.estimatedMinutes)
        assertEquals(original.dueDate?.date, task.dueDate?.date)
        assertEquals(original.scheduledDate?.date, task.scheduledDate?.date)
        assertEquals(original.startDate?.date, task.startDate?.date)
        assertEquals(original.reminderTime, task.reminderTime)
        assertEquals(original.recurrenceRule, task.recurrenceRule)
    }

    @Test
    fun `round-trip done task preserves completedDate`() {
        val serializer = MarkdownTaskSerializer()
        val fixedCreatedAt = Instant.parse("2026-03-20T10:00:00Z")
        val fixedUpdatedAt = Instant.parse("2026-03-22T14:30:00Z")

        val original = Task(
            id = "rt-done-1",
            title = "Completed task",
            status = TaskStatus.DONE,
            completedDate = LocalDateTime.parse("2026-03-21T10:00:00"),
            position = 0.0,
            createdAt = fixedCreatedAt,
            updatedAt = fixedUpdatedAt,
        )

        val markdown = serializer.serialize(listOf(original))
        val parsed = parser.parse(markdown)

        assertEquals(1, parsed.tasks.size)
        val task = parsed.tasks[0]
        assertEquals(TaskStatus.DONE, task.status)
        assertNotNull(task.completedDate)
        assertEquals(2026, task.completedDate?.year)
        assertEquals(3, task.completedDate?.monthNumber)
        assertEquals(21, task.completedDate?.dayOfMonth)
    }

    @Test
    fun `round-trip cancelled task preserves cancelledDate`() {
        val serializer = MarkdownTaskSerializer()
        val fixedCreatedAt = Instant.parse("2026-03-20T10:00:00Z")
        val fixedUpdatedAt = Instant.parse("2026-03-22T14:30:00Z")

        val original = Task(
            id = "rt-cancel-1",
            title = "Cancelled task",
            status = TaskStatus.CANCELLED,
            cancelledDate = LocalDateTime.parse("2026-03-21T15:00:00"),
            position = 0.0,
            createdAt = fixedCreatedAt,
            updatedAt = fixedUpdatedAt,
        )

        val markdown = serializer.serialize(listOf(original))
        val parsed = parser.parse(markdown)

        assertEquals(1, parsed.tasks.size)
        val task = parsed.tasks[0]
        assertEquals(TaskStatus.CANCELLED, task.status)
        assertNotNull(task.cancelledDate)
        assertEquals(2026, task.cancelledDate?.year)
        assertEquals(3, task.cancelledDate?.monthNumber)
        assertEquals(21, task.cancelledDate?.dayOfMonth)
    }

    @Test
    fun `all extended statuses round-trip correctly`() {
        val serializer = MarkdownTaskSerializer()
        val fixedCreatedAt = Instant.parse("2026-03-20T10:00:00Z")
        val fixedUpdatedAt = Instant.parse("2026-03-22T14:30:00Z")

        for (status in listOf(
            TaskStatus.TODO,
            TaskStatus.IN_PROGRESS,
            TaskStatus.ON_HOLD,
            TaskStatus.PLANNED,
            TaskStatus.DONE,
            TaskStatus.CANCELLED,
        )) {
            val original = Task(
                id = "status-${status.name}",
                title = "Task ${status.name}",
                status = status,
                completedDate = if (status == TaskStatus.DONE) {
                    LocalDateTime.parse("2026-03-22T00:00:00")
                } else null,
                cancelledDate = if (status == TaskStatus.CANCELLED) {
                    LocalDateTime.parse("2026-03-22T00:00:00")
                } else null,
                position = 0.0,
                createdAt = fixedCreatedAt,
                updatedAt = fixedUpdatedAt,
            )

            val markdown = serializer.serialize(listOf(original))
            val parsed = parser.parse(markdown)

            assertEquals(1, parsed.tasks.size, "Should parse one task for status $status")
            assertEquals(
                status,
                parsed.tasks[0].status,
                "Status should round-trip for $status"
            )
        }
    }
}
