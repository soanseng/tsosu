package app.tsosu.data.markdown.index

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

class TaskIndexGeneratorTest {

    private val generator = TaskIndexGenerator()

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

    // --- Frontmatter ---

    @Test
    fun `frontmatter includes generated true`() {
        val result = generator.generate(emptyList(), emptyMap(), emptyMap())

        assertTrue(result.contains("generated: true"), "Should have generated: true in frontmatter")
        assertTrue(result.contains("tsosu: v1"), "Should have tsosu version")
        assertTrue(result.contains("updated:"), "Should have updated timestamp")
    }

    // --- Wikilinks ---

    @Test
    fun `task with noteFilename gets wikilink before id comment`() {
        val tasks = listOf(task(id = "a1b2", title = "Buy groceries"))
        val noteFilenames = mapOf("a1b2" to "buy-groceries")

        val result = generator.generate(tasks, emptyMap(), noteFilenames)
        val taskLine = result.lines().first { it.startsWith("- [") }

        assertTrue(
            taskLine.contains("[[tasks/buy-groceries]]"),
            "Should contain wikilink, got: $taskLine",
        )
        assertTrue(
            taskLine.contains("[[tasks/buy-groceries]] <!-- id:a1b2 -->"),
            "Wikilink should appear before id comment, got: $taskLine",
        )
    }

    @Test
    fun `task without noteFilename has no wikilink`() {
        val tasks = listOf(task(id = "x1y2", title = "Quick task"))

        val result = generator.generate(tasks, emptyMap(), emptyMap())
        val taskLine = result.lines().first { it.startsWith("- [") }

        assertFalse(taskLine.contains("[["), "Should not contain wikilink")
        assertTrue(taskLine.contains("<!-- id:x1y2 -->"), "Should still have id comment")
    }

    // --- Grouping ---

    @Test
    fun `tasks grouped by project with Inbox first`() {
        val tasks = listOf(
            task(id = "inbox-1", title = "Inbox task", projectId = null, position = 1.0),
            task(id = "work-1", title = "Work task", projectId = "proj-1", position = 1.0),
            task(id = "home-1", title = "Home task", projectId = "proj-2", position = 1.0),
        )
        val projectNames = mapOf("proj-1" to "Work", "proj-2" to "Home")

        val result = generator.generate(tasks, projectNames, emptyMap())

        assertTrue(result.contains("## Inbox"), "Should have Inbox section")
        assertTrue(result.contains("## Work"), "Should have Work section")
        assertTrue(result.contains("## Home"), "Should have Home section")

        // Inbox must come before other sections
        val inboxIdx = result.indexOf("## Inbox")
        val workIdx = result.indexOf("## Work")
        val homeIdx = result.indexOf("## Home")
        assertTrue(inboxIdx < workIdx, "Inbox before Work")
        assertTrue(inboxIdx < homeIdx, "Inbox before Home")

        // Verify tasks are in their correct sections
        val inboxSection = extractSection(result, "Inbox")
        assertTrue(inboxSection.contains("Inbox task"), "Inbox section has inbox task")

        val workSection = extractSection(result, "Work")
        assertTrue(workSection.contains("Work task"), "Work section has work task")

        val homeSection = extractSection(result, "Home")
        assertTrue(homeSection.contains("Home task"), "Home section has home task")
    }

    @Test
    fun `project sections sorted alphabetically`() {
        val tasks = listOf(
            task(id = "z-1", title = "Zebra task", projectId = "proj-z", position = 1.0),
            task(id = "a-1", title = "Apple task", projectId = "proj-a", position = 1.0),
        )
        val projectNames = mapOf("proj-z" to "Zebra", "proj-a" to "Apple")

        val result = generator.generate(tasks, projectNames, emptyMap())

        val appleIdx = result.indexOf("## Apple")
        val zebraIdx = result.indexOf("## Zebra")
        assertTrue(appleIdx < zebraIdx, "Apple section before Zebra section")
    }

    @Test
    fun `tasks sorted by position within sections`() {
        val tasks = listOf(
            task(id = "id-3", title = "Third", position = 30.0),
            task(id = "id-1", title = "First", position = 10.0),
            task(id = "id-2", title = "Second", position = 20.0),
        )

        val result = generator.generate(tasks, emptyMap(), emptyMap())
        val taskLines = result.lines().filter { it.startsWith("- [") }

        assertEquals(3, taskLines.size, "Should have 3 task lines")
        assertTrue(taskLines[0].contains("First"), "First by position")
        assertTrue(taskLines[1].contains("Second"), "Second by position")
        assertTrue(taskLines[2].contains("Third"), "Third by position")
    }

    // --- Extended statuses ---

    @Test
    fun `in-progress task uses forward slash checkbox`() {
        val result = generator.generate(
            listOf(task(status = TaskStatus.IN_PROGRESS)),
            emptyMap(),
            emptyMap(),
        )
        assertTrue(result.contains("- [/] Buy groceries"), "Should have [/] checkbox")
    }

    @Test
    fun `on-hold task uses exclamation checkbox`() {
        val result = generator.generate(
            listOf(task(status = TaskStatus.ON_HOLD)),
            emptyMap(),
            emptyMap(),
        )
        assertTrue(result.contains("- [!] Buy groceries"), "Should have [!] checkbox")
    }

    @Test
    fun `planned task uses greater-than checkbox`() {
        val result = generator.generate(
            listOf(task(status = TaskStatus.PLANNED)),
            emptyMap(),
            emptyMap(),
        )
        assertTrue(result.contains("- [>] Buy groceries"), "Should have [>] checkbox")
    }

