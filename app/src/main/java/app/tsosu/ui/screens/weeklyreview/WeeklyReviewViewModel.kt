package app.tsosu.ui.screens.weeklyreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.model.WeeklyReview
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.GetStaleTaskIdsUseCase
import app.tsosu.domain.usecase.GetWeeklyReviewUseCase
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.notification.ReminderScheduler
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    getWeeklyReviewUseCase: GetWeeklyReviewUseCase,
    getStaleTaskIds: GetStaleTaskIdsUseCase,
    private val taskRepository: TaskRepository,
    private val setTaskStatus: SetTaskStatusUseCase,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val weekStart = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    val review: StateFlow<WeeklyReview?> = getWeeklyReviewUseCase(weekStart)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Stale tasks for the guided keep/someday/delete step. */
    val staleTasks: StateFlow<List<Task>> =
        combine(getStaleTaskIds(olderThanDays = 14), taskRepository.getAllActiveTasks()) { ids, all ->
            val idSet = ids.toSet()
            all.filter { it.id in idSet }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Someday/Planned parking lot for the guided promote-or-delete step. */
    val somedayTasks: StateFlow<List<Task>> = taskRepository.getTasksByStatus(TaskStatus.PLANNED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun keepTask(taskId: String) {
        // Touching counts as engagement: bump updatedAt so it stops being stale.
        viewModelScope.launch {
            taskRepository.getTask(taskId).firstOrNull { it != null }?.let {
                taskRepository.updateTask(it)
            }
        }
    }

    fun parkTask(taskId: String) {
        viewModelScope.launch { setTaskStatus(taskId, TaskStatus.PLANNED) }
    }

    fun promoteTask(taskId: String) {
        viewModelScope.launch {
            setTaskStatus(taskId, TaskStatus.TODO).getOrNull()?.let { reminderScheduler.schedule(it) }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            reminderScheduler.cancel(taskId)
        }
    }
}
