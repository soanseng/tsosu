package app.tsosu.data.markdown.todoist

import app.tsosu.domain.recurrence.RecurrenceParser
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TodoistCsvParserTest {

    private val parser = TodoistCsvParser(RecurrenceParser())

    @Test
    fun `empty input returns empty result`() {
        val result = parser.parse("")
        assertEquals(0, result.tasks.size)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `header only returns empty result`() {
        val csv = "TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE"
        val result = parser.parse(csv)
        assertEquals(0, result.tasks.size)
    }

    @Test
    fun `single basic task`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Buy groceries,,4,1,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("Buy groceries", task.title)
        assertEquals("", task.description)
        assertEquals(Priority.NONE, task.priority)
        assertEquals(TaskStatus.TODO, task.status)
        assertNull(task.dueDate)
        assertNull(task.recurrenceRule)
    }

    @Test
    fun `task with description`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Buy groceries,Don't forget milk,4,1,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        assertEquals("Don't forget milk", result.tasks[0].description)
    }

    @Test
    fun `priority mapping`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Urgent task,,1,1,,,,,
            task,High task,,2,1,,,,,
            task,Medium task,,3,1,,,,,
            task,Normal task,,4,1,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(4, result.tasks.size)
        assertEquals(Priority.URGENT, result.tasks[0].priority)
        assertEquals(Priority.HIGH, result.tasks[1].priority)
        assertEquals(Priority.MEDIUM, result.tasks[2].priority)
        assertEquals(Priority.NONE, result.tasks[3].priority)
    }

    @Test
    fun `task with due date`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Buy groceries,,4,1,,,2026-03-28,en,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        val dueDate = result.tasks[0].dueDate
        assertNotNull(dueDate)
        assertEquals(2026, dueDate!!.year)
        assertEquals(3, dueDate.monthNumber)
        assertEquals(28, dueDate.dayOfMonth)
    }

    @Test
    fun `task with recurrence rule`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Weekly review,,2,1,,,every Friday,en,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        assertEquals("RRULE:FREQ=WEEKLY;BYDAY=FR", result.tasks[0].recurrenceRule)
    }

    @Test
    fun `unrecognized recurrence appended to description with warning`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Odd task,Some desc,4,1,,,every other Tuesday,en,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        assertTrue(result.tasks[0].description.contains("every other Tuesday"))
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun `subtask tree from indent`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Parent task,,4,1,,,,,
            task,Child 1,,4,2,,,,,
            task,Child 2,,4,2,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        val parent = result.tasks[0]
        assertEquals("Parent task", parent.title)
        assertEquals(2, parent.subtasks.size)
        assertEquals("Child 1", parent.subtasks[0].title)
        assertEquals("Child 2", parent.subtasks[1].title)
    }

    @Test
    fun `nested subtasks`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Parent,,4,1,,,,,
            task,Child,,4,2,,,,,
            task,Grandchild,,4,3,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        assertEquals(1, result.tasks[0].subtasks.size)
        assertEquals("Child", result.tasks[0].subtasks[0].title)
        assertEquals(1, result.tasks[0].subtasks[0].subtasks.size)
        assertEquals("Grandchild", result.tasks[0].subtasks[0].subtasks[0].title)
    }

    @Test
    fun `section rows are skipped`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            section,Work Section,,4,1,,,,,
            task,Work task,,4,1,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        assertEquals("Work task", result.tasks[0].title)
    }

    @Test
    fun `quoted fields with commas`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,"Buy eggs, milk, bread",,4,1,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        assertEquals("Buy eggs, milk, bread", result.tasks[0].title)
    }

    @Test
    fun `quoted fields with escaped quotes`() {
        val csv = "TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE\n" +
            "task,\"Read \"\"War and Peace\"\"\",\"A \"\"classic\"\" book\",4,1,,,,,"

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        assertEquals("Read \"War and Peace\"", result.tasks[0].title)
        assertEquals("A \"classic\" book", result.tasks[0].description)
    }

    @Test
    fun `multiple top-level tasks`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Task 1,,4,1,,,,,
            task,Task 2,,3,1,,,2026-04-01,en,
            task,Task 3,,1,1,,,every day,en,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(3, result.tasks.size)
        assertEquals("Task 1", result.tasks[0].title)
        assertEquals(Priority.NONE, result.tasks[0].priority)
        assertEquals("Task 2", result.tasks[1].title)
        assertEquals(Priority.MEDIUM, result.tasks[1].priority)
        assertNotNull(result.tasks[1].dueDate)
        assertEquals("Task 3", result.tasks[2].title)
        assertEquals(Priority.URGENT, result.tasks[2].priority)
        assertEquals("RRULE:FREQ=DAILY", result.tasks[2].recurrenceRule)
    }

    @Test
    fun `tasks get unique UUIDs`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Task 1,,4,1,,,,,
            task,Task 2,,4,1,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(2, result.tasks.size)
        assertTrue(result.tasks[0].id != result.tasks[1].id)
        assertTrue(result.tasks[0].id.isNotBlank())
    }

    @Test
    fun `positions are sequential`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Task 1,,4,1,,,,,
            task,Task 2,,4,1,,,,,
            task,Task 3,,4,1,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(0.0, result.tasks[0].position)
        assertEquals(1.0, result.tasks[1].position)
        assertEquals(2.0, result.tasks[2].position)
    }

    @Test
    fun `mixed parents and children with correct positioning`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Parent 1,,4,1,,,,,
            task,Child 1a,,4,2,,,,,
            task,Parent 2,,4,1,,,,,
            task,Child 2a,,4,2,,,,,
            task,Child 2b,,4,2,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(2, result.tasks.size)
        assertEquals("Parent 1", result.tasks[0].title)
        assertEquals(1, result.tasks[0].subtasks.size)
        assertEquals("Child 1a", result.tasks[0].subtasks[0].title)
        assertEquals("Parent 2", result.tasks[1].title)
        assertEquals(2, result.tasks[1].subtasks.size)
    }

    @Test
    fun `description with existing text and unrecognized recurrence merges`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Task,Existing description,4,1,,,every fortnight-ish,en,
        """.trimIndent()

        val result = parser.parse(csv)

        val desc = result.tasks[0].description
        assertTrue(desc.contains("Existing description"))
        assertTrue(desc.contains("every fortnight-ish"))
    }

    @Test
    fun `every other month is recognized as recurrence rule`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Task,Existing description,4,1,,,every other month,en,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals("RRULE:FREQ=MONTHLY;INTERVAL=2", result.tasks[0].recurrenceRule)
    }

    @Test
    fun `combined date and recurrence keeps both`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Water plants,,4,1,,,every day starting 2026-03-28,en,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals(LocalDateTime(2026, 3, 28, 0, 0), task.dueDate)
        assertEquals("RRULE:FREQ=DAILY", task.recurrenceRule)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `date with time is parsed and time is zeroed`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Morning task,,4,1,,,2026-03-28 at 09:00,en,
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        val dueDate = result.tasks[0].dueDate
        assertNotNull(dueDate)
        assertEquals(2026, dueDate!!.year)
        assertEquals(3, dueDate.monthNumber)
        assertEquals(28, dueDate.dayOfMonth)
        assertEquals(0, dueDate.hour)
        assertEquals(0, dueDate.minute)
    }

    @Test
    fun `orphan subtask with no parent is promoted to top level`() {
        val csv = """
            TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
            task,Orphan child,,4,2,,,,,
            task,Another orphan,,4,3,,,,,
        """.trimIndent()

        val result = parser.parse(csv)

        // Orphans should be promoted to top-level
        assertTrue(result.tasks.isNotEmpty())
        assertEquals("Orphan child", result.tasks[0].title)
    }

    @Test
    fun `CSV with missing required columns returns warning`() {
        val csv = """
            FOO,BAR,BAZ
            something,else,here
        """.trimIndent()

        val result = parser.parse(csv)

        assertEquals(0, result.tasks.size)
        assertTrue(result.warnings.any { it.contains("missing") }, "Expected a warning about missing columns")
    }

    @Test
    fun `CSV with trailing newline parses correctly`() {
        val csv = "TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE\n" +
            "task,Task 1,,4,1,,,,,\n"

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        assertEquals("Task 1", result.tasks[0].title)
    }

    @Test
    fun `CSV with UTF-8 BOM parses correctly`() {
        val csv = "\uFEFFTYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE\n" +
            "task,BOM task,,4,1,,,,,"

        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        assertEquals("BOM task", result.tasks[0].title)
    }

    @Test
    fun `CSV with CRLF line endings parses correctly`() {
        val csv = "TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE\r\n" +
            "task,Task 1,,4,1,,,,,\r\n" +
            "task,Task 2,,4,1,,,,,\r\n"

        val result = parser.parse(csv)

        assertEquals(2, result.tasks.size)
        assertEquals("Task 1", result.tasks[0].title)
        assertEquals("Task 2", result.tasks[1].title)
    }
}
