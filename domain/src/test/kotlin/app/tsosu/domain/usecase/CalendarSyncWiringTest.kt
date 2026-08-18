package app.tsosu.domain.usecase

import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.CalendarRepository
import app.tsosu.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CalendarSyncWiringTest {

    private val taskRepository = mockk<TaskRepository>()
    private val calendarRepository = mockk<CalendarRepository>(relaxed = true)

    private fun configured(enabled: Boolean = true) {
        every { calendarRepository.isConfigured() } returns flowOf(enabled)
    }

    private fun coordinator() = TaskCalendarCoordinator(taskRepository, calendarRepository)

    private val datedTask = Task(
        id = "t1",
        title = "Buy milk",
        dueDate = LocalDateTime(2026, 8, 20, 9, 0),
    )

    @Test
    fun `syncTask stores returned event id`() = runTest {
        configured()
        coEvery { calendarRepository.syncTaskToCalendar(datedTask) } returns Result.success("tsosu-t1")
        coEvery { taskRepository.updateTask(any()) } answers { Result.success(firstArg()) }

        val result = coordinator().syncTask(datedTask)

        assertEquals("tsosu-t1", result.calendarEventId)
        coVerify { taskRepository.updateTask(datedTask.copy(calendarEventId = "tsosu-t1")) }
    }

    @Test
    fun `syncTask no-ops when no calendar configured`() = runTest {
        configured(enabled = false)

        val result = coordinator().syncTask(datedTask)

        assertEquals(datedTask, result)
        coVerify(exactly = 0) { calendarRepository.syncTaskToCalendar(any()) }
    }

    @Test
    fun `syncTask removes event when the due date is cleared`() = runTest {
        configured()
        val undated = datedTask.copy(dueDate = null, calendarEventId = "tsosu-t1")
        coEvery { taskRepository.updateTask(any()) } answers { Result.success(firstArg()) }

        val result = coordinator().syncTask(undated)

        assertNull(result.calendarEventId)
        coVerify { calendarRepository.removeCalendarEvent("tsosu-t1") }
    }

    @Test
    fun `syncTask survives calendar failure`() = runTest {
        configured()
        coEvery { calendarRepository.syncTaskToCalendar(datedTask) } returns
            Result.failure(Exception("server down"))

        val result = coordinator().syncTask(datedTask)

        assertEquals(datedTask, result)
    }

    @Test
    fun `removeEvent falls back to deterministic tsosu id`() = runTest {
        configured()

        coordinator().removeEvent(datedTask)

        coVerify { calendarRepository.removeCalendarEvent("tsosu-t1") }
    }

    @Test
    fun `toggling to done removes the calendar event`() = runTest {
        configured()
        val done = datedTask.copy(status = TaskStatus.DONE)
        coEvery { taskRepository.toggleDone("t1") } returns Result.success(done)

        ToggleTaskDoneUseCase(taskRepository, coordinator()).invoke("t1")

        coVerify { calendarRepository.removeCalendarEvent(any()) }
        coVerify(exactly = 0) { calendarRepository.syncTaskToCalendar(any()) }
    }

    @Test
    fun `toggling a recurring reset re-syncs the next occurrence`() = runTest {
        configured()
        val reset = datedTask.copy(status = TaskStatus.TODO, recurrenceRule = "RRULE:FREQ=DAILY")
        coEvery { taskRepository.toggleDone("t1") } returns Result.success(reset)
        coEvery { calendarRepository.syncTaskToCalendar(reset) } returns Result.success("tsosu-t1")
        coEvery { taskRepository.updateTask(any()) } answers { Result.success(firstArg()) }

        ToggleTaskDoneUseCase(taskRepository, coordinator()).invoke("t1")

        coVerify { calendarRepository.syncTaskToCalendar(reset) }
    }

    @Test
    fun `creating a dated task pushes it to the calendar`() = runTest {
        configured()
        coEvery { taskRepository.createTask(datedTask) } returns Result.success(datedTask)
        coEvery { calendarRepository.syncTaskToCalendar(datedTask) } returns Result.success("tsosu-t1")
        coEvery { taskRepository.updateTask(any()) } answers { Result.success(firstArg()) }

        CreateTaskUseCase(taskRepository, coordinator()).invoke(datedTask)

        coVerify { calendarRepository.syncTaskToCalendar(datedTask) }
    }

    @Test
    fun `deleting a task removes its event`() = runTest {
        configured()
        val withEvent = datedTask.copy(calendarEventId = "tsosu-t1")
        coEvery { taskRepository.getTask("t1") } returns flowOf(withEvent)
        coEvery { taskRepository.deleteTask("t1") } returns Result.success(Unit)

        DeleteTaskUseCase(taskRepository, coordinator()).invoke("t1")

        coVerify { calendarRepository.removeCalendarEvent("tsosu-t1") }
    }

    @Test
    fun `updating a task re-syncs its event`() = runTest {
        configured()
        coEvery { taskRepository.updateTask(datedTask) } returns Result.success(datedTask)
        coEvery { calendarRepository.syncTaskToCalendar(datedTask) } returns Result.success("tsosu-t1")
        coEvery { taskRepository.updateTask(any()) } answers { Result.success(firstArg()) }

        UpdateTaskUseCase(taskRepository, coordinator()).invoke(datedTask)

        coVerify(atLeast = 1) { calendarRepository.syncTaskToCalendar(datedTask) }
    }
}
