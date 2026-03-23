package app.tsosu.data.markdown.tasknote

import app.tsosu.data.markdown.YamlFrontmatterParser
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskNoteParserTest {

    private val parser = TaskNoteParser()

    @Test
    fun `parse full TaskNote`() {
        val content = """
            ---
            id: abc-123
            status: todo
            priority: high
            due: 2026-03-25
            scheduled: 2026-03-24
            start: 2026-03-20
            reminder: "14:30"
            energy: medium
            estimate: 30m
            recurrence: "every week"
            project: Personal
            created: 2026-03-18
            ---

            # Buy Groceries

            Weekly shopping list.
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals("abc-123", result.task.id)
        assertEquals("Buy Groceries", result.task.title)
        assertEquals("Weekly shopping list.", result.task.description.trim())
        assertEquals(TaskStatus.TODO, result.task.status)
        assertEquals(Priority.HIGH, result.task.priority)
        assertEquals(25, result.task.dueDate?.dayOfMonth)
        assertEquals(24, result.task.scheduledDate?.dayOfMonth)
        assertEquals(20, result.task.startDate?.dayOfMonth)
        assertEquals(14, result.task.reminderTime?.hour)
        assertEquals(30, result.task.reminderTime?.minute)
        assertEquals(EnergyLevel.MEDIUM, result.task.energyLevel)
        assertEquals(30, result.task.estimatedMinutes)
        assertEquals("every week", result.task.recurrenceRule)
        assertEquals("Personal", result.projectName)
    }

    @Test
    fun `parse minimal TaskNote`() {
        val content = """
            ---
            id: min-1
            status: todo
            created: 2026-03-20
            ---

            # Simple task
        """.trimIndent()

        val result = parser.parse(content)
        assertEquals("min-1", result.task.id)
        assertEquals("Simple task", result.task.title)
        assertEquals(TaskStatus.TODO, result.task.status)
        assertNull(result.task.dueDate)
        assertNull(result.task.scheduledDate)
        assertNull(result.task.reminderTime)
        assertNull(result.projectName)
    }

    @Test
    fun `parse done task`() {
        val content = """
            ---
            id: done-1
            status: done
            completed: 2026-03-22
            created: 2026-03-18
            ---

            # Finished task
        """.trimIndent()

        val result = parser.parse(content)
        assertEquals(TaskStatus.DONE, result.task.status)
        assertEquals(22, result.task.completedDate?.dayOfMonth)
    }

    @Test
    fun `parse cancelled task`() {
        val content = """
            ---
            id: cancel-1
            status: cancelled
            cancelled: 2026-03-22
            created: 2026-03-18
            ---

            # Cancelled task
        """.trimIndent()

        val result = parser.parse(content)
        assertEquals(TaskStatus.CANCELLED, result.task.status)
        assertEquals(22, result.task.cancelledDate?.dayOfMonth)
    }

    @Test
    fun `title extracted from h1 heading, rest is description`() {
        val content = """
            ---
            id: body-1
            status: todo
            created: 2026-03-20
            ---

            # Main Title

            First paragraph.

            ## Subtasks
            - [ ] Sub 1
            - [ ] Sub 2
        """.trimIndent()

        val result = parser.parse(content)
        assertEquals("Main Title", result.task.title)
        assertTrue(result.task.description.contains("First paragraph."))
        assertTrue(result.task.description.contains("## Subtasks"))
    }

    @Test
    fun `round-trip serialize then parse preserves fields`() {
        val original = Task(
            id = "rt-1",
            title = "Round Trip",
            description = "Test body",
            status = TaskStatus.IN_PROGRESS,
            priority = Priority.MEDIUM,
            dueDate = LocalDateTime.parse("2026-04-01T00:00:00"),
            energyLevel = EnergyLevel.HIGH,
            estimatedMinutes = 60,
            createdAt = Instant.parse("2026-03-20T10:00:00Z"),
            updatedAt = Instant.parse("2026-03-22T10:00:00Z"),
        )

        val projectName = "Work"
        val markdown = serializeForTest(original, projectName)
        val parsed = parser.parse(markdown)

        assertEquals(original.id, parsed.task.id)
        assertEquals(original.title, parsed.task.title)
        assertEquals(original.status, parsed.task.status)
        assertEquals(original.priority, parsed.task.priority)
        assertEquals(original.dueDate?.date, parsed.task.dueDate?.date)
        assertEquals(original.energyLevel, parsed.task.energyLevel)
        assertEquals(original.estimatedMinutes, parsed.task.estimatedMinutes)
        assertEquals("Work", parsed.projectName)
    }

    /**
     * Minimal serializer for round-trip testing until TaskNoteSerializer is available.
     */
    private fun serializeForTest(task: Task, projectName: String?): String {
        val yamlHelper = YamlFrontmatterParser()
        val fm = linkedMapOf<String, String>()

        fm["id"] = task.id
        fm["status"] = when (task.status) {
            TaskStatus.TODO -> "todo"
            TaskStatus.IN_PROGRESS -> "in_progress"
            TaskStatus.ON_HOLD -> "on_hold"
            TaskStatus.PLANNED -> "planned"
            TaskStatus.DONE -> "done"
            TaskStatus.CANCELLED -> "cancelled"
        }
        when (task.priority) {
            Priority.NONE -> {}
            Priority.LOW -> fm["priority"] = "low"
            Priority.MEDIUM -> fm["priority"] = "medium"
            Priority.HIGH -> fm["priority"] = "high"
            Priority.URGENT -> fm["priority"] = "urgent"
        }
        task.dueDate?.let { fm["due"] = it.date.toString() }
        task.scheduledDate?.let { fm["scheduled"] = it.date.toString() }
        task.startDate?.let { fm["start"] = it.date.toString() }
        task.reminderTime?.let {
            fm["reminder"] = "%02d:%02d".format(it.hour, it.minute)
        }
        task.completedDate?.let { fm["completed"] = it.date.toString() }
        task.cancelledDate?.let { fm["cancelled"] = it.date.toString() }
        fm["energy"] = when (task.energyLevel) {
            EnergyLevel.LOW -> "low"
            EnergyLevel.MEDIUM -> "medium"
            EnergyLevel.HIGH -> "high"
        }
        task.estimatedMinutes?.let { fm["estimate"] = "${it}m" }
        task.recurrenceRule?.let { fm["recurrence"] = it }
        projectName?.let { fm["project"] = it }
        fm["created"] = task.createdAt.toLocalDateTime(TimeZone.UTC).date.toString()

        val body = buildString {
            appendLine("# ${task.title}")
            if (task.description.isNotBlank()) {
                appendLine()
                append(task.description)
            }
        }

        return yamlHelper.serialize(fm, body)
    }
}
