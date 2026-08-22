package app.tsosu.ui.screens.inbox

import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.GetStaleTaskIdsUseCase
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import io.mockk.coEvery
import io.mockk.every
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelBulkTest {

    private val dispatcher = StandardTestDispatcher()
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val toggleDone = mockk<ToggleTaskDoneUseCase>()
    private val setStatus = mockk<SetTaskStatusUseCase>()
    private val scheduler = mockk<ReminderScheduler>(relaxed = true)
    private val staleIds = mockk<GetStaleTaskIdsUseCase>()

    private fun task(id: String) = Task(id = id, title = "t-$id")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { taskRepository.getInboxTasks() } returns MutableStateFlow(emptyList())
        every { staleIds(any()) } returns MutableStateFlow(emptyList())
        coEvery { setStatus(any(), any()) } answers {
            Result.success(task(firstArg()))
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleSelection adds then removes`() = runTest(dispatcher) {
        val vm = InboxViewModel(taskRepository, toggleDone, scheduler, setStatus, staleIds)
        vm.toggleSelection("a")
        vm.toggleSelection("b")
        assertEquals(setOf("a", "b"), vm.selectedIds.value)
        vm.toggleSelection("a")
        assertEquals(setOf("b"), vm.selectedIds.value)
    }

    @Test
    fun `bulkComplete completes each selected task and clears selection`() = runTest(dispatcher) {
        val vm = InboxViewModel(taskRepository, toggleDone, scheduler, setStatus, staleIds)
        vm.toggleSelection("a")
        vm.toggleSelection("b")
        vm.bulkComplete()
        dispatcher.scheduler.advanceUntilIdle()
        coVerify {
            setStatus("a", TaskStatus.DONE)
            setStatus("b", TaskStatus.DONE)
        }
        assertTrue(vm.selectedIds.value.isEmpty())
    }

    @Test
    fun `bulkSomeday parks each selected task`() = runTest(dispatcher) {
        val vm = InboxViewModel(taskRepository, toggleDone, scheduler, setStatus, staleIds)
        vm.toggleSelection("a")
        vm.bulkSomeday()
        dispatcher.scheduler.advanceUntilIdle()
        coVerify { setStatus("a", TaskStatus.PLANNED) }
        assertTrue(vm.selectedIds.value.isEmpty())
    }

    @Test
    fun `bulkDelete deletes and cancels alarms`() = runTest(dispatcher) {
        val vm = InboxViewModel(taskRepository, toggleDone, scheduler, setStatus, staleIds)
        coEvery { taskRepository.deleteTask(any()) } returns Result.success(Unit)
        vm.toggleSelection("x")
        vm.bulkDelete()
        dispatcher.scheduler.advanceUntilIdle()
        coVerify { taskRepository.deleteTask("x") }
        coVerify { scheduler.cancel("x") }
    }


}
