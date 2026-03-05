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

class CreateTaskUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val useCase = CreateTaskUseCase(taskRepository)

    @Test
    fun `creates task with valid title`() = runTest {
        val task = Task(title = "Buy groceries")
        coEvery { taskRepository.createTask(any()) } returns Result.success(task)

        val result = useCase(task)

        assertTrue(result.isSuccess)
        assertEquals("Buy groceries", result.getOrThrow().title)
        coVerify { taskRepository.createTask(task) }
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
