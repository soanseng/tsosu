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

    @Test
    fun `includes VALARM when reminderMinutesBefore is set`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-7",
            title = "Dentist appointment",
            description = "",
            dueDate = "2026-03-10T14:00:00",
            estimatedMinutes = 30,
            reminderMinutesBefore = 15,
        )
        assertTrue(ical.contains("BEGIN:VALARM"))
        assertTrue(ical.contains("TRIGGER:-PT15M"))
        assertTrue(ical.contains("ACTION:DISPLAY"))
        assertTrue(ical.contains("DESCRIPTION:Dentist appointment"))
        assertTrue(ical.contains("END:VALARM"))
    }

    @Test
    fun `VALARM is inside VEVENT`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-8",
            title = "Review",
            description = "",
            dueDate = "2026-03-10T10:00:00",
            estimatedMinutes = null,
            reminderMinutesBefore = 30,
        )
        val veventStart = ical.indexOf("BEGIN:VEVENT")
        val veventEnd = ical.indexOf("END:VEVENT")
        val valarmStart = ical.indexOf("BEGIN:VALARM")
        val valarmEnd = ical.indexOf("END:VALARM")
        assertTrue(valarmStart > veventStart)
        assertTrue(valarmEnd < veventEnd)
    }

    @Test
    fun `omits VALARM when reminderMinutesBefore is null`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-9",
            title = "Task",
            description = "",
            dueDate = "2026-03-10T10:00:00",
            estimatedMinutes = null,
            reminderMinutesBefore = null,
        )
        assertTrue(!ical.contains("BEGIN:VALARM"))
        assertTrue(!ical.contains("TRIGGER:"))
        assertTrue(!ical.contains("END:VALARM"))
    }

    @Test
    fun `VALARM escapes special characters in title`() {
        val ical = builder.buildVEvent(
            uid = "task-uuid-10",
            title = "Call, Dr; Smith",
            description = "",
            dueDate = "2026-03-10T10:00:00",
            estimatedMinutes = null,
            reminderMinutesBefore = 10,
        )
        assertTrue(ical.contains("DESCRIPTION:Call\\, Dr\\; Smith"))
    }
}
