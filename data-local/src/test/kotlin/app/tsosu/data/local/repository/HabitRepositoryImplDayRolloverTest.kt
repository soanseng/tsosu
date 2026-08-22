package app.tsosu.data.local.repository

import app.tsosu.data.local.dao.HabitDao
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class HabitRepositoryImplDayRolloverTest {

    @Test
    fun `today completions re-query with the new date after local midnight`() = runTest {
        val tz = TimeZone.currentSystemDefault()
        val nextMidnight = LocalDate(2026, 8, 21).plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
        var currentNow: Instant = nextMidnight - 1_000.milliseconds

        val queriedDates = mutableListOf<Long>()
        val dao = mockk<HabitDao> {
            every { getCompletionsForDate(capture(queriedDates)) } returns flowOf(emptyList())
        }
        val repo = HabitRepositoryImpl(dao, now = { currentNow })

        // First emission (just before midnight) flips the clock past midnight,
        // so when the ticker's delay elapses it must observe the NEW day.
        val emissions = repo.getTodayCompletions()
            .take(2)
            .onEach { currentNow = nextMidnight + 2_000.milliseconds }
            .toList()

        assertEquals(2, emissions.size)
        val day1 = (nextMidnight - 1_000.milliseconds).toLocalDateTime(tz).date.toEpochDays().toLong()
        val day2 = (nextMidnight + 2_000.milliseconds).toLocalDateTime(tz).date.toEpochDays().toLong()
        assertEquals(listOf(day1, day2), queriedDates)
    }
}
