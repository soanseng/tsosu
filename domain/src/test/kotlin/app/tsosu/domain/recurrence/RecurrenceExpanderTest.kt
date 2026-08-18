package app.tsosu.domain.recurrence

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecurrenceExpanderTest {

    // 2026-01-05 is a Monday; 2026-01-07 Wednesday; 2026-01-09 Friday; 2026-01-12 next Monday.
    private val mon = LocalDate(2026, 1, 5)
    private val today = LocalDate(2026, 8, 17)

    @Test
    fun `null or blank rule is not recurring`() {
        assertNull(RecurrenceExpander.nextDueDate(null, today, today))
        assertNull(RecurrenceExpander.nextDueDate("  ", today, today))
    }

    @Test
    fun `unparsable or unsupported rules are not recurring`() {
        assertNull(RecurrenceExpander.nextDueDate("not-a-rule", today, today))
        assertNull(RecurrenceExpander.nextDueDate("FREQ=HOURLY", today, today))
    }

    @Test
    fun `daily due today completes to tomorrow`() {
        assertEquals(
            LocalDate(2026, 8, 18),
            RecurrenceExpander.nextDueDate("RRULE:FREQ=DAILY", today, today),
        )
    }

    @Test
    fun `daily overdue instance catches up to tomorrow not today`() {
        // Due 3 days ago, completed today -> next occurrence is strictly after today.
        assertEquals(
            LocalDate(2026, 8, 18),
            RecurrenceExpander.nextDueDate("FREQ=DAILY", LocalDate(2026, 8, 14), today),
        )
    }

    @Test
    fun `daily interval 2 keeps two-day spacing`() {
        assertEquals(
            LocalDate(2026, 8, 19),
            RecurrenceExpander.nextDueDate("FREQ=DAILY;INTERVAL=2", today, today),
        )
    }

    @Test
    fun `weekly without byday advances one week`() {
        assertEquals(
            LocalDate(2026, 1, 12),
            RecurrenceExpander.nextDueDate("FREQ=WEEKLY", mon, mon),
        )
    }

    @Test
    fun `weekly byday picks next matching weekday`() {
        // Mon rule-set, completed Mon -> next is Wed.
        assertEquals(
            LocalDate(2026, 1, 7),
            RecurrenceExpander.nextDueDate("FREQ=WEEKLY;BYDAY=MO,WE,FR", mon, mon),
        )
        // Completed on Wed -> next is Fri.
        assertEquals(
            LocalDate(2026, 1, 9),
            RecurrenceExpander.nextDueDate("FREQ=WEEKLY;BYDAY=MO,WE,FR", mon, LocalDate(2026, 1, 7)),
        )
    }

    @Test
    fun `weekly interval 2 keeps week parity`() {
        // Anchor Mon Jan 5; on Mon Jan 12 the next aligned Monday is Jan 19.
        assertEquals(
            LocalDate(2026, 1, 19),
            RecurrenceExpander.nextDueDate(
                "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO",
                mon,
                LocalDate(2026, 1, 12),
            ),
        )
    }

    @Test
    fun `monthly advances one month`() {
        assertEquals(
            LocalDate(2026, 9, 17),
            RecurrenceExpander.nextDueDate("FREQ=MONTHLY", LocalDate(2026, 8, 17), today),
        )
    }

    @Test
    fun `monthly bymonthday targets the given day`() {
        assertEquals(
            LocalDate(2026, 2, 1),
            RecurrenceExpander.nextDueDate("FREQ=MONTHLY;BYMONTHDAY=1", LocalDate(2026, 1, 1), LocalDate(2026, 1, 15)),
        )
    }

    @Test
    fun `monthly clamps short months`() {
        // Jan 31 -> Feb 28 (2026 is not a leap year).
        assertEquals(
            LocalDate(2026, 2, 28),
            RecurrenceExpander.nextDueDate("FREQ=MONTHLY", LocalDate(2026, 1, 31), LocalDate(2026, 1, 31)),
        )
    }

    @Test
    fun `yearly clamps feb 29`() {
        assertEquals(
            LocalDate(2025, 2, 28),
            RecurrenceExpander.nextDueDate("FREQ=YEARLY", LocalDate(2024, 2, 29), LocalDate(2024, 2, 29)),
        )
    }

    @Test
    fun `no anchor defaults to today as anchor`() {
        // Daily with no due date -> tomorrow.
        assertEquals(
            LocalDate(2026, 8, 18),
            RecurrenceExpander.nextDueDate("RRULE:FREQ=DAILY", null, today),
        )
    }

    @Test
    fun `bounded rule recurs while next is within until`() {
        // "daily until 8/19": completing today's instance still schedules tomorrow.
        assertEquals(
            LocalDate(2026, 8, 18),
            RecurrenceExpander.nextDueDate(
                "FREQ=DAILY;UNTIL=20260819T235959Z",
                today,
                today,
            ),
        )
    }

    @Test
    fun `bounded rule ends when next passes until`() {
        // "daily until 8/17" (today): the series is over -> plain completion.
        assertNull(
            RecurrenceExpander.nextDueDate(
                "FREQ=DAILY;UNTIL=20260817T235959Z",
                today,
                today,
            ),
        )
    }
}
