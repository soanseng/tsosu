package app.tsosu.ui.screens.inbox

import app.tsosu.domain.model.Project
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.GetStaleTaskIdsUseCase
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelGroupTest {

    private val dispatcher = StandardTestDispatcher()
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val projectRepository = mockk<ProjectRepository>(relaxed = true)
    private val toggleDone = mockk<ToggleTaskDoneUseCase>()
    private val scheduler = mockk<ReminderScheduler>(relaxed = true)
    private val setStatus = mockk<SetTaskStatusUseCase>()
    private val staleIds = mockk<GetStaleTaskIdsUseCase>()

    private fun task(id: String, projectId: String? = null) =
        Task(id = id, title = "t-$id", projectId = projectId)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { taskRepository.getInboxTasks() } returns MutableStateFlow(emptyList())
        every { projectRepository.getAllProjects() } returns MutableStateFlow(emptyList())
        every { staleIds(any()) } returns MutableStateFlow(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `undated tasks stay in the inbox, folded under their category`() =
        runTest(dispatcher) {
            every { taskRepository.getInboxTasks() } returns MutableStateFlow(
                listOf(
                    task("a"),
                    task("b", projectId = "deleted-project"),
                    task("c", projectId = "p1"),
                    task("d", projectId = "p1"),
                ),
            )
            every { projectRepository.getAllProjects() } returns MutableStateFlow(
                listOf(Project(id = "p1", title = "論文"), Project(id = "p2", title = "空")),
            )

            val viewModel = InboxViewModel(taskRepository, projectRepository, toggleDone, scheduler, setStatus, staleIds)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val groups = viewModel.uiState.value
            // Loose first — including a task whose category vanished.
            assertEquals(listOf("a", "b"), groups.uncategorized.map { it.id })
            assertEquals(listOf("p1"), groups.categories.map { it.project.id })
            assertEquals(listOf("c", "d"), groups.categories.single().tasks.map { it.id })

            job.cancel()
        }
}
