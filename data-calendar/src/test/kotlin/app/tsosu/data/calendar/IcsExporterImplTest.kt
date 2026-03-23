package app.tsosu.data.calendar

import app.tsosu.domain.model.Task
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IcsExporterImplTest {

    private val exporter = IcsExporterImpl()

    @Test
    fun `exports multiple tasks in single VCALENDAR`() {
        val tasks = listOf(
            Task(
                id = "task-1",
                title = "Buy groceries",
                dueDate = LocalDateTime(2026, 3, 10, 9, 0),
            ),
            Task(
                id = "task-2",
                title = "Meeting",
                dueDate = LocalDateTime(2026, 3, 11, 14, 0),
                estimatedMinutes = 30,
            ),
        )

        val ics = exporter.exportTasks(tasks)

        // Single calendar wrapper
        assertTrue(ics.indexOf("BEGIN:VCALENDAR") == ics.lastIndexOf("BEGIN:VCALENDAR"))
        assertTrue(ics.indexOf("END:VCALENDAR") == ics.lastIndexOf("END:VCALENDAR"))

        // Both events present
        assertTrue(ics.contains("UID:task-1"))
        assertTrue(ics.contains("SUMMARY:Buy groceries"))
        assertTrue(ics.contains("UID:task-2"))
        assertTrue(ics.contains("SUMMARY:Meeting"))
        assertTrue(ics.contains("DURATION:PT30M"))
    }

    @Test
    fun `skips tasks without due dates`() {
        val tasks = listOf(
            Task(id = "task-1", title = "No due date", dueDate = null),
            Task(
                id = "task-2",
                title = "Has due date",
                dueDate = LocalDateTime(2026, 3, 10, 9, 0),
            ),
        )

        val ics = exporter.exportTasks(tasks)

        assertFalse(ics.contains("UID:task-1"))
        assertTrue(ics.contains("UID:task-2"))
    }

    @Test
    fun `includes VALARM when reminderTime is before dueDate time`() {
        val tasks = listOf(
            Task(
                id = "task-1",
                title = "Dentist",
                dueDate = LocalDateTime(2026, 3, 10, 14, 0),
                reminderTime = LocalTime(13, 45),
            ),
        )

        val ics = exporter.exportTasks(tasks)

        assertTrue(ics.contains("BEGIN:VALARM"))
        assertTrue(ics.contains("TRIGGER:-PT15M"))
        assertTrue(ics.contains("ACTION:DISPLAY"))
    }

    @Test
    fun `omits VALARM when no reminderTime set`() {
        val tasks = listOf(
            Task(
                id = "task-1",
                title = "Task",
                dueDate = LocalDateTime(2026, 3, 10, 14, 0),
                reminderTime = null,
            ),
        )

        val ics = exporter.exportTasks(tasks)

        assertFalse(ics.contains("BEGIN:VALARM"))
    }

    @Test
    fun `omits VALARM when reminderTime is after dueDate time`() {
        val tasks = listOf(
            Task(
                id = "task-1",
                title = "Task",
                dueDate = LocalDateTime(2026, 3, 10, 9, 0),
                reminderTime = LocalTime(10, 0),
            ),
        )

        val ics = exporter.exportTasks(tasks)

        assertFalse(ics.contains("BEGIN:VALARM"))
    }

    @Test
    fun `returns empty calendar when all tasks lack due dates`() {
        val tasks = listOf(
            Task(id = "task-1", title = "No due date"),
        )

        val ics = exporter.exportTasks(tasks)

        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("END:VCALENDAR"))
        assertFalse(ics.contains("BEGIN:VEVENT"))
    }
}
