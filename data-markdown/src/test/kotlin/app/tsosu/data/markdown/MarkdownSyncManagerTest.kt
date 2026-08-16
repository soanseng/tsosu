package app.tsosu.data.markdown

import app.tsosu.data.markdown.dailynote.DailyNoteWriter
import app.tsosu.data.markdown.habitnote.HabitNoteParser
import app.tsosu.data.markdown.habitnote.HabitNoteSerializer
import app.tsosu.data.markdown.index.HabitIndexGenerator
import app.tsosu.data.markdown.index.TaskIndexGenerator
import app.tsosu.data.markdown.tasknote.TaskNoteParser
import app.tsosu.data.markdown.tasknote.TaskNoteSerializer
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.model.RoutineTime
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
    private val taskNoteSerializer = TaskNoteSerializer()
    private val taskNoteParser = TaskNoteParser()
    private val habitNoteSerializer = HabitNoteSerializer()
    private val habitNoteParser = HabitNoteParser()
    private val dailyNoteWriter = DailyNoteWriter()
    private val taskIndexGenerator = TaskIndexGenerator()
    private val habitIndexGenerator = HabitIndexGenerator()

    private val manager = MarkdownSyncManager(
        fileAccess = fileAccess,
        taskSerializer = taskSerializer,
        taskParser = taskParser,
        habitSerializer = habitSerializer,
        habitParser = habitParser,
        taskNoteSerializer = taskNoteSerializer,
        taskNoteParser = taskNoteParser,
        habitNoteSerializer = habitNoteSerializer,
        habitNoteParser = habitNoteParser,
        dailyNoteWriter = dailyNoteWriter,
        taskIndexGenerator = taskIndexGenerator,
        habitIndexGenerator = habitIndexGenerator,
    )

    private val fixedCreatedAt = Instant.parse("2026-03-20T10:00:00Z")
    private val fixedUpdatedAt = Instant.parse("2026-03-22T14:30:00Z")

    private fun task(
        id: String = "task-1",
        title: String = "Buy groceries",
        done: Boolean = false,
        description: String = "",
        subtasks: List<Task> = emptyList(),
    ) = Task(
        id = id,
        title = title,
        description = description,
        status = if (done) TaskStatus.DONE else TaskStatus.TODO,
        subtasks = subtasks,
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
    fun `exportTasks writes index file with task title and id`() = runTest {
        val tasks = listOf(task(id = "abc-123", title = "Write tests"))

        manager.exportTasks(tasks, emptyMap())

        coVerify {
            fileAccess.ensureFolder("tasks")
            fileAccess.writeTasksFile(withArg { content ->
                assertTrue(content.contains("Write tests"), "Should contain task title")
                assertTrue(content.contains("<!-- id:abc-123 -->"), "Should contain task id")
            })
        }
    }

    @Test
    fun `importTasks dedupes same task id across legacy and new filenames`() = runTest {
        val noteContent = buildString {
            appendLine("---")
            appendLine("id: t1")
            appendLine("status: todo")
            appendLine("energy: medium")
            appendLine("created: 2026-03-20")
            appendLine("---")
            appendLine()
            appendLine("# From note")
            appendLine()
            appendLine("Detailed description")
        }
        // Same id present in a legacy slug file and the new id-suffixed file
        coEvery { fileAccess.listFolder("tasks") } returns listOf("from-note.md", "from-note-t1.md")
        coEvery { fileAccess.readFileInFolder("tasks", any()) } returns noteContent

        val result = manager.importTasks()

        assertEquals(1, result.tasks.size, "Duplicate id files must collapse to one task")
        assertEquals("t1", result.tasks[0].id)
        assertEquals("From note", result.tasks[0].title)
    }

    @Test
    fun `exportTasks skips writing note file when content unchanged`() = runTest {
        val tasks = listOf(task(id = "t1", title = "Complex task", description = "Some details"))
        coEvery {
            fileAccess.readFileInFolder("tasks", "complex-task-t1.md")
        } answers { taskNoteSerializer.serialize(tasks[0]) }

        manager.exportTasks(tasks, emptyMap())

        coVerify(exactly = 0) {
            fileAccess.writeFileInFolder("tasks", "complex-task-t1.md", any())
        }
    }

    @Test
    fun `exportTasks skips rewriting index when unchanged`() = runTest {
        val tasks = listOf(task(id = "t1", title = "Complex task", description = "Some details"))
        coEvery { fileAccess.readTasksFile() } answers {
            taskIndexGenerator.generate(tasks, emptyMap(), mapOf("t1" to "complex-task-t1"))
        }

        manager.exportTasks(tasks, emptyMap())

        coVerify(exactly = 0) {
            fileAccess.writeTasksFile(any())
        }
    }

    @Test
    fun `exportTasks creates note files for tasks with description`() = runTest {
        val tasks = listOf(
            task(id = "t1", title = "Complex task", description = "Some details"),
        )

        manager.exportTasks(tasks, emptyMap())

        coVerify {
            fileAccess.ensureFolder("tasks")
            fileAccess.writeFileInFolder("tasks", "complex-task-t1.md", withArg { content ->
                assertTrue(content.contains("# Complex task"), "Note should contain H1 title")
                assertTrue(content.contains("Some details"), "Note should contain description")
                assertTrue(content.contains("id: t1"), "Note should contain id in frontmatter")
            })
        }
    }

    @Test
    fun `exportTasks does not create note files for simple tasks`() = runTest {
        val tasks = listOf(task(id = "t1", title = "Simple task", description = ""))

        manager.exportTasks(tasks, emptyMap())

        coVerify(exactly = 0) {
            fileAccess.writeFileInFolder("tasks", any(), any())
        }
    }

    @Test
    fun `exportTasks index includes wikilink for tasks with notes`() = runTest {
        val tasks = listOf(
            task(id = "t1", title = "Complex task", description = "Details here"),
        )

        manager.exportTasks(tasks, emptyMap())

        coVerify {
            fileAccess.writeTasksFile(withArg { content ->
                assertTrue(
                    content.contains("[[tasks/complex-task-t1]]"),
                    "Index should have wikilink for noted task",
                )
            })
        }
    }

    @Test
    fun `importTasks reads from note files and index, notes take priority`() = runTest {
        // Task note file provides the task with description
        val noteContent = buildString {
            appendLine("---")
            appendLine("id: t1")
            appendLine("status: todo")
            appendLine("energy: medium")
            appendLine("created: 2026-03-20")
            appendLine("---")
            appendLine()
            appendLine("# From note")
            appendLine()
            appendLine("Detailed description")
        }
        coEvery { fileAccess.listFolder("tasks") } returns listOf("from-note.md")
        coEvery { fileAccess.readFileInFolder("tasks", "from-note.md") } returns noteContent

        // Index also has t1 plus an inline-only task t2
        val indexContent = buildString {
            appendLine("---")
            appendLine("tsosu: v1")
            appendLine("updated: 2026-03-23T10:00:00")
            appendLine("generated: true")
            appendLine("---")
            appendLine()
            appendLine("## Inbox")
            appendLine("- [ ] From note \uD83D\uDE10medium <!-- id:t1 -->")
            appendLine("- [ ] Inline only \uD83D\uDE10medium <!-- id:t2 -->")
        }
        coEvery { fileAccess.readTasksFile() } returns indexContent

        val result = manager.importTasks()

        assertEquals(2, result.tasks.size)
        val noteTask = result.tasks.find { it.id == "t1" }!!
        assertEquals("From note", noteTask.title)
        assertEquals("Detailed description", noteTask.description)
        val inlineTask = result.tasks.find { it.id == "t2" }!!
        assertEquals("Inline only", inlineTask.title)
    }

    @Test
    fun `importTasks returns empty when no files exist`() = runTest {
        coEvery { fileAccess.listFolder("tasks") } returns emptyList()
        coEvery { fileAccess.readTasksFile() } returns null

        val result = manager.importTasks()

        assertTrue(result.tasks.isEmpty(), "Should return empty task list")
        assertTrue(result.projectSections.isEmpty(), "Should return empty project sections")
    }

    @Test
    fun `importTasks skips malformed note files`() = runTest {
        coEvery { fileAccess.listFolder("tasks") } returns listOf("bad.md")
        coEvery { fileAccess.readFileInFolder("tasks", "bad.md") } returns "not valid yaml frontmatter"
        coEvery { fileAccess.readTasksFile() } returns null

        val result = manager.importTasks()

        assertTrue(result.tasks.isEmpty(), "Should skip malformed files gracefully")
    }

    @Test
    fun `exportHabits writes note files and index`() = runTest {
        val habits = listOf(habit(id = "h1", title = "Meditate"))
        val completions = listOf(
            HabitCompletion("h1", LocalDate.parse("2026-03-23"), Instant.parse("2026-03-23T08:00:00Z")),
        )

        manager.exportHabits(habits, completions)

        coVerify {
            fileAccess.ensureFolder("habits")
            fileAccess.writeFileInFolder("habits", "meditate-h1.md", withArg { content ->
                assertTrue(content.contains("# Meditate"), "Note should contain title")
                assertTrue(content.contains("id: h1"), "Note should contain id")
                assertTrue(content.contains("2026-03-23"), "Note should contain completion date")
            })
            fileAccess.writeHabitsFile(withArg { content ->
                assertTrue(content.contains("Meditate"), "Index should contain habit title")
                assertTrue(content.contains("<!-- id:h1 -->"), "Index should contain habit id")
            })
        }
    }

    @Test
    fun `importHabits reads from note files`() = runTest {
        val noteContent = buildString {
            appendLine("---")
            appendLine("id: h1")
            appendLine("frequency: daily")
            appendLine("energy: medium")
            appendLine("color: \"#4CAF50\"")
            appendLine("archived: false")
            appendLine("created: 2026-03-20")
            appendLine("---")
            appendLine()
            appendLine("# Meditate")
            appendLine()
            appendLine("## Completions")
            appendLine("- \u2705 2026-03-22")
        }
        coEvery { fileAccess.listFolder("habits") } returns listOf("meditate.md")
        coEvery { fileAccess.readFileInFolder("habits", "meditate.md") } returns noteContent

        val result = manager.importHabits()

        assertEquals(1, result.habits.size)
        assertEquals("h1", result.habits[0].id)
        assertEquals("Meditate", result.habits[0].title)
        assertEquals(1, result.completions.size)
        assertEquals("h1", result.completions[0].habitId)
        assertEquals(LocalDate.parse("2026-03-22"), result.completions[0].date)
    }

    @Test
    fun `importHabits falls back to old habits file when no note files`() = runTest {
        coEvery { fileAccess.listFolder("habits") } returns emptyList()
        val oldContent = buildString {
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
        coEvery { fileAccess.readHabitsFile() } returns oldContent

        val result = manager.importHabits()

        assertEquals(1, result.habits.size)
        assertEquals("h1", result.habits[0].id)
        assertEquals("Meditate", result.habits[0].title)
        assertEquals(1, result.completions.size)
    }

    @Test
    fun `importHabits merges index-only lines when note files exist`() = runTest {
        val noteContent = buildString {
            appendLine("---")
            appendLine("id: h1")
            appendLine("frequency: daily")
            appendLine("energy: medium")
            appendLine("created: 2026-03-20")
            appendLine("---")
            appendLine()
            appendLine("# Meditate")
        }
        val indexContent = buildString {
            appendLine("---")
            appendLine("tsosu: v1")
            appendLine("---")
            appendLine()
            appendLine("## \uD83C\uDF05 Morning")
            appendLine()
            appendLine("- Meditate \u26A1medium <!-- id:h1 -->")
            appendLine("- Water plants \u26A1low <!-- id:hand-1 -->")
        }
        coEvery { fileAccess.listFolder("habits") } returns listOf("meditate.md")
        coEvery { fileAccess.readFileInFolder("habits", "meditate.md") } returns noteContent
        coEvery { fileAccess.readHabitsFile() } returns indexContent

        val result = manager.importHabits()

        // Note-file habit wins for h1; the hand-added index-only line
        // supplements instead of being wiped by the next push.
        assertEquals(2, result.habits.size)
        assertEquals(setOf("h1", "hand-1"), result.habits.map { it.id }.toSet())
        val h1 = result.habits.first { it.id == "h1" }
        assertEquals("Meditate", h1.title)
        assertEquals("medium", h1.energyLevel.name.lowercase())
    }

    @Test
    fun `index-only line strips wikilink and infers routine from heading`() = runTest {
        val indexContent = buildString {
            appendLine("---")
            appendLine("tsosu: v1")
            appendLine("---")
            appendLine()
            appendLine("## \uD83C\uDF05 Morning")
            appendLine()
            appendLine("- Water plants \u26A1low [[habits/water-plants-x1]] <!-- id:hand-1 -->")
        }
        coEvery { fileAccess.listFolder("habits") } returns emptyList()
        coEvery { fileAccess.readHabitsFile() } returns indexContent

        val result = manager.importHabits()

        assertEquals(1, result.habits.size)
        assertEquals("Water plants", result.habits[0].title)
        assertEquals(RoutineTime.MORNING, result.routineTimeByHabitId["hand-1"])
    }

    @Test
    fun `evening heading maps index-only habit to evening routine`() = runTest {
        val indexContent = buildString {
            appendLine("## \uD83C\uDF19 Evening")
            appendLine()
            appendLine("- Stretch <!-- id:eve-1 -->")
        }
        coEvery { fileAccess.listFolder("habits") } returns emptyList()
        coEvery { fileAccess.readHabitsFile() } returns indexContent

        val result = manager.importHabits()

        assertEquals("Stretch", result.habits[0].title)
        assertEquals(RoutineTime.EVENING, result.routineTimeByHabitId["eve-1"])
    }

    @Test
    fun `importHabits returns empty when no files exist`() = runTest {
        coEvery { fileAccess.listFolder("habits") } returns emptyList()
        coEvery { fileAccess.readHabitsFile() } returns null

        val result = manager.importHabits()

        assertTrue(result.habits.isEmpty(), "Should return empty habits list")
        assertTrue(result.completions.isEmpty(), "Should return empty completions list")
    }

    @Test
    fun `exportDailyNote writes file in daily folder`() = runTest {
        val date = LocalDate.parse("2026-03-23")
        val habits = listOf(habit(id = "h1", title = "Exercise"))
        val completedIds = setOf("h1")

        manager.exportDailyNote(date, habits, completedIds)

        coVerify {
            fileAccess.ensureFolder("daily")
            fileAccess.writeFileInFolder("daily", "2026-03-23.md", withArg { content ->
                assertTrue(content.contains("date: 2026-03-23"), "Should contain date")
                assertTrue(content.contains("[x] Exercise"), "Should mark completed habit")
            })
        }
    }

    @Test
    fun `exportDailyNote marks uncompleted habits with empty checkbox`() = runTest {
        val date = LocalDate.parse("2026-03-23")
        val habits = listOf(habit(id = "h1", title = "Exercise"))
        val completedIds = emptySet<String>()

        manager.exportDailyNote(date, habits, completedIds)

        coVerify {
            fileAccess.writeFileInFolder("daily", "2026-03-23.md", withArg { content ->
                assertTrue(content.contains("[ ] Exercise"), "Should mark uncompleted habit")
            })
        }
    }
}
