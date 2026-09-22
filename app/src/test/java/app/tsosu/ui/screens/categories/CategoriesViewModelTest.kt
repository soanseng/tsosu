package app.tsosu.ui.screens.categories

import app.tsosu.domain.model.Project
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.repository.TaskRepository
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
class CategoriesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val projectRepository = mockk<ProjectRepository>(relaxed = true)
    private val toggleDone = mockk<ToggleTaskDoneUseCase>(relaxed = true)
    private val scheduler = mockk<ReminderScheduler>(relaxed = true)

    private fun task(id: String, projectId: String?) =
        Task(id = id, title = "t-$id", projectId = projectId)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tasks group under their category and unknown ids fall to uncategorized`() =
        runTest(dispatcher) {
            val papers = Project(id = "p1", title = "論文")
            every { projectRepository.getAllProjects() } returns MutableStateFlow(listOf(papers))
            every { taskRepository.getAllActiveTasks() } returns MutableStateFlow(
                listOf(
                    task("a", "p1"),
                    task("b", "p1"),
                    task("c", "deleted-project"),
                    task("d", null),
                ),
            )

            val viewModel = CategoriesViewModel(taskRepository, projectRepository, toggleDone, scheduler)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val groups = viewModel.uiState.value.groups
            assertEquals("p1", groups.first().project?.id)
            assertEquals(listOf("a", "b"), groups.first().tasks.map { it.id })
            assertEquals(null, groups.last().project)
            assertEquals(setOf("c", "d"), groups.last().tasks.map { it.id }.toSet())
            assertEquals(4, viewModel.uiState.value.taskCount)

            job.cancel()
        }

    @Test
    fun `an empty category is still listed and omitted when there are no tasks`() =
        runTest(dispatcher) {
            every { projectRepository.getAllProjects() } returns MutableStateFlow(
                listOf(Project(id = "p1", title = "論文"), Project(id = "p2", title = "自我成長")),
            )
            every { taskRepository.getAllActiveTasks() } returns MutableStateFlow(
                listOf(task("a", "p2")),
            )

            val viewModel = CategoriesViewModel(taskRepository, projectRepository, toggleDone, scheduler)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            val groups = viewModel.uiState.value.groups
            assertEquals(listOf("p1", "p2"), groups.map { it.project?.id })
            assertEquals(emptyList<String>(), groups.first().tasks.map { it.id })
            assertEquals(listOf("a"), groups.last().tasks.map { it.id })

            job.cancel()
        }
}
