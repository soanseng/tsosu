package app.tsosu.domain.usecase

import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompleteHabitUseCaseTest {
    private val habitRepository = mockk<HabitRepository>()
    private val useCase = CompleteHabitUseCase(habitRepository)

    @Test
    fun `records completion for date`() = runTest {
        val date = LocalDate(2026, 3, 5)
        val completion = HabitCompletion("habit-1", date, Clock.System.now())
        coEvery { habitRepository.completeHabit("habit-1", date) } returns Result.success(completion)

        val result = useCase("habit-1", date)

        assertTrue(result.isSuccess)
        assertEquals("habit-1", result.getOrThrow().habitId)
        assertEquals(date, result.getOrThrow().date)
        coVerify { habitRepository.completeHabit("habit-1", date) }
    }
}
