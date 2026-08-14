package app.tsosu.ui.screens.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GroupBy(val label: String) {
    STATUS("Status"),
    PRIORITY("Priority"),
    PROJECT("Project"),
    ENERGY("Energy"),
}

data class KanbanColumnData(
    val title: String,
    val tasks: List<Task>,
)

data class KanbanUiState(
    val groupBy: GroupBy = GroupBy.STATUS,
    val columns: List<KanbanColumnData> = emptyList(),
)

@HiltViewModel
class KanbanViewModel @Inject constructor(
    taskRepository: TaskRepository,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val setTaskStatus: SetTaskStatusUseCase,
) : ViewModel() {

    private val groupByFlow = MutableStateFlow(GroupBy.STATUS)

    val uiState: StateFlow<KanbanUiState> = combine(
        taskRepository.getAllActiveTasks(),
        groupByFlow,
    ) { tasks, groupBy ->
        KanbanUiState(
            groupBy = groupBy,
            columns = groupTasks(tasks, groupBy),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KanbanUiState())

    fun setGroupBy(group: GroupBy) {
        groupByFlow.value = group
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

    private fun groupTasks(tasks: List<Task>, groupBy: GroupBy): List<KanbanColumnData> =
        when (groupBy) {
            GroupBy.STATUS -> groupByStatus(tasks)
            GroupBy.PRIORITY -> groupByPriority(tasks)
            GroupBy.PROJECT -> groupByProject(tasks)
            GroupBy.ENERGY -> groupByEnergy(tasks)
        }

    private fun groupByStatus(tasks: List<Task>): List<KanbanColumnData> {
        val activeStatuses = TaskStatus.entries.filter { !it.isTerminal }
        return activeStatuses.map { status ->
            KanbanColumnData(
                title = status.displayLabel(),
                tasks = tasks.filter { it.status == status },
            )
        }
    }

    private fun groupByPriority(tasks: List<Task>): List<KanbanColumnData> =
        Priority.entries.reversed().map { priority ->
            KanbanColumnData(
                title = if (priority == Priority.NONE) "No Priority" else "${priority.emoji} ${priority.name.lowercase().replaceFirstChar { it.uppercase() }}",
                tasks = tasks.filter { it.priority == priority },
            )
        }

    private fun groupByProject(tasks: List<Task>): List<KanbanColumnData> {
        val grouped = tasks.groupBy { it.projectId ?: "" }
        return buildList {
            grouped[""]?.let { noProject ->
                add(KanbanColumnData(title = "No Project", tasks = noProject))
            }
            grouped.filter { it.key.isNotEmpty() }.forEach { (projectId, projectTasks) ->
                add(KanbanColumnData(title = projectId.take(8), tasks = projectTasks))
            }
        }
    }

    private fun groupByEnergy(tasks: List<Task>): List<KanbanColumnData> =
        EnergyLevel.entries.map { level ->
            KanbanColumnData(
                title = "${level.emoji} ${level.name.lowercase().replaceFirstChar { it.uppercase() }}",
                tasks = tasks.filter { it.energyLevel == level },
            )
        }

    private fun TaskStatus.displayLabel(): String = when (this) {
        TaskStatus.TODO -> "To Do"
        TaskStatus.IN_PROGRESS -> "In Progress"
        TaskStatus.ON_HOLD -> "On Hold"
        TaskStatus.PLANNED -> "Planned"
        TaskStatus.DONE -> "Done"
        TaskStatus.CANCELLED -> "Cancelled"
    }
}
