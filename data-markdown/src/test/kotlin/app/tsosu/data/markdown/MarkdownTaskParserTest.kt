package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertEquals(true, task.done)
        assertEquals("Buy groceries", task.title)
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
}