    @Test
    fun `done task uses x checkbox with completion date`() {
        val result = generator.generate(
            listOf(task(status = TaskStatus.DONE)),
            emptyMap(),
            emptyMap(),
        )
        assertTrue(result.contains("- [x] Buy groceries"), "Should have [x] checkbox")
        assertTrue(result.contains("\u2705 2026-03-22"), "Should have completion date from updatedAt")
    }

    @Test
    fun `cancelled task uses dash checkbox`() {
        val cancelled = LocalDateTime.parse("2026-03-21T15:00:00")
        val result = generator.generate(
            listOf(task(status = TaskStatus.CANCELLED, cancelledDate = cancelled)),
            emptyMap(),
            emptyMap(),
        )
        assertTrue(result.contains("- [-] Buy groceries"), "Should have [-] checkbox")
        assertTrue(result.contains("\u274C 2026-03-21"), "Should have cancelled date")
    }

    // --- Date fields and priority ---

    @Test
    fun `task with all metadata includes every field`() {
        val due = LocalDateTime.parse("2026-04-01T08:00:00")
        val scheduled = LocalDateTime.parse("2026-03-28T09:00:00")
        val start = LocalDateTime.parse("2026-03-24T08:00:00")
        val tasks = listOf(
            task(
                id = "full-1",
                title = "Deep work session",
                status = TaskStatus.TODO,
                dueDate = due,
                scheduledDate = scheduled,
                startDate = start,
                priority = Priority.URGENT,
                energyLevel = EnergyLevel.HIGH,
                estimatedMinutes = 30,
                reminderTime = LocalTime(14, 30),
                recurrenceRule = "every week",
            ),
        )
        val noteFilenames = mapOf("full-1" to "deep-work-session")

        val result = generator.generate(tasks, emptyMap(), noteFilenames)
        val taskLine = result.lines().first { it.startsWith("- [") }

        assertTrue(taskLine.contains("Deep work session"), "Title present")
        assertTrue(taskLine.contains("\uD83D\uDCC5 2026-04-01"), "Due date present")
        assertTrue(taskLine.contains("\u23F3 2026-03-28"), "Scheduled date present")
        assertTrue(taskLine.contains("\uD83D\uDEEB 2026-03-24"), "Start date present")
        assertTrue(taskLine.contains("\u2795 2026-03-20"), "Created date present")
        assertTrue(taskLine.contains("\u23F0 14:30"), "Reminder time present")
        assertTrue(taskLine.contains("\uD83D\uDD01 every week"), "Recurrence rule present")
        assertTrue(taskLine.contains("\u26A1high"), "Energy level present")
        assertTrue(taskLine.contains("\uD83C\uDF45 30m"), "Estimate present")
        assertTrue(taskLine.contains("\u23EB"), "Priority present")
        assertTrue(taskLine.contains("[[tasks/deep-work-session]]"), "Wikilink present")
        assertTrue(taskLine.contains("<!-- id:full-1 -->"), "ID comment present")
    }

    @Test
    fun `priority NONE is omitted`() {
        val result = generator.generate(
            listOf(task(priority = Priority.NONE)),
            emptyMap(),
            emptyMap(),
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertFalse(taskLine.contains("\u23EB"), "No URGENT marker")
        assertFalse(taskLine.contains("\uD83D\uDD3A"), "No HIGH marker")
        assertFalse(taskLine.contains("\uD83D\uDD3C"), "No MEDIUM marker")
        assertFalse(taskLine.contains("\uD83D\uDD3D"), "No LOW marker")
    }

    @Test
    fun `all priority levels use correct emoji`() {
        for ((priority, expectedMarker) in listOf(
            Priority.LOW to "\uD83D\uDD3D",
            Priority.MEDIUM to "\uD83D\uDD3C",
            Priority.HIGH to "\uD83D\uDD3A",
            Priority.URGENT to "\u23EB",
        )) {
            val result = generator.generate(
                listOf(task(priority = priority)),
                emptyMap(),
                emptyMap(),
            )
            val taskLine = result.lines().first { it.startsWith("- [") }
            assertTrue(
                taskLine.contains(expectedMarker),
                "Priority $priority should produce marker '$expectedMarker', got: $taskLine",
            )
        }
    }

    @Test
    fun `energy level always emitted`() {
        val result = generator.generate(
            listOf(task(energyLevel = EnergyLevel.MEDIUM)),
            emptyMap(),
            emptyMap(),
        )
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(taskLine.contains("\uD83D\uDE10medium"), "MEDIUM energy should be emitted")
    }

    // --- Mixed wikilink and inline tasks ---

    @Test
    fun `mixed tasks with and without wikilinks in same section`() {
        val tasks = listOf(
            task(id = "with-note", title = "Has a note", position = 1.0),
            task(id = "no-note", title = "Inline only", position = 2.0),
        )
        val noteFilenames = mapOf("with-note" to "has-a-note")

        val result = generator.generate(tasks, emptyMap(), noteFilenames)
        val taskLines = result.lines().filter { it.startsWith("- [") }

        assertEquals(2, taskLines.size)
        assertTrue(taskLines[0].contains("[[tasks/has-a-note]]"), "First task has wikilink")
        assertFalse(taskLines[1].contains("[["), "Second task has no wikilink")
    }

    // --- Empty list ---

    @Test
    fun `empty list produces frontmatter and Inbox section`() {
        val result = generator.generate(emptyList(), emptyMap(), emptyMap())

        assertTrue(result.startsWith("---\n"), "Should start with frontmatter")
        assertTrue(result.contains("tsosu: v1"), "Should contain version")
        assertTrue(result.contains("generated: true"), "Should contain generated flag")
        assertTrue(result.contains("## Inbox"), "Should contain Inbox section")
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
