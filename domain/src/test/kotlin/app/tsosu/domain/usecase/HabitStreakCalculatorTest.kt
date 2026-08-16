package app.tsosu.domain.usecase

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HabitStreakCalculatorTest {

    private val today = LocalDate.parse("2026-08-16")

    @Test
    fun `empty dates give zero streak`() {
        assertEquals(0, HabitStreakCalculator.consecutiveDays(emptySet(), today))
    }

    @Test
    fun `single completion today gives streak of one`() {
        assertEquals(1, HabitStreakCalculator.consecutiveDays(setOf(today), today))
    }

    @Test
    fun `single completion yesterday still counts one (paused not reset)`() {
        val yesterday = LocalDate.parse("2026-08-15")
        assertEquals(1, HabitStreakCalculator.consecutiveDays(setOf(yesterday), today))
    }

    @Test
    fun `single completion two days ago gives zero`() {
        assertEquals(0, HabitStreakCalculator.consecutiveDays(setOf(LocalDate.parse("2026-08-14")), today))
    }

    @Test
    fun `consecutive run counts fully`() {
        val dates = setOf(
            LocalDate.parse("2026-08-16"),
            LocalDate.parse("2026-08-15"),
            LocalDate.parse("2026-08-14"),
        )
        assertEquals(3, HabitStreakCalculator.consecutiveDays(dates, today))
    }

    @Test
    fun `gap breaks the streak at the gap`() {
        val dates = setOf(
            LocalDate.parse("2026-08-16"),
            LocalDate.parse("2026-08-15"),
            LocalDate.parse("2026-08-12"),
        )
        assertEquals(2, HabitStreakCalculator.consecutiveDays(dates, today))
    }

    @Test
    fun `duplicate same-day completions cannot inflate the streak`() {
        // Distinct dates by contract — same day repeated is a single date.
        val dates = setOf(today, today, today)
        assertEquals(1, HabitStreakCalculator.consecutiveDays(dates, today))
    }

    @Test
    fun `countInWindow counts distinct days in the window`() {
        val dates = setOf(
            LocalDate.parse("2026-08-16"),
            LocalDate.parse("2026-08-15"),
            LocalDate.parse("2026-08-01"),
        )
        assertEquals(2, HabitStreakCalculator.countInWindow(dates, 7, today))
        assertEquals(3, HabitStreakCalculator.countInWindow(dates, 30, today))
    }
}
