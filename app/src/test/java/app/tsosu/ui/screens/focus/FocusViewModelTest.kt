package app.tsosu.ui.screens.focus

import app.tsosu.domain.model.DailyFocus
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.FocusRepository
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.GetTodayOverviewUseCase
import app.tsosu.domain.usecase.SetDailyFocusUseCase
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.TodayOverview
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModelTest {

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val taskA = Task(id = "a", title = "A")
    private val taskB = Task(id = "b", title = "B")

    private val taskRepository = mockk<TaskRepository> {
        every { getInboxTasks() } returns flowOf(emptyList())
    }
    private val focusRepository = mockk<FocusRepository>()
    private val setDailyFocus = mockk<SetDailyFocusUseCase>()

    private fun overviewFlow(tasks: List<Task>) = mockk<GetTodayOverviewUseCase> {
        every { this@mockk() } returns flowOf(
            TodayOverview(tasks = tasks, totalEstimatedMinutes = 0, focusCount = 0),
        )
    }

    private fun viewModel(
        overview: GetTodayOverviewUseCase = overviewFlow(listOf(taskA, taskB)),
        dailyFocus: DailyFocus? = null,
    ): FocusViewModel {
        every { focusRepository.getDailyFocus(any()) } returns flowOf(dailyFocus)
        coEvery { setDailyFocus(any(), any()) } returns
            Result.success(DailyFocus(today, emptyList()))
        return FocusViewModel(
            getTodayOverview = overview,
            toggleTaskDone = mockk(relaxed = true),
            reminderScheduler = mockk(relaxed = true),
            setTaskStatus = mockk(relaxed = true),
            taskRepository = taskRepository,
            focusRepository = focusRepository,
            setDailyFocus = setDailyFocus,
        )
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `focus section comes from daily focus table not task isFocus`() = runTest {
        // taskB carries isFocus=true but is NOT in today's daily focus -> must land in "other".
        val vm = viewModel(
            overview = overviewFlow(listOf(taskA, taskB.copy(isFocus = true))),
            dailyFocus = DailyFocus(today, listOf("a")),
        )

        val states = mutableListOf<FocusUiState>()
        backgroundScope.launch { vm.uiState.collect { states.add(it) } }
        advanceUntilIdle()

        val populated = states.lastOrNull { it.focusTasks.isNotEmpty() || it.otherTasks.isNotEmpty() }
        assertEquals(listOf("a"), populated?.focusTasks?.map { it.id })
        assertEquals(true, populated?.otherTasks?.any { it.id == "b" })
    }

    @Test
    fun `setFocusToday appends to current daily focus`() = runTest {
        val vm = viewModel(dailyFocus = DailyFocus(today, listOf("a")))
        vm.setFocusToday("b")
        advanceUntilIdle()
        coVerify(exactly = 1) { setDailyFocus(today, listOf("a", "b")) }
    }

    @Test
    fun `setFocusToday ignores already focused task`() = runTest {
        val vm = viewModel(dailyFocus = DailyFocus(today, listOf("a")))
        vm.setFocusToday("a")
        advanceUntilIdle()
        coVerify(exactly = 0) { setDailyFocus(any(), any()) }
    }
}
