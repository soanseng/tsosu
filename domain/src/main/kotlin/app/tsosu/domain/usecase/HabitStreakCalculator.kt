package app.tsosu.domain.usecase

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

/**
 * True consecutive-day streak from completion dates.
 *
 * A streak counts back from today when completed today, otherwise from
 * yesterday (streak pauses, not resets). Dates must be distinct days;
 * duplicates inflate nothing.
 */
object HabitStreakCalculator {

    fun today(): LocalDate =
        Clock.System.todayIn(TimeZone.currentSystemDefault())

    fun consecutiveDays(
        distinctDates: Set<LocalDate>,
        today: LocalDate = today(),
    ): Int {
        if (distinctDates.isEmpty()) return 0

        val anchor = if (today in distinctDates) today else today.minus(1, DateTimeUnit.DAY)
        if (anchor !in distinctDates) return 0

        var streak = 0
        var cursor = anchor
        while (cursor in distinctDates) {
            streak++
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }

    fun countInWindow(
        distinctDates: Set<LocalDate>,
        days: Int,
        today: LocalDate = today(),
    ): Int {
        val start = today.minus((days - 1).toLong(), DateTimeUnit.DAY)
        return distinctDates.count { it in start..today }
    }

    /**
     * The first gap day (if any) directly before the current streak's
     * anchor, i.e. the day a freeze would bridge to extend the streak.
     */
    fun firstGapBeforeStreak(
        distinctDates: Set<LocalDate>,
        today: LocalDate = today(),
    ): LocalDate? {
        if (distinctDates.isEmpty()) return null

        val anchor = if (today in distinctDates) today else today.minus(1, DateTimeUnit.DAY)
        if (anchor !in distinctDates) return null

        var cursor = anchor
        while (cursor in distinctDates) {
            cursor = cursor.minus(1, DateTimeUnit.DAY)
        }
        // cursor is the first missing day right below the streak.
        return if (cursor < anchor) cursor else null
    }
}
