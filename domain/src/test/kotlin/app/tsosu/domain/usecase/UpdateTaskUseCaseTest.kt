package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UpdateTaskUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val useCase = UpdateTaskUseCase(taskRepository)

    @Test
    fun `updates task with valid title`() = runTest {
        val task = Task(title = "Updated title")
        coEvery { taskRepository.updateTask(any()) } returns Result.success(task)

        val result = useCase(task)

        assertTrue(result.isSuccess)
        assertEquals("Updated title", result.getOrThrow().title)
        coVerify { taskRepository.updateTask(task) }
    }

    @Test
    fun `rejects blank title`() = runTest {
        val task = Task(title = "   ")

        assertThrows<IllegalArgumentException> {
            useCase(task)
        }
    }

    @Test
    fun `rejects empty title`() = runTest {
        val task = Task(title = "")

        assertThrows<IllegalArgumentException> {
            useCase(task)
        }
    }
}
