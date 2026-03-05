package app.tsosu.domain.usecase

import app.tsosu.domain.repository.TaskRepository
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetStaleTaskIdsUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val useCase = GetStaleTaskIdsUseCase(taskRepository)

    @Test
    fun `returns stale task ids with default 14 days`() = runTest {
        val staleIds = listOf("old-1", "old-2")
        every { taskRepository.getStaleTaskIds(14) } returns flowOf(staleIds)

        useCase().test {
            assertEquals(listOf("old-1", "old-2"), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `supports custom threshold`() = runTest {
        every { taskRepository.getStaleTaskIds(30) } returns flowOf(listOf("ancient"))

        useCase(30).test {
            assertEquals(listOf("ancient"), awaitItem())
            awaitComplete()
        }
    }
}
