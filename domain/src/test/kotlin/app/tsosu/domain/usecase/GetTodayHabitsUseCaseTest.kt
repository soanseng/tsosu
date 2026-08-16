package app.tsosu.domain.usecase

import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
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

    @Test
    fun `weekend hides WEEKDAYS habits but keeps DAILY`() = runTest {
        // 2026-08-15 is a Saturday (ISO dayNumber 6)
        val saturday = LocalDate(2026, 8, 15)
        val weekendUseCase = GetTodayHabitsUseCase(habitRepository, today = { saturday })
        val habits = listOf(
            Habit(id = "h1", title = "Meditate", frequency = HabitFrequency.DAILY),
            Habit(id = "h2", title = "Standup", frequency = HabitFrequency.WEEKDAYS),
        )
        every { habitRepository.getActiveHabits() } returns flowOf(habits)
        every { habitRepository.getTodayCompletions() } returns flowOf(emptyList())

        weekendUseCase().test {
            val result = awaitItem()
            assertEquals(listOf("h1"), result.map { it.habit.id })
            awaitComplete()
        }
    }

    @Test
    fun `weekday keeps WEEKDAYS habits`() = runTest {
        // 2026-08-14 is a Friday (ISO dayNumber 5)
        val friday = LocalDate(2026, 8, 14)
        val weekdayUseCase = GetTodayHabitsUseCase(habitRepository, today = { friday })
        val habits = listOf(
            Habit(id = "h1", title = "Meditate", frequency = HabitFrequency.DAILY),
            Habit(id = "h2", title = "Standup", frequency = HabitFrequency.WEEKDAYS),
        )
        every { habitRepository.getActiveHabits() } returns flowOf(habits)
        every { habitRepository.getTodayCompletions() } returns flowOf(emptyList())

        weekdayUseCase().test {
            val result = awaitItem()
            assertEquals(listOf("h1", "h2"), result.map { it.habit.id })
            awaitComplete()
        }
    }

    @Test
    fun `scheduled weekday shows, unscheduled hides`() = runTest {
        // 2026-08-17 is a Monday (ISO 1)
        val monday = LocalDate(2026, 8, 17)
        val mondayUseCase = GetTodayHabitsUseCase(habitRepository, today = { monday })
        val habits = listOf(
            Habit(id = "h1", title = "MWF run", weekdays = setOf(1, 3, 5)),
            Habit(id = "h2", title = "Daily stretch"),
        )
        every { habitRepository.getActiveHabits() } returns flowOf(habits)
        every { habitRepository.getTodayCompletions() } returns flowOf(emptyList())

        mondayUseCase().test {
            assertEquals(listOf("h1", "h2"), awaitItem().map { it.habit.id })
            awaitComplete()
        }

        // Tuesday (ISO 2): MWF habit hidden, daily kept.
        val tuesday = LocalDate(2026, 8, 18)
        val tuesdayUseCase = GetTodayHabitsUseCase(habitRepository, today = { tuesday })
        tuesdayUseCase().test {
            assertEquals(listOf("h2"), awaitItem().map { it.habit.id })
            awaitComplete()
        }
    }
}
