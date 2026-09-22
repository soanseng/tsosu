package app.tsosu.ui.screens.quickadd

import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Project
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.usecase.CreateTaskUseCase
import app.tsosu.notification.ReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class QuickAddViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val createTaskUseCase = mockk<CreateTaskUseCase>()
    private val projectRepository = mockk<ProjectRepository>(relaxed = true)
    private val scheduler = mockk<ReminderScheduler>(relaxed = true)

    private val createdTask = slot<Task>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { createTaskUseCase(capture(createdTask)) } answers {
            Result.success(createdTask.captured)
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `typing a new category creates it and files the task there`() = runTest(dispatcher) {
        val papers = Project(id = "p1", title = "論文")
        every { projectRepository.getAllProjects() } returns MutableStateFlow(listOf(papers))
        val createdProject = slot<Project>()
        coEvery { projectRepository.createProject(capture(createdProject)) } answers {
            Result.success(createdProject.captured)
        }

        val viewModel = QuickAddViewModel(createTaskUseCase, projectRepository, scheduler)
        viewModel.createTask(
            title = "讀 paper",
            priority = Priority.NONE,
            dueDate = null,
            newCategoryName = "自我成長",
        )
        advanceUntilIdle()

        assertEquals("自我成長", createdProject.captured.title)
        assertEquals(createdProject.captured.id, createdTask.captured.projectId)
    }

    @Test
    fun `an existing category is reused without creating a duplicate`() = runTest(dispatcher) {
        val papers = Project(id = "p1", title = "論文")
        every { projectRepository.getAllProjects() } returns MutableStateFlow(listOf(papers))

        val viewModel = QuickAddViewModel(createTaskUseCase, projectRepository, scheduler)
        viewModel.createTask(
            title = "買牛奶",
            priority = Priority.NONE,
            dueDate = null,
            newCategoryName = "論文",
        )
        advanceUntilIdle()

        assertEquals("p1", createdTask.captured.projectId)
        coVerify(exactly = 0) { projectRepository.createProject(any()) }
    }

    @Test
    fun `an explicitly picked category wins over a typed token`() = runTest(dispatcher) {
        every { projectRepository.getAllProjects() } returns MutableStateFlow(
            listOf(Project(id = "p1", title = "論文")),
        )

        val viewModel = QuickAddViewModel(createTaskUseCase, projectRepository, scheduler)
        viewModel.createTask(
            title = "買牛奶",
            priority = Priority.NONE,
            dueDate = null,
            projectName = "論文",
            projectId = "p2",
        )
        advanceUntilIdle()

        assertEquals("p2", createdTask.captured.projectId)
        coVerify(exactly = 0) { projectRepository.createProject(any()) }
    }
}
