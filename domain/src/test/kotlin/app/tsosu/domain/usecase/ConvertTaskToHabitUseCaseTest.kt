package app.tsosu.domain.usecase

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConvertTaskToHabitUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val habitRepository = mockk<HabitRepository>()
    private val useCase = ConvertTaskToHabitUseCase(taskRepository, habitRepository)

    @Test
    fun `converts task into habit and deletes the task`() = runTest {
        val task = Task(
            id = "task-1",
            title = "Meditate",
            description = "Sit, take 3 breaths",
            energyLevel = EnergyLevel.LOW,
        )
        val habit = Habit(
            title = task.title,
            tinyVersion = task.description,
            energyLevel = task.energyLevel,
        )
        every { taskRepository.getTask("task-1") } returns flowOf(task)
        coEvery { habitRepository.createHabit(any()) } returns Result.success(habit)
        coEvery { taskRepository.deleteTask("task-1") } returns Result.success(Unit)

        val result = useCase("task-1")

        assertTrue(result.isSuccess)
        assertEquals("Meditate", result.getOrThrow().title)
        assertEquals("Sit, take 3 breaths", result.getOrThrow().tinyVersion)
        assertEquals(EnergyLevel.LOW, result.getOrThrow().energyLevel)
        coVerify {
            habitRepository.createHabit(
                match { created ->
                    created.title == "Meditate" &&
                        created.tinyVersion == "Sit, take 3 breaths" &&
                        created.energyLevel == EnergyLevel.LOW
                },
            )
            taskRepository.deleteTask("task-1")
        }
    }

    @Test
    fun `fails when task is missing`() = runTest {
        every { taskRepository.getTask("missing") } returns flowOf(null)

        val result = useCase("missing")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { habitRepository.createHabit(any()) }
        coVerify(exactly = 0) { taskRepository.deleteTask(any()) }
    }

    @Test
    fun `rejects blank title`() = runTest {
        every { taskRepository.getTask("blank") } returns flowOf(Task(id = "blank", title = "  "))

        assertThrows<IllegalArgumentException> {
            useCase("blank")
        }
        coVerify(exactly = 0) { habitRepository.createHabit(any()) }
    }

    @Test
    fun `does not delete task when habit create fails`() = runTest {
        val task = Task(id = "task-2", title = "Walk")
        every { taskRepository.getTask("task-2") } returns flowOf(task)
        coEvery { habitRepository.createHabit(any()) } returns Result.failure(RuntimeException("vault"))

        val result = useCase("task-2")

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { taskRepository.deleteTask(any()) }
    }
}
