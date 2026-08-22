package app.tsosu.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.FilterSpec
import app.tsosu.domain.model.SortSpec
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.repository.FocusRepository
import app.tsosu.domain.usecase.GetTodayOverviewUseCase
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.PomodoroEngine
import app.tsosu.domain.usecase.SetDailyFocusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val focusTasks: List<Task> = emptyList(),
    val otherTasks: List<Task> = emptyList(),
    val inboxTasks: List<Task> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val totalEstimatedMinutes: Int = 0,
    val isFiltered: Boolean = false,
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    getTodayOverview: GetTodayOverviewUseCase,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val setTaskStatus: SetTaskStatusUseCase,
    private val taskRepository: TaskRepository,
    private val focusRepository: FocusRepository,
    private val setDailyFocus: SetDailyFocusUseCase,
) : ViewModel() {

    private val _filterSpec = MutableStateFlow(FilterSpec())
    val filterSpec: StateFlow<FilterSpec> = _filterSpec.asStateFlow()

    // ── Pomodoro ──

    private val _pomodoro = MutableStateFlow(PomodoroEngine.State())
    val pomodoro: StateFlow<PomodoroEngine.State> = _pomodoro.asStateFlow()

    private val _pomodoroTaskId = MutableStateFlow<String?>(null)
    val pomodoroTaskId: StateFlow<String?> = _pomodoroTaskId.asStateFlow()

    private var tickJob: Job? = null

    fun onPomodoroPresetSelected(preset: PomodoroEngine.Preset) {
        _pomodoro.value = PomodoroEngine.State(preset = preset)
        stopTicking()
    }

    fun onPomodoroTaskSelected(taskId: String?) {
        _pomodoroTaskId.value = taskId
    }

    fun startPomodoro() {
        if (_pomodoro.value.isRunning) return
        val next = when (_pomodoro.value.phase) {
            PomodoroEngine.Phase.FINISHED_WORK -> PomodoroEngine.startBreak(_pomodoro.value)
            PomodoroEngine.Phase.FINISHED_BREAK -> PomodoroEngine.startWork(_pomodoro.value)
            else -> PomodoroEngine.start(_pomodoro.value)
        }
        _pomodoro.value = next
        stopTicking()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val state = PomodoroEngine.tick(_pomodoro.value)
                _pomodoro.value = state
                if (state.phase == PomodoroEngine.Phase.FINISHED_WORK) {
                    // Log the focused minutes onto the selected task.
                    _pomodoroTaskId.value?.let { id ->
                        taskRepository.addTimeSpent(id, _pomodoro.value.preset.workMinutes)
                    }
                    break
                }
                if (state.phase == PomodoroEngine.Phase.FINISHED_BREAK) break
            }
        }
    }

    fun resetPomodoro() {
        stopTicking()
        _pomodoro.value = PomodoroEngine.reset()
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private val _sortSpec = MutableStateFlow(SortSpec())
    val sortSpec: StateFlow<SortSpec> = _sortSpec.asStateFlow()

    val uiState: StateFlow<FocusUiState> = combine(
        getTodayOverview(),
        taskRepository.getInboxTasks(),
        focusRepository.getDailyFocus(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        ),
        _filterSpec,
        _sortSpec,
    ) { overview, inbox, dailyFocus, filter, sort ->
        val isFiltered = filter != FilterSpec()
        val focusIds = dailyFocus?.taskIds?.toSet() ?: emptySet()

        val focusRaw = overview.tasks.filter { it.id in focusIds && !it.done }
        val otherRaw = overview.tasks.filter { it.id !in focusIds && !it.done }
        val inboxRaw = inbox.filter { !it.done }

        FocusUiState(
            focusTasks = sort.apply(filter.apply(focusRaw)),
            otherTasks = sort.apply(filter.apply(otherRaw)),
            inboxTasks = sort.apply(filter.apply(inboxRaw)),
            completedCount = overview.tasks.count { it.done },
            totalCount = overview.tasks.size,
            totalEstimatedMinutes = overview.totalEstimatedMinutes,
            isFiltered = isFiltered,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusUiState())

    /** Adds a task to today's Focus 3 (kept to at most 3 by the use case contract). */
    fun setFocusToday(taskId: String) {
        viewModelScope.launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val current = focusRepository.getDailyFocus(today).first()?.taskIds.orEmpty()
            if (taskId in current) return@launch
            setDailyFocus(today, current + taskId)
        }
    }

    fun applyFilter(filter: FilterSpec, sort: SortSpec) {
        _filterSpec.value = filter
        _sortSpec.value = sort
    }

    fun clearFilter() {
        _filterSpec.value = FilterSpec()
        _sortSpec.value = SortSpec()
    }

    fun onToggleDone(taskId: String) {
        viewModelScope.launch {
            toggleTaskDone(taskId).getOrNull()?.let { reminderScheduler.schedule(it) }
        }
    }

    fun setStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            setTaskStatus(taskId, status)
        }
    }

    fun postponeTask(taskId: String) {
        viewModelScope.launch {
            val tz = TimeZone.currentSystemDefault()
            val tomorrow = Clock.System.now().toLocalDateTime(tz).date
                .plus(1, DateTimeUnit.DAY)
            val task = taskRepository.getTask(taskId).first()
                ?: return@launch
            taskRepository.updateTask(task.copy(dueDate = tomorrow.atTime(9, 0)))
        }
    }
}
