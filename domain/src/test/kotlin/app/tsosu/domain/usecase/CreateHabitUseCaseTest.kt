package app.tsosu.domain.usecase

import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateHabitUseCaseTest {
    private val habitRepository = mockk<HabitRepository>()
    private val useCase = CreateHabitUseCase(habitRepository)

    @Test
    fun `creates habit with valid title`() = runTest {
        val habit = Habit(title = "Meditate", tinyVersion = "Sit, take 3 breaths")
        coEvery { habitRepository.createHabit(any()) } returns Result.success(habit)

        val result = useCase(habit)

        assertTrue(result.isSuccess)
        assertEquals("Meditate", result.getOrThrow().title)
        assertEquals("Sit, take 3 breaths", result.getOrThrow().tinyVersion)
        coVerify { habitRepository.createHabit(habit) }
    }

    @Test
    fun `sets correct repeat after for daily frequency`() = runTest {
        val habit = Habit(title = "Exercise", frequency = HabitFrequency.DAILY)
        coEvery { habitRepository.createHabit(any()) } returns Result.success(habit)

        val result = useCase(habit)

        assertTrue(result.isSuccess)
        assertEquals(86400L, result.getOrThrow().frequency.repeatAfterSeconds)
    }

    @Test
    fun `rejects blank title`() = runTest {
        assertThrows<IllegalArgumentException> {
            useCase(Habit(title = ""))
        }
    }
}
