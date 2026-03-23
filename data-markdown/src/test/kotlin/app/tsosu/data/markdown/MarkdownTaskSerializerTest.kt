package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarkdownTaskSerializerTest {

    private val serializer = MarkdownTaskSerializer()

    private val fixedCreatedAt = Instant.parse("2026-03-20T10:00:00Z")
    private val fixedUpdatedAt = Instant.parse("2026-03-22T14:30:00Z")

    private fun task(
        id: String = "test-id-1",
        title: String = "Buy groceries",
        status: TaskStatus = TaskStatus.TODO,
        dueDate: LocalDateTime? = null,
        scheduledDate: LocalDateTime? = null,
        startDate: LocalDateTime? = null,
        completedDate: LocalDateTime? = null,
        cancelledDate: LocalDateTime? = null,
        reminderTime: LocalTime? = null,
        recurrenceRule: String? = null,
        priority: Priority = Priority.NONE,
        energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
        estimatedMinutes: Int? = null,
        projectId: String? = null,
        position: Double = 0.0,
        createdAt: Instant = fixedCreatedAt,
        updatedAt: Instant = fixedUpdatedAt,
    ) = Task(
        id = id,
        title = title,
        status = status,
        dueDate = dueDate,
        scheduledDate = scheduledDate,
        startDate = startDate,
        completedDate = completedDate,
        cancelledDate = cancelledDate,
        reminderTime = reminderTime,
        recurrenceRule = recurrenceRule,
        priority = priority,
        energyLevel = energyLevel,
        estimatedMinutes = estimatedMinutes,
        projectId = projectId,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    @Test
    fun `empty list produces frontmatter and Inbox section`() {
        val result = serializer.serialize(emptyList())

        assertTrue(result.startsWith("---\n"), "Should start with frontmatter")
        assertTrue(result.contains("tsosu: v1"), "Should contain version marker")
        assertTrue(result.contains("updated:"), "Should contain updated timestamp")
        assertTrue(result.contains("---\n"), "Should close frontmatter")
        assertTrue(result.contains("## Inbox"), "Should contain Inbox section")
    }

    @Test
    fun `single inbox task serializes as unchecked item`() {
        val result = serializer.serialize(listOf(task()))

        assertTrue(result.contains("- [ ] Buy groceries"), "Should have unchecked checkbox")
        assertTrue(result.contains("<!-- id:test-id-1 -->"), "Should have hidden ID comment")
    }

    @Test
    fun `done task has checked box and completion date`() {
        val result = serializer.serialize(
            listOf(task(status = TaskStatus.DONE))
        )

        assertTrue(result.contains("- [x] Buy groceries"), "Should have checked checkbox")
        assertTrue(
            result.contains("\u2705 2026-03-22"),
            "Should have completion date from updatedAt"
        )
    }

    @Test
    fun `done task with explicit completedDate uses it`() {
        val completed = LocalDateTime.parse("2026-03-21T09:00:00")
        val result = serializer.serialize(
            listOf(task(status = TaskStatus.DONE, completedDate = completed))
        )

        assertTrue(result.contains("- [x] Buy groceries"), "Should have checked checkbox")
        assertTrue(
            result.contains("\u2705 2026-03-21"),
            "Should use completedDate instead of updatedAt"
        )
    }

    @Test
    fun `task with due date includes calendar emoji and date`() {
        val due = LocalDateTime.parse("2026-03-25T09:00:00")
        val result = serializer.serialize(
            listOf(task(dueDate = due))
        )

        assertTrue(
            result.contains("\uD83D\uDCC5 2026-03-25"),
            "Should contain due date with calendar emoji"
        )
    }

    @Test
    fun `task with all metadata includes every field`() {
        val due = LocalDateTime.parse("2026-04-01T08:00:00")
        val result = serializer.serialize(
            listOf(
                task(
                    title = "Deep work session",
                    status = TaskStatus.TODO,
                    dueDate = due,
                    priority = Priority.URGENT,
                    energyLevel = EnergyLevel.HIGH,
                    estimatedMinutes = 30,
                )
            )
        )

        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(taskLine.contains("Deep work session"), "Title present")
        assertTrue(
            taskLine.contains("\uD83D\uDCC5 2026-04-01"),
            "Due date present"
        )
        assertTrue(taskLine.contains("\u26A1high"), "Energy level present")
        assertTrue(
            taskLine.contains("\uD83C\uDF45 30m"),
            "Estimate present"
        )
        assertTrue(taskLine.contains("\u23EB"), "Priority present")
        assertTrue(
            taskLine.contains("<!-- id:test-id-1 -->"),
            "ID comment present"
        )
        assertTrue(taskLine.contains("\u2795 2026-03-20"), "Created date present")
    }

    @Test
    fun `tasks grouped by project as sections`() {
        val tasks = listOf(
            task(id = "inbox-1", title = "Inbox task", projectId = null, position = 1.0),
            task(id = "work-1", title = "Work task", projectId = "proj-1", position = 1.0),
            task(id = "home-1", title = "Home task", projectId = "proj-2", position = 1.0),
        )
        val projectNames = mapOf("proj-1" to "Work", "proj-2" to "Home")

        val result = serializer.serialize(tasks, projectNames)

        assertTrue(result.contains("## Inbox"), "Should have Inbox section")
        assertTrue(result.contains("## Work"), "Should have Work section")
        assertTrue(result.contains("## Home"), "Should have Home section")

        // Inbox section should contain the inbox task
        val inboxSection = extractSection(result, "Inbox")
        assertTrue(
            inboxSection.contains("Inbox task"),
            "Inbox section contains inbox task"
        )

        // Work section should contain the work task
        val workSection = extractSection(result, "Work")
        assertTrue(
            workSection.contains("Work task"),
            "Work section contains work task"
        )

        // Home section should contain the home task
        val homeSection = extractSection(result, "Home")
        assertTrue(
            homeSection.contains("Home task"),
            "Home section contains home task"
        )
    }

    @Test
    fun `tasks sorted by position within sections`() {
        val tasks = listOf(
            task(id = "id-3", title = "Third", position = 30.0),
            task(id = "id-1", title = "First", position = 10.0),
            task(id = "id-2", title = "Second", position = 20.0),
        )

        val result = serializer.serialize(tasks)
        val taskLines = result.lines().filter { it.startsWith("- [") }

        assertEquals(3, taskLines.size, "Should have 3 task lines")
        assertTrue(taskLines[0].contains("First"), "First by position")
        assertTrue(taskLines[1].contains("Second"), "Second by position")
        assertTrue(taskLines[2].contains("Third"), "Third by position")
    }

    @Test
    fun `energy level always emitted even for MEDIUM`() {
        val result = serializer.serialize(
            listOf(task(energyLevel = EnergyLevel.MEDIUM))
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(
            taskLine.contains("\uD83D\uDE10medium"),
            "MEDIUM energy should be emitted"
        )
    }

    @Test
    fun `priority NONE is omitted`() {
        val result = serializer.serialize(
            listOf(task(priority = Priority.NONE))
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        // None of the priority markers should appear
        assertTrue(!taskLine.contains("\u23EB"), "No URGENT marker")
        assertTrue(!taskLine.contains("\uD83D\uDD3A"), "No HIGH marker")
        assertTrue(!taskLine.contains("\uD83D\uDD3C"), "No MEDIUM marker")
        assertTrue(!taskLine.contains("\uD83D\uDD3D"), "No LOW marker")
    }

    @Test
    fun `priority uses Obsidian Tasks standard emoji`() {
        for ((priority, expectedMarker) in listOf(
            Priority.LOW to "\uD83D\uDD3D",
            Priority.MEDIUM to "\uD83D\uDD3C",
            Priority.HIGH to "\uD83D\uDD3A",
            Priority.URGENT to "\u23EB",
        )) {
            val result = serializer.serialize(listOf(task(priority = priority)))
            val taskLine = result.lines().first { it.startsWith("- [") }
            assertTrue(
                taskLine.contains(expectedMarker),
                "Priority $priority should produce marker '$expectedMarker', got: $taskLine"
            )
        }
    }

    @Test
    fun `unknown projectId uses fallback section name`() {
        val tasks = listOf(
            task(id = "orphan-1", title = "Orphan task", projectId = "unknown-id"),
        )
        val result = serializer.serialize(tasks, emptyMap())

        // Should still create a section for the unknown project
        assertTrue(
            result.contains("## unknown-id"),
            "Unknown project should use ID as fallback section name"
        )
    }

    // --- Extended status tests ---

    @Test
    fun `in-progress task uses forward slash checkbox`() {
        val result = serializer.serialize(listOf(task(status = TaskStatus.IN_PROGRESS)))
        assertTrue(result.contains("- [/] Buy groceries"), "Should have [/] checkbox")
    }

    @Test
    fun `on-hold task uses exclamation checkbox`() {
        val result = serializer.serialize(listOf(task(status = TaskStatus.ON_HOLD)))
        assertTrue(result.contains("- [!] Buy groceries"), "Should have [!] checkbox")
    }

    @Test
    fun `planned task uses greater-than checkbox`() {
        val result = serializer.serialize(listOf(task(status = TaskStatus.PLANNED)))
        assertTrue(result.contains("- [>] Buy groceries"), "Should have [>] checkbox")
    }

    @Test
    fun `cancelled task uses dash checkbox`() {
        val result = serializer.serialize(listOf(task(status = TaskStatus.CANCELLED)))
        assertTrue(result.contains("- [-] Buy groceries"), "Should have [-] checkbox")
    }

    @Test
    fun `cancelled task with cancelledDate emits cancelled date`() {
        val cancelled = LocalDateTime.parse("2026-03-21T15:00:00")
        val result = serializer.serialize(
            listOf(task(status = TaskStatus.CANCELLED, cancelledDate = cancelled))
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(taskLine.contains("- [-] Buy groceries"), "Should have [-] checkbox")
        assertTrue(taskLine.contains("\u274C 2026-03-21"), "Should have cancelled date")
    }

    @Test
    fun `cancelled task without cancelledDate omits cancelled date`() {
        val result = serializer.serialize(
            listOf(task(status = TaskStatus.CANCELLED))
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertFalse(taskLine.contains("\u274C"), "Should not have cancelled date emoji")
    }

    @Test
    fun `non-done task does not emit completion date`() {
        val result = serializer.serialize(listOf(task(status = TaskStatus.IN_PROGRESS)))
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertFalse(taskLine.contains("\u2705"), "Non-DONE task should not have completion date")
    }

    // --- New date field tests ---

    @Test
    fun `scheduled date emits hourglass emoji`() {
        val scheduled = LocalDateTime.parse("2026-04-05T09:00:00")
        val result = serializer.serialize(
            listOf(task(scheduledDate = scheduled))
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(taskLine.contains("\u23F3 2026-04-05"), "Should have scheduled date")
    }

    @Test
    fun `start date emits airplane emoji`() {
        val start = LocalDateTime.parse("2026-04-10T08:00:00")
        val result = serializer.serialize(
            listOf(task(startDate = start))
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(taskLine.contains("\uD83D\uDEEB 2026-04-10"), "Should have start date")
    }

    @Test
    fun `created date always emitted from createdAt`() {
        val result = serializer.serialize(listOf(task()))
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(taskLine.contains("\u2795 2026-03-20"), "Should have created date")
    }

    @Test
    fun `reminder time emits alarm clock emoji`() {
        val result = serializer.serialize(
            listOf(task(reminderTime = LocalTime(14, 30)))
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(taskLine.contains("\u23F0 14:30"), "Should have reminder time")
    }

    @Test
    fun `reminder time pads single digit hours and minutes`() {
        val result = serializer.serialize(
            listOf(task(reminderTime = LocalTime(9, 5)))
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(taskLine.contains("\u23F0 09:05"), "Should have zero-padded reminder time")
    }

    @Test
    fun `recurrence rule emits repeat emoji`() {
        val result = serializer.serialize(
            listOf(task(recurrenceRule = "every week"))
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(
            taskLine.contains("\uD83D\uDD01 every week"),
            "Should have recurrence rule"
        )
    }

    @Test
    fun `null optional dates are omitted`() {
        val result = serializer.serialize(listOf(task()))
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertFalse(taskLine.contains("\u23F3"), "No scheduled date")
        assertFalse(taskLine.contains("\uD83D\uDEEB"), "No start date")
        assertFalse(taskLine.contains("\u23F0"), "No reminder time")
        assertFalse(taskLine.contains("\uD83D\uDD01"), "No recurrence")
        assertFalse(taskLine.contains("\u274C"), "No cancelled date")
        assertFalse(taskLine.contains("\u2705"), "No completion date for TODO")
    }

    /** Extract the content between a `## Name` heading and the next heading (or EOF). */
    private fun extractSection(markdown: String, sectionName: String): String {
        val lines = markdown.lines()
        val startIdx = lines.indexOfFirst { it.trim() == "## $sectionName" }
        if (startIdx == -1) return ""
        val endIdx = lines.drop(startIdx + 1).indexOfFirst { it.startsWith("## ") }
        return if (endIdx == -1) {
            lines.drop(startIdx).joinToString("\n")
        } else {
            lines.subList(startIdx, startIdx + 1 + endIdx).joinToString("\n")
        }
    }
}
