package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetTodayOverviewUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val useCase = GetTodayOverviewUseCase(taskRepository)

    @Test
    fun `returns tasks with time total and focus count`() = runTest {
        val tasks = listOf(
            Task(title = "A", estimatedMinutes = 30, isFocus = true),
            Task(title = "B", estimatedMinutes = 15, isFocus = true),
            Task(title = "C", estimatedMinutes = null, isFocus = false),
        )
        every { taskRepository.getTodayTasks() } returns flowOf(tasks)

        useCase().test {
            val overview = awaitItem()
            assertEquals(3, overview.tasks.size)
            assertEquals(45, overview.totalEstimatedMinutes)
            assertEquals(2, overview.focusCount)
            awaitComplete()
        }
    }

    @Test
    fun `handles empty task list`() = runTest {
        every { taskRepository.getTodayTasks() } returns flowOf(emptyList())

        useCase().test {
            val overview = awaitItem()
            assertEquals(0, overview.tasks.size)
            assertEquals(0, overview.totalEstimatedMinutes)
            assertEquals(0, overview.focusCount)
            awaitComplete()
        }
    }
}
