package app.tsosu.data.calendar

import kotlin.test.Test
import kotlin.test.assertTrue

class VEventBuilderTest {

    private val builder = VEventBuilder()

    @Test
    fun `builds VEVENT with title and due date`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-1",
            title = "Buy groceries",
            description = "Get milk and eggs",
            dueDate = "2026-03-10T09:00:00",
            estimatedMinutes = null,
        )
        assertTrue(ical.contains("BEGIN:VCALENDAR"))
        assertTrue(ical.contains("BEGIN:VEVENT"))
        assertTrue(ical.contains("SUMMARY:Buy groceries"))
        assertTrue(ical.contains("UID:task-uuid-1"))
        assertTrue(ical.contains("DTSTART:20260310T090000"))
        assertTrue(ical.contains("END:VEVENT"))
        assertTrue(ical.contains("END:VCALENDAR"))
    }

    @Test
    fun `uses estimatedMinutes for DURATION`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-2",
            title = "Meeting",
            description = "",
            dueDate = "2026-03-10T14:00:00",
            estimatedMinutes = 30,
        )
        assertTrue(ical.contains("DURATION:PT30M"))
    }

    @Test
    fun `defaults to 1 hour when no estimate`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-3",
            title = "Task",
            description = "",
            dueDate = "2026-03-10T10:00:00",
            estimatedMinutes = null,
        )
        assertTrue(ical.contains("DURATION:PT60M"))
    }

    @Test
    fun `escapes special characters in title`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-4",
            title = "Meeting, with; semicolons",
            description = "",
            dueDate = "2026-03-10T10:00:00",
            estimatedMinutes = null,
        )
        assertTrue(ical.contains("SUMMARY:Meeting\\, with\\; semicolons"))
    }

    @Test
    fun `includes description when present`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-5",
            title = "Task",
            description = "Some notes",
            dueDate = "2026-03-10T10:00:00",
            estimatedMinutes = null,
        )
        assertTrue(ical.contains("DESCRIPTION:Some notes"))
    }

    @Test
    fun `omits description when empty`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-6",
            title = "Task",
            description = "",
            dueDate = "2026-03-10T10:00:00",
            estimatedMinutes = null,
        )
        assertTrue(!ical.contains("DESCRIPTION:"))
    }
}
