package app.tsosu.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.FilterSpec
import app.tsosu.domain.model.SortSpec
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.GetTodayOverviewUseCase
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val setTaskStatus: SetTaskStatusUseCase,
    taskRepository: TaskRepository,
) : ViewModel() {

    private val _filterSpec = MutableStateFlow(FilterSpec())
    val filterSpec: StateFlow<FilterSpec> = _filterSpec.asStateFlow()

    private val _sortSpec = MutableStateFlow(SortSpec())
    val sortSpec: StateFlow<SortSpec> = _sortSpec.asStateFlow()

    val uiState: StateFlow<FocusUiState> = combine(
        getTodayOverview(),
        taskRepository.getInboxTasks(),
        _filterSpec,
        _sortSpec,
    ) { overview, inbox, filter, sort ->
        val isFiltered = filter != FilterSpec()

        val focusRaw = overview.tasks.filter { it.isFocus && !it.done }
        val otherRaw = overview.tasks.filter { !it.isFocus && !it.done }
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
            toggleTaskDone(taskId)
        }
    }

    fun setStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            setTaskStatus(taskId, status)
        }
    }
}
