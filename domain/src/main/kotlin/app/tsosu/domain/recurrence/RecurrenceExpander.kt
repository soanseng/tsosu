package app.tsosu.domain.recurrence

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Computes the next occurrence date for the RRULE subset Tsosu generates:
 * `FREQ=DAILY|WEEKLY|MONTHLY|YEARLY`, `INTERVAL=n`, `BYDAY=MO,...`, `BYMONTHDAY=n`.
 *
 * Semantics: the next occurrence is the first date in the rule's sequence that is
 * strictly after `today`. The sequence starts stepping from the completed
 * occurrence's due date (the anchor), which keeps weekday alignment for weekly
 * rules — e.g. completing a `FREQ=WEEKLY;INTERVAL=2` task still lands on the same
 * week parity. Unknown or unparsable rules return null and the task behaves as a
 * plain (non-recurring) task.
 */
object RecurrenceExpander {

    private const val MAX_STEPS = 10_000

    fun nextDueDate(rule: String?, anchorDue: LocalDate?, today: LocalDate): LocalDate? {
        if (rule.isNullOrBlank()) return null
        val parts = parseRule(rule) ?: return null
        val interval = parts.interval
        val anchor = anchorDue ?: today
        val next = when (parts.freq) {
            "DAILY" -> stepByPeriod(anchor, today) { it.plus(DatePeriod(days = interval)) }
            "WEEKLY" -> {
                val byDay = parts.byDay
                if (byDay.isNullOrEmpty()) {
                    stepByPeriod(anchor, today) { it.plus(DatePeriod(days = 7 * interval)) }
                } else {
                    nextByWeekday(anchor, today, byDay, interval)
                }
            }
            "MONTHLY" -> {
                val targetDay = parts.byMonthDay ?: anchor.dayOfMonth
                stepByPeriod(anchor, today) {
                    it.plus(DatePeriod(months = interval)).withDayOfMonthClamped(targetDay)
                }
            }
            "YEARLY" -> stepByPeriod(anchor, today) { it.plus(DatePeriod(years = interval)) }
            else -> null
        } ?: return null

        // A bounded rule (`until <date>`) ends the series: the occurrence on the
        // UNTIL date is the last one, so a next date past the bound means the task
        // completes like a plain task instead of recurring.
        val until = parts.until ?: return next
        return next.takeIf { it <= until }
    }
    /** Repeatedly steps from the anchor until the date is strictly after today. */
    private inline fun stepByPeriod(
        anchor: LocalDate,
        today: LocalDate,
        step: (LocalDate) -> LocalDate,
    ): LocalDate? {
        var current = anchor
        var steps = 0
        while (current <= today) {
            current = step(current)
            if (++steps > MAX_STEPS) return null
        }
        return current
    }

    /**
     * Weekly rules with BYDAY: scans forward day by day from the anchor. With
     * INTERVAL > 1 the candidate must also fall in an aligned week (a whole
     * number of INTERVAL-weeks after the anchor's week).
     */
    private fun nextByWeekday(
        anchor: LocalDate,
        today: LocalDate,
        byDay: Set<DayOfWeek>,
        interval: Int,
    ): LocalDate? {
        var current = anchor
        var steps = 0
        val anchorWeekStart = anchor.minus(DatePeriod(days = anchor.dayOfWeek.ordinal))
        while (current <= today) {
            current = current.plus(DatePeriod(days = 1))
            if (++steps > MAX_STEPS) return null
        }
        while (true) {
            if (current.dayOfWeek in byDay) {
                if (interval == 1) return current
                val weekStart = current.minus(DatePeriod(days = current.dayOfWeek.ordinal))
                val weeksFromAnchor = (weekStart - anchorWeekStart).days / 7
                if (weeksFromAnchor % interval == 0) return current
            }
            current = current.plus(DatePeriod(days = 1))
            if (++steps > MAX_STEPS) return null
        }
    }

    private fun LocalDate.withDayOfMonthClamped(targetDay: Int): LocalDate {
        val maxDay = when (monthNumber) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear) 29 else 28
            else -> 31
        }
        val clamped = targetDay.coerceIn(1, maxDay)
        // Re-anchor to day 1 first so clamping (e.g. Mar 31 -> Feb 28) is stable.
        return LocalDate(year, monthNumber, 1).plus(DatePeriod(days = clamped - 1))
    }

    private fun parseRule(rule: String): RuleParts? {
        val kv = rule.removePrefix("RRULE:")
            .split(";")
            .mapNotNull { entry ->
                val idx = entry.indexOf('=')
                if (idx <= 0) null else entry.take(idx) to entry.substring(idx + 1)
            }
            .toMap()
        val freq = kv["FREQ"]?.uppercase() ?: return null
        val interval = kv["INTERVAL"]?.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val byDay = kv["BYDAY"]?.split(",")?.mapNotNull { it.toDayOfWeek() }?.toSet()
        val byMonthDay = kv["BYMONTHDAY"]?.toIntOrNull()
        val until = kv["UNTIL"]?.take(8)?.let {
            runCatching {
                LocalDate(it.take(4).toInt(), it.substring(4, 6).toInt(), it.substring(6, 8).toInt())
            }.getOrNull()
        }
        return RuleParts(freq, interval, byDay, byMonthDay, until)
    }

    private data class RuleParts(
        val freq: String,
        val interval: Int,
        val byDay: Set<DayOfWeek>?,
        val byMonthDay: Int?,
        val until: LocalDate?,
    )
    private fun String.toDayOfWeek(): DayOfWeek? = when (uppercase()) {
        "MO" -> DayOfWeek.MONDAY
        "TU" -> DayOfWeek.TUESDAY
        "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY
        "FR" -> DayOfWeek.FRIDAY
        "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY
        else -> null
    }

    private val LocalDate.isLeapYear: Boolean
        get() = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
