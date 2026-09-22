package app.tsosu.data.markdown

import app.tsosu.data.markdown.dailynote.DailyNoteWriter
import app.tsosu.data.markdown.index.TaskIndexGenerator
import app.tsosu.data.markdown.tasknote.TaskNoteParser
import app.tsosu.data.markdown.tasknote.TaskNoteSerializer
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.model.TaskStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
    private val taskNoteSerializer = TaskNoteSerializer()
    private val taskNoteParser = TaskNoteParser()
    private val dailyNoteWriter = DailyNoteWriter()
    private val taskIndexGenerator = TaskIndexGenerator()

    private val manager = MarkdownSyncManager(
        fileAccess = fileAccess,
        taskSerializer = taskSerializer,
        taskParser = taskParser,
        taskNoteSerializer = taskNoteSerializer,
        taskNoteParser = taskNoteParser,
        dailyNoteWriter = dailyNoteWriter,
        taskIndexGenerator = taskIndexGenerator,
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

    private fun recurringTask(
        id: String = "r1",
        title: String = "Exercise",
    ) = task(id = id, title = title).copy(recurrenceRule = "RRULE:FREQ=DAILY")

    @Test
    fun `exportTasks writes index file with task title and id`() = runTest {
        val tasks = listOf(task(id = "abc-123", title = "Write tests"))

        manager.exportTasks(tasks, emptyMap())

        coVerify {
            fileAccess.ensureFolder("tasks")
            fileAccess.writeTasksFile(withArg { content ->
                assertTrue(content.contains("Write tests"), "Should contain task title")
                assertTrue(content.contains("\uD83C\uDD94 abc-123"), "Should contain task id")
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
    fun `exportTasks removes notes of tasks that no longer exist`() = runTest {
        val tasks = listOf(task(id = "keep1234", title = "Keep me"))
        coEvery { fileAccess.listFolder("tasks") } returns listOf(
            "keep-me-keep1234.md",
            "gone-task-abc99999.md",
        )

        manager.exportTasks(tasks, emptyMap(), removedTaskIds = setOf("abc99999"))

        coVerify(exactly = 1) { fileAccess.deleteFileInFolder("tasks", "gone-task-abc99999.md") }
        coVerify(exactly = 0) { fileAccess.deleteFileInFolder("tasks", "keep-me-keep1234.md") }
    }

    @Test
    fun `exportTasks deletes nothing when no task was removed`() = runTest {
        val tasks = listOf(task(id = "keep1234", title = "Keep me"))
        coEvery { fileAccess.listFolder("tasks") } returns listOf("keep-me-keep1234.md")

        manager.exportTasks(tasks, emptyMap())

        coVerify(exactly = 0) { fileAccess.deleteFileInFolder(any(), any()) }
    }

    @Test
    fun `removeTaskNote deletes only the note belonging to that task`() = runTest {
        coEvery { fileAccess.listFolder("tasks") } returns listOf(
            "meal-plan-8b0f0fd2.md",
            "meal-plan-ffffffff.md",
            "legacy-no-id.md",
        )

        manager.removeTaskNote("8b0f0fd2-3612-4da4-b803-3fc9301e4024")

        coVerify(exactly = 1) { fileAccess.deleteFileInFolder("tasks", "meal-plan-8b0f0fd2.md") }
    }

    @Test
    fun `a routine slot survives the vault round trip`() = runTest {
        // No description: the note exists only because of the Tsosu-only
        // fields. Before that rule, the inline tasks.md line (which cannot
        // carry them) was the task's only vault home and the slot was lost.
        val habit = recurringTask(id = "h1", title = "Meditate").copy(
            routineTime = RoutineTime.MORNING,
            tinyVersion = "Sit for one minute",
        )
        val note = slot<String>()
        coEvery { fileAccess.readFileInFolder("tasks", any()) } returns null
        coEvery { fileAccess.writeFileInFolder("tasks", any(), capture(note)) } returns Unit

        manager.exportTasks(listOf(habit), emptyMap())

        coVerify { fileAccess.writeFileInFolder("tasks", match { it.startsWith("meditate-") }, any()) }
        assertTrue(
            note.captured.contains("routine: morning"),
            "Note must carry the routine slot, got: ${note.captured}",
        )

        // The note is what import reads back.
        coEvery { fileAccess.listFolder("tasks") } returns listOf("meditate-h1.md")
        coEvery { fileAccess.readFileInFolder("tasks", "meditate-h1.md") } returns note.captured
        coEvery { fileAccess.readTasksFile() } returns null

        val imported = manager.importTasks()

        assertEquals(RoutineTime.MORNING, imported.tasks.single().routineTime)
        assertEquals("Sit for one minute", imported.tasks.single().tinyVersion)
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
    fun `exportDailyNote writes recurring checklist in daily folder`() = runTest {
        val date = LocalDate.parse("2026-03-23")
        val recurring = listOf(recurringTask(id = "r1", title = "Exercise"))
        val completedIds = setOf("r1")

        manager.exportDailyNote(date, recurring, completedIds)

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
        val recurring = listOf(recurringTask(id = "r1", title = "Exercise"))
        val completedIds = emptySet<String>()

        manager.exportDailyNote(date, recurring, completedIds)

        coVerify {
            fileAccess.writeFileInFolder("daily", "2026-03-23.md", withArg { content ->
                assertTrue(content.contains("[ ] Exercise"), "Should mark uncompleted habit")
            })
        }
    }

    @Test
    fun `obsidian done plus spawned pair folds into one recurring task`() = runTest {
        val index = """
            ---
            tsosu: v1
            generated: true
            ---

            ## Inbox
            - [x] Meditate 🔁 every day 🆔 med-1 ➕ 2026-09-01 📅 2026-09-02 ✅ 2026-09-02
            - [ ] Meditate 🔁 every day 🆔 med-1 ➕ 2026-09-01 📅 2026-09-03
        """.trimIndent()
        coEvery { fileAccess.listFolder("tasks") } returns emptyList()
        coEvery { fileAccess.readTasksFile() } returns index

        val result = manager.importTasks()

        assertEquals(1, result.tasks.size)
        val merged = result.tasks[0]
        assertEquals("med-1", merged.id)
        assertEquals(TaskStatus.TODO, merged.status)
        assertEquals(kotlinx.datetime.LocalDate(2026, 9, 3), merged.dueDate?.date)
        assertEquals(listOf(kotlinx.datetime.LocalDate(2026, 9, 2)), merged.completions)
        assertEquals("RRULE:FREQ=DAILY", merged.recurrenceRule)
    }

    @Test
    fun `single done non-recurring line still imports as done`() = runTest {
        val index = """
            ## Inbox
            - [x] One-off task 🆔 one-1 ✅ 2026-09-02
        """.trimIndent()
        coEvery { fileAccess.listFolder("tasks") } returns emptyList()
        coEvery { fileAccess.readTasksFile() } returns index

        val result = manager.importTasks()

        assertEquals(1, result.tasks.size)
        assertEquals(TaskStatus.DONE, result.tasks[0].status)
        assertTrue(result.tasks[0].completions.isEmpty(), "Plain task: no completions fold")
    }
}
