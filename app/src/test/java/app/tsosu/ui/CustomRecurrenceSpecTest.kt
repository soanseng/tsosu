package app.tsosu.ui

import app.tsosu.ui.components.RecurrencePreset
import app.tsosu.ui.components.parseCustomSpec
import app.tsosu.ui.components.toRrule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The custom builder must round-trip with the parser's keyword output so a
 * rule typed as "every 2 days" and one chipped in the builder are the same
 * task to the database.
 */
class CustomRecurrenceSpecTest {

    @Test
    fun `builder daily interval round-trips with keyword parse`() {
        // "every 2 days" keyword parse
        val keyword = (app.tsosu.domain.recurrence.RecurrenceParser().parse("every 2 days")
            as app.tsosu.domain.recurrence.RecurrenceResult.Success).rrule
        val built = parseCustomSpec(keyword).toRrule()
        assertEquals(keyword, built)
    }

    @Test
    fun `builder weekly weekdays sorts BYDAY canonically`() {
        val spec = parseCustomSpec("RRULE:FREQ=WEEKLY;BYDAY=FR,MO")
            .copy(interval = 2)
        assertEquals("RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,FR", spec.toRrule())
    }

    @Test
    fun `builder monthly day-of-month`() {
        assertEquals(
            "RRULE:FREQ=MONTHLY;BYMONTHDAY=15",
            parseCustomSpec("RRULE:FREQ=MONTHLY;BYMONTHDAY=15").toRrule(),
        )
    }

    @Test
    fun `weekday multi-select preserves preset shape`() {
        // 每…一二三 via builder must equal what RecurrenceExpander expects
        val spec = parseCustomSpec(null).copy(
            freq = "WEEKLY",
            weekdays = setOf(1, 3, 5),
        )
        assertEquals("RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR", spec.toRrule())
    }

    @Test
    fun `interval one and empty selections collapse to preset rules`() {
        assertEquals("RRULE:FREQ=DAILY", parseCustomSpec(null).toRrule())
        assertEquals(
            RecurrencePreset.DAILY.rrule,
            parseCustomSpec(null).toRrule(),
        )
    }

    @Test
    fun `junk rules fall back to safe defaults`() {
        val spec = parseCustomSpec("RRULE:FREQ=HOURLY;INTERVAL=9;BYMONTHDAY=99")
        assertEquals("DAILY", spec.freq)
        assertEquals(9, spec.interval.coerceAtMost(9))
        assertEquals(null, spec.monthDay)
    }
}
