package app.tsosu.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.GetStaleTaskIdsUseCase
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val setTaskStatus: SetTaskStatusUseCase,
    getStaleTaskIds: GetStaleTaskIdsUseCase,
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = taskRepository.getInboxTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Tasks untouched for 2+ weeks — surfaced as a gentle clean-up suggestion, never forced. */
    val staleIds: StateFlow<List<String>> = getStaleTaskIds(olderThanDays = 14)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cleanUpStale() {
        viewModelScope.launch {
            staleIds.value.forEach { taskRepository.deleteTask(it) }
        }
    }

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
