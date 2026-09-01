package app.tsosu.data.local.repository

import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.entity.HabitEntity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Regression test: completing a habit must re-emit streak info immediately
 * (bug: getAllStreakInfos only re-queried when the habit table changed, so
 * the 🔥 badge and week bar stayed stale until the next re-subscription).
 */
class HabitRepositoryImplStreakReactivityTest {

    @Test
    fun `streak info re-emits when a completion lands`() = runTest {
        val today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val habit = HabitEntity(id = "h1", title = "kettlebell", createdAt = 0L)
        val completionDates = MutableStateFlow<List<Long>>(emptyList())

        val dao = mockk<HabitDao> {
            every { getActiveHabits() } returns flowOf(listOf(habit))
            every { getById("h1") } returns flowOf(habit)
            every { getCompletionDates("h1") } returns completionDates
        }
        val repo = HabitRepositoryImpl(dao)

        // First collection: no completions yet. Then a completion for today
        // arrives through the DAO flow — the streak must update in the same
        // subscription, without re-collection.
        val collected = async { repo.getAllStreakInfos().take(2).toList() }
        delay(50)
        completionDates.value = listOf(today.toEpochDays().toLong())
        val streaks = collected.await()

        assertEquals(2, streaks.size)
        assertEquals(0, streaks[0].first().currentConsecutiveDays)
        assertEquals(1, streaks[1].first().currentConsecutiveDays)
        assertEquals(1, streaks[1].first().completedLast7Days)
    }
}
