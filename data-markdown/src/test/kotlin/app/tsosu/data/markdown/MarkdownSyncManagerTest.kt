package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarkdownSyncManagerTest {

    private val fileAccess = mockk<MarkdownFileAccess>(relaxed = true)
    private val taskSerializer = MarkdownTaskSerializer()
    private val taskParser = MarkdownTaskParser()
    private val habitSerializer = MarkdownHabitSerializer()
    private val habitParser = MarkdownHabitParser()

    private val manager = MarkdownSyncManager(
        fileAccess = fileAccess,
        taskSerializer = taskSerializer,
        taskParser = taskParser,
        habitSerializer = habitSerializer,
        habitParser = habitParser,
    )

    private val fixedCreatedAt = Instant.parse("2026-03-20T10:00:00Z")
    private val fixedUpdatedAt = Instant.parse("2026-03-22T14:30:00Z")

    private fun task(
        id: String = "task-1",
        title: String = "Buy groceries",
        done: Boolean = false,
    ) = Task(
        id = id,
        title = title,
        status = if (done) TaskStatus.DONE else TaskStatus.TODO,
        createdAt = fixedCreatedAt,
        updatedAt = fixedUpdatedAt,
    )

    private fun habit(
        id: String = "h1",
        title: String = "Exercise",
    ) = Habit(
        id = id,
        title = title,
        frequency = HabitFrequency.DAILY,
        targetDaysPerWeek = 7,
        energyLevel = EnergyLevel.MEDIUM,
        createdAt = fixedCreatedAt,
    )

    @Test
    fun `exportTasks writes serialized markdown containing task title and id`() = runTest {
        val tasks = listOf(task(id = "abc-123", title = "Write tests"))

        manager.exportTasks(tasks, emptyMap())

        coVerify {
            fileAccess.writeTasksFile(withArg { content ->
                assertTrue(content.contains("Write tests"), "Should contain task title")
                assertTrue(content.contains("<!-- id:abc-123 -->"), "Should contain task id")
            })
        }
    }

    @Test
    fun `importTasks reads and parses markdown returning correct tasks`() = runTest {
        val markdown = buildString {
            appendLine("---")
            appendLine("tsosu: v1")
            appendLine("updated: 2026-03-23T10:00:00")
            appendLine("---")
            appendLine()
            appendLine("## Inbox")
            appendLine("- [ ] Buy groceries \uD83D\uDE10medium <!-- id:task-1 -->")
        }
        coEvery { fileAccess.readTasksFile() } returns markdown

        val result = manager.importTasks()

        assertEquals(1, result.tasks.size)
        assertEquals("task-1", result.tasks[0].id)
        assertEquals("Buy groceries", result.tasks[0].title)
    }

    @Test
    fun `importTasks returns empty when file does not exist`() = runTest {
        coEvery { fileAccess.readTasksFile() } returns null

        val result = manager.importTasks()

        assertTrue(result.tasks.isEmpty(), "Should return empty task list")
        assertTrue(result.projectSections.isEmpty(), "Should return empty project sections")
    }

    @Test
    fun `exportHabits writes serialized habits markdown`() = runTest {
        val habits = listOf(habit(id = "h1", title = "Meditate"))
        val completions = listOf(
            HabitCompletion("h1", LocalDate.parse("2026-03-23"), Instant.parse("2026-03-23T08:00:00Z")),
        )

        manager.exportHabits(habits, completions)

        coVerify {
            fileAccess.writeHabitsFile(withArg { content ->
                assertTrue(content.contains("Meditate"), "Should contain habit title")
                assertTrue(content.contains("<!-- id:h1 -->"), "Should contain habit id")
                assertTrue(content.contains("2026-03-23"), "Should contain completion date")
            })
        }
    }

    @Test
    fun `importHabits reads and parses habits returning correct data`() = runTest {
        val markdown = buildString {
            appendLine("---")
            appendLine("tsosu: v1")
            appendLine("updated: 2026-03-23T10:00:00")
            appendLine("---")
            appendLine()
            appendLine("## Daily")
            appendLine()
            appendLine("- [ ] Meditate \uD83D\uDD01daily \u26A1medium <!-- id:h1 -->")
            appendLine("  - \u2705 2026-03-22")
        }
        coEvery { fileAccess.readHabitsFile() } returns markdown

        val result = manager.importHabits()

        assertEquals(1, result.habits.size)
        assertEquals("h1", result.habits[0].id)
        assertEquals("Meditate", result.habits[0].title)
        assertEquals(1, result.completions.size)
        assertEquals("h1", result.completions[0].habitId)
        assertEquals(LocalDate.parse("2026-03-22"), result.completions[0].date)
    }

    @Test
    fun `importHabits returns empty when file does not exist`() = runTest {
        coEvery { fileAccess.readHabitsFile() } returns null

        val result = manager.importHabits()

        assertTrue(result.habits.isEmpty(), "Should return empty habits list")
        assertTrue(result.completions.isEmpty(), "Should return empty completions list")
    }
}
