package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class ToggleTaskDoneUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val useCase = ToggleTaskDoneUseCase(taskRepository)

    @Test
    fun `delegates to repository`() = runTest {
        val task = Task(title = "Test", status = TaskStatus.DONE)
        coEvery { taskRepository.toggleDone("task-1") } returns Result.success(task)

        val result = useCase("task-1")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().done)
        coVerify { taskRepository.toggleDone("task-1") }
    }
}
