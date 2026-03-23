package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.IcsExporter
import app.tsosu.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportIcsUseCaseTest {

    private val taskRepository = mockk<TaskRepository>()
    private val icsExporter = mockk<IcsExporter>()
    private val useCase = ExportIcsUseCase(taskRepository, icsExporter)

    @Test
    fun `exports tasks with due dates as ICS`() = runTest {
        val tasks = listOf(
            Task(
                id = "task-1",
                title = "Buy groceries",
                dueDate = LocalDateTime(2026, 3, 10, 9, 0),
            ),
            Task(
                id = "task-2",
                title = "Meeting",
                dueDate = LocalDateTime(2026, 3, 11, 14, 0),
            ),
        )
        coEvery { taskRepository.getUpcomingTasks(365) } returns flowOf(tasks)
        every { icsExporter.exportTasks(tasks) } returns "BEGIN:VCALENDAR\nEND:VCALENDAR"

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals("BEGIN:VCALENDAR\nEND:VCALENDAR", result.getOrThrow())
        verify { icsExporter.exportTasks(tasks) }
    }

    @Test
    fun `filters out tasks without due dates`() = runTest {
        val taskWithDue = Task(
            id = "task-1",
            title = "Has due date",
            dueDate = LocalDateTime(2026, 3, 10, 9, 0),
        )
        val taskWithoutDue = Task(
            id = "task-2",
            title = "No due date",
            dueDate = null,
        )
        coEvery { taskRepository.getUpcomingTasks(365) } returns flowOf(
            listOf(taskWithDue, taskWithoutDue),
        )
        every { icsExporter.exportTasks(listOf(taskWithDue)) } returns "ICS_CONTENT"

        val result = useCase()

        assertTrue(result.isSuccess)
        verify { icsExporter.exportTasks(listOf(taskWithDue)) }
    }

    @Test
    fun `returns empty string when no tasks have due dates`() = runTest {
        val tasks = listOf(
            Task(id = "task-1", title = "No due date", dueDate = null),
        )
        coEvery { taskRepository.getUpcomingTasks(365) } returns flowOf(tasks)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals("", result.getOrThrow())
    }

    @Test
    fun `returns failure on exception`() = runTest {
        coEvery { taskRepository.getUpcomingTasks(365) } throws RuntimeException("DB error")

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("DB error", result.exceptionOrNull()?.message)
    }
}
