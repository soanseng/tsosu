package app.tsosu.data.markdown.ticktick

import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TickTickCsvParserTest {

    private val parser = TickTickCsvParser()

    @Test
    fun `parses basic row with due date and priority`() {
        val csv = """
            Folder Name,List Name,Task Name,Task Content,Is All Day,Start Date,Due Date,Reminder,Priority,Status,Created Time,Completed Time,Order,Task ID,Parent ID,Project ID,Tags
            Work,Personal,Pay rent,Remember the check,TRUE,2026-08-01,2026-08-21 09:30,,High,Normal,2026-07-01,,,,,,finance
        """.trimIndent()
        val result = parser.parse(csv)

        assertEquals(1, result.tasks.size)
        val t = result.tasks[0]
        assertEquals("Pay rent", t.title)
        assertEquals(LocalDateTime(2026, 8, 21, 9, 30), t.dueDate)
        assertEquals(Priority.HIGH, t.priority)
        assertEquals(TaskStatus.TODO, t.status)
        assertTrue(t.description.contains("Remember the check"))
        assertTrue(t.description.contains("#finance"))
    }

    @Test
    fun `completed status maps to done`() {
        val csv = header() + "\n,Old,Task,,,2026-08-01,2026-08-02,,None,Completed,,,,,,"
        val result = parser.parse(csv)
        assertEquals(TaskStatus.DONE, result.tasks.single().status)
    }

    @Test
    fun `date-only due dates parse to midnight`() {
        val csv = header() + "\n,,Task,,,2026-08-01,2026/9/5,,None,0,,,,,,"
        val result = parser.parse(csv)
        assertEquals(LocalDateTime(2026, 9, 5, 0, 0), result.tasks.single().dueDate)
    }

    @Test
    fun `missing task name column yields warning`() {
        val result = parser.parse("Foo,Bar\n1,2")
        assertTrue(result.tasks.isEmpty())
        assertTrue(result.warnings.any { it.contains("Task Name") })
    }

    @Test
    fun `blank rows are skipped and quoted commas survive`() {
        val csv = header() + "\n" +
            ",,\"Buy eggs, milk\",,,2026-08-01,2026-08-03,,None,Normal,,,,,," + "\n" +
            ",,,,,,,,,,,,,,,"
        val result = parser.parse(csv)
        assertEquals(1, result.tasks.size)
        assertEquals("Buy eggs, milk", result.tasks.single().title)
    }

    private fun header() =
        "Folder Name,List Name,Task Name,Task Content,Is All Day,Start Date,Due Date,Reminder,Priority,Status,Created Time,Completed Time,Order,Task ID,Parent ID,Project ID,Tags"
}
