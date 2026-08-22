package app.tsosu.data.calendar

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IcsParserTest {

    private val window = LocalDate(2026, 8, 1)..LocalDate(2026, 8, 31)

    @Test
    fun `parses single timed event inside window`() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:e1
            SUMMARY:Dentist
            DTSTART:20260821T090000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        val events = IcsParser.parse(ics, window.start, window.endInclusive)
        assertEquals(1, events.size)
        assertEquals("Dentist", events[0].summary)
        assertEquals(LocalDate(2026, 8, 21), events[0].start)
        assertEquals(false, events[0].allDay)
    }

    @Test
    fun `unfolds continuation lines and unescapes text`() {
        val ics = "BEGIN:VCALENDAR\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:e2\r\n" +
            "SUMMARY:Very long title part one \r\n" +
            " continued here\\, with comma\r\n" +
            "DTSTART;VALUE=DATE:20260810\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        val events = IcsParser.parse(ics, window.start, window.endInclusive)
        assertEquals(1, events.size)
        assertEquals("Very long title part one continued here, with comma", events[0].summary)
        assertEquals(true, events[0].allDay)
    }

    @Test
    fun `expands daily rrule within the window only`() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:e3
            SUMMARY:Standup
            DTSTART:20260701T093000
            RRULE:FREQ=DAILY
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        val events = IcsParser.parse(ics, window.start, window.endInclusive)
        assertEquals(31, events.size, "one occurrence per day in August")
        assertEquals(LocalDate(2026, 8, 1), events.first().start)
        assertEquals(LocalDate(2026, 8, 31), events.last().start)
    }

    @Test
    fun `weekly byday rrule keeps weekday alignment`() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:e4
            SUMMARY:Weekly sync
            DTSTART:20260805T110000
            RRULE:FREQ=WEEKLY;BYDAY=WE
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        val events = IcsParser.parse(ics, window.start, window.endInclusive)
        assertEquals(listOf(5, 12, 19, 26).map { LocalDate(2026, 8, it) }, events.map { it.start })
    }

    @Test
    fun `event outside window is dropped`() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:e5
            SUMMARY:Old thing
            DTSTART:20260101T090000
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        assertTrue(IcsParser.parse(ics, window.start, window.endInclusive).isEmpty())
    }

    @Test
    fun `valarm description does not clobber event summary`() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:e6
            SUMMARY:Real summary
            DTSTART:20260815T100000
            BEGIN:VALARM
            TRIGGER:-PT10M
            ACTION:DISPLAY
            DESCRIPTION:Alarm text
            END:VALARM
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        val events = IcsParser.parse(ics, window.start, window.endInclusive)
        assertEquals("Real summary", events.single().summary)
    }
}
