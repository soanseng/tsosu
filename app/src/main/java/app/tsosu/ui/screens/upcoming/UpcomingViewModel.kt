package app.tsosu.ui.screens.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class DateGroup(
    val label: String,
    val tasks: List<Task>,
)

data class UpcomingUiState(
    val groups: List<DateGroup> = emptyList(),
)

@HiltViewModel
class UpcomingViewModel @Inject constructor(
    taskRepository: TaskRepository,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val setTaskStatus: SetTaskStatusUseCase,
) : ViewModel() {

    val uiState: StateFlow<UpcomingUiState> = taskRepository.getUpcomingTasks()
        .map { tasks ->
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val today = now.date
            val tomorrow = today.plus(1, DateTimeUnit.DAY)
            val weekEnd = today.plus(7, DateTimeUnit.DAY)

            val todayTasks = tasks.filter { it.dueDate?.date == today }
            val tomorrowTasks = tasks.filter { it.dueDate?.date == tomorrow }
            val thisWeekTasks = tasks.filter { task ->
                val d = task.dueDate?.date ?: return@filter false
                d > tomorrow && d < weekEnd
            }
            val laterTasks = tasks.filter { task ->
                val d = task.dueDate?.date ?: return@filter false
                d >= weekEnd
            }

            val groups = buildList {
                if (todayTasks.isNotEmpty()) add(DateGroup("Today", todayTasks))
                if (tomorrowTasks.isNotEmpty()) add(DateGroup("Tomorrow", tomorrowTasks))
                if (thisWeekTasks.isNotEmpty()) add(DateGroup("This Week", thisWeekTasks))
                if (laterTasks.isNotEmpty()) add(DateGroup("Later", laterTasks))
            }

            UpcomingUiState(groups = groups)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UpcomingUiState())

    fun toggleDone(taskId: String) {
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
