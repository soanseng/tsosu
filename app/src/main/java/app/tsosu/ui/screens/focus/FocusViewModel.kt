package app.tsosu.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.GetTodayOverviewUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    getTodayOverview: GetTodayOverviewUseCase,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    taskRepository: TaskRepository,
) : ViewModel() {

    val uiState: StateFlow<FocusUiState> = combine(
        getTodayOverview(),
        taskRepository.getInboxTasks(),
    ) { overview, inbox ->
        FocusUiState(
            focusTasks = overview.tasks.filter { it.isFocus && !it.done },
            otherTasks = overview.tasks.filter { !it.isFocus && !it.done },
            inboxTasks = inbox.filter { !it.done },
            completedCount = overview.tasks.count { it.done },
            totalCount = overview.tasks.size,
            totalEstimatedMinutes = overview.totalEstimatedMinutes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusUiState())

    fun onToggleDone(taskId: String) {
        viewModelScope.launch {
            toggleTaskDone(taskId)
        }
    }
}
