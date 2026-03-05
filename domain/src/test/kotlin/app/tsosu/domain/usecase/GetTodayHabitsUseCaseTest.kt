package app.tsosu.domain.usecase

import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.repository.HabitRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetTodayHabitsUseCaseTest {
    private val habitRepository = mockk<HabitRepository>()
    private val useCase = GetTodayHabitsUseCase(habitRepository)
    private val today = LocalDate(2026, 3, 5)

    @Test
    fun `returns habits with completion status`() = runTest {
        val habits = listOf(
            Habit(id = "h1", title = "Meditate"),
            Habit(id = "h2", title = "Exercise"),
            Habit(id = "h3", title = "Read"),
        )
        val completions = listOf(
            HabitCompletion("h1", today, Clock.System.now()),
            HabitCompletion("h3", today, Clock.System.now()),
        )
        every { habitRepository.getActiveHabits() } returns flowOf(habits)
        every { habitRepository.getTodayCompletions() } returns flowOf(completions)

        useCase().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertTrue(result[0].isCompletedToday)   // h1 completed
            assertFalse(result[1].isCompletedToday)  // h2 not completed
            assertTrue(result[2].isCompletedToday)   // h3 completed
            awaitComplete()
        }
    }

    @Test
    fun `handles no completions`() = runTest {
        val habits = listOf(Habit(id = "h1", title = "Meditate"))
        every { habitRepository.getActiveHabits() } returns flowOf(habits)
        every { habitRepository.getTodayCompletions() } returns flowOf(emptyList())

        useCase().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertFalse(result[0].isCompletedToday)
            awaitComplete()
        }
    }
}
