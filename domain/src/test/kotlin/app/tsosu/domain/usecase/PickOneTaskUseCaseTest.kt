package app.tsosu.domain.usecase

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PickOneTaskUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val useCase = PickOneTaskUseCase(taskRepository)

    @Test
    fun `picks undone task matching energy level`() = runTest {
        val tasks = listOf(
            Task(title = "A", status = TaskStatus.TODO, energyLevel = EnergyLevel.LOW),
            Task(title = "B", status = TaskStatus.DONE, energyLevel = EnergyLevel.LOW),
        )
        every { taskRepository.getTasksByEnergy(EnergyLevel.LOW) } returns flowOf(tasks)

        useCase(EnergyLevel.LOW).test {
            val picked = awaitItem()
            assertNotNull(picked)
            assertTrue(!picked.done)
            awaitComplete()
        }
    }

    @Test
    fun `returns null when all tasks done`() = runTest {
        val tasks = listOf(
            Task(title = "A", status = TaskStatus.DONE, energyLevel = EnergyLevel.HIGH),
        )
        every { taskRepository.getTasksByEnergy(EnergyLevel.HIGH) } returns flowOf(tasks)

        useCase(EnergyLevel.HIGH).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns null when no tasks`() = runTest {
        every { taskRepository.getTasksByEnergy(EnergyLevel.MEDIUM) } returns flowOf(emptyList())

        useCase(EnergyLevel.MEDIUM).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }
}
