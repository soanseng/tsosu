package app.tsosu.domain.usecase

import app.tsosu.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class DeleteTaskUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val useCase = DeleteTaskUseCase(taskRepository)

    @Test
    fun `deletes task by id`() = runTest {
        coEvery { taskRepository.deleteTask(any()) } returns Result.success(Unit)

        val result = useCase("task-123")

        assertTrue(result.isSuccess)
        coVerify { taskRepository.deleteTask("task-123") }
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val error = RuntimeException("not found")
        coEvery { taskRepository.deleteTask(any()) } returns Result.failure(error)

        val result = useCase("task-999")

        assertTrue(result.isFailure)
    }
}
