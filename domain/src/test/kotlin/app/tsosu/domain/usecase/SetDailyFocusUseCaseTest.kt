package app.tsosu.domain.usecase

import app.tsosu.domain.model.DailyFocus
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.FocusRepository
import app.tsosu.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetDailyFocusUseCaseTest {
    private val focusRepository = mockk<FocusRepository>()
    private val taskRepository = mockk<TaskRepository>()
    private val useCase = SetDailyFocusUseCase(focusRepository, taskRepository)
    private val today = LocalDate(2026, 3, 5)

    @Test
    fun `sets focus for up to 3 tasks`() = runTest {
        val taskIds = listOf("t1", "t2", "t3")
        val focus = DailyFocus(today, taskIds)
        coEvery { taskRepository.setFocus(any(), true) } returns Result.success(Task(title = "x"))
        coEvery { focusRepository.setDailyFocus(today, taskIds) } returns Result.success(focus)

        val result = useCase(today, taskIds)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().taskIds.size)
        coVerify(exactly = 3) { taskRepository.setFocus(any(), true) }
    }

    @Test
    fun `rejects more than 3 tasks`() = runTest {
        assertThrows<IllegalArgumentException> {
            useCase(today, listOf("t1", "t2", "t3", "t4"))
        }
    }

    @Test
    fun `rejects empty task list`() = runTest {
        assertThrows<IllegalArgumentException> {
            useCase(today, emptyList())
        }
    }
}
