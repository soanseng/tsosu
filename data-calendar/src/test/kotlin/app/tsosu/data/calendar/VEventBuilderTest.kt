package app.tsosu.data.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
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

    // ── RFC 5545 compliance (added with the folding/DTSTAMP fix) ──

    @Test
    fun `long lines are folded at 75 octets with continuation prefix`() {
        val title = "A very long summary " + "x".repeat(120)
        val body = builder.buildVEventBody(
            uid = "u1",
            title = title,
            description = "",
            dueDate = "2026-08-21T09:00",
            estimatedMinutes = 30,
        )
        val summaryLines = body.lineSequence()
            .filter { it.startsWith("SUMMARY:") || it.startsWith(" ") }
            .toList()
        assertTrue(summaryLines.size > 1, "should have been folded into continuations")
        // Every physical line must be <= 75 octets.
        for (line in body.lineSequence()) {
            assertTrue(
                line.toByteArray(Charsets.UTF_8).size <= 75,
                "line exceeds 75 octets: ${line.take(40)}…",
            )
        }
        // Continuation lines start with a space; the logical text round-trips.
        val logical = summaryLines.joinToString("") { line -> line.removePrefix("SUMMARY:").removePrefix(" ") }
        assertEquals(title, logical)
    }

    @Test
    fun `multi-byte characters are never split across fold boundary`() {
        // Each CJK char is 3 bytes: 30 chars = 90 bytes, forcing a fold.
        val title = "檢查一下這個很長的標題是否會被正確折疊".repeat(2)
        val body = builder.buildVEventBody(
            uid = "u1",
            title = title,
            description = "",
            dueDate = "2026-08-21T09:00",
            estimatedMinutes = 30,
        )
        // Every folded segment must decode to a full-width char boundary —
        // joined logical line must equal the original title.
        val logical = body.lineSequence()
            .filter { it.startsWith("SUMMARY:") || it.startsWith(" ") }
            .joinToString("") { line -> line.removePrefix("SUMMARY:").removePrefix(" ") }
        assertEquals(title, logical)
    }

    @Test
    fun `DTSTAMP is present and UTC-flagged`() {
        val body = builder.buildVEventBody(
            uid = "u1",
            title = "t",
            description = "",
            dueDate = "2026-08-21T09:00",
            estimatedMinutes = 30,
            dtStampEpochMillis = 1_800_000_000_000,
        )
        assertTrue(body.contains("DTSTAMP:20270115T080000Z"), "expected UTC DTSTAMP, got:\n$body")
    }

    @Test
    fun `text values escape CRLF and CR newlines`() {
        val body = builder.buildVEventBody(
            uid = "u1",
            title = "t",
            description = "line1\r\nline2\rline3\nline4",
            dueDate = "2026-08-21T09:00",
            estimatedMinutes = 30,
        )
        assertTrue(body.contains("DESCRIPTION:line1\\nline2\\nline3\\nline4"))
    }
}
