package app.tsosu.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayUiState(
    val overdue: List<Task> = emptyList(),
    val today: List<Task> = emptyList(),
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    taskRepository: TaskRepository,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val setTaskStatus: SetTaskStatusUseCase,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        taskRepository.getOverdueTasks(),
        taskRepository.getTodayTasks(),
    ) { overdue, today ->
        TodayUiState(overdue = overdue, today = today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    val pendingCount: StateFlow<Int> = uiState
        .map { it.overdue.size + it.today.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleDone(taskId: String) {
        viewModelScope.launch {
            toggleTaskDone(taskId).getOrNull()?.let { reminderScheduler.schedule(it) }
        }
    }

    fun setStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            setTaskStatus(taskId, status)
        }
    }
}
