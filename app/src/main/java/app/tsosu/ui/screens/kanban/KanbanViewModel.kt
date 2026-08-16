package app.tsosu.ui.screens.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GroupBy(val label: String) {
    STATUS("Status"),
    PRIORITY("Priority"),
    PROJECT("Project"),
    ENERGY("Energy"),
}

enum class KanbanViewMode { BOARD, LIST }

data class KanbanColumnData(
    val title: String,
    val tasks: List<Task>,
)

data class KanbanUiState(
    val groupBy: GroupBy = GroupBy.STATUS,
    val viewMode: KanbanViewMode = KanbanViewMode.BOARD,
    val columns: List<KanbanColumnData> = emptyList(),
)

@HiltViewModel
class KanbanViewModel @Inject constructor(
    taskRepository: TaskRepository,
    habitRepository: HabitRepository,
    projectRepository: ProjectRepository,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val setTaskStatus: SetTaskStatusUseCase,
) : ViewModel() {

    private val groupByFlow = MutableStateFlow(GroupBy.STATUS)
    private val viewModeFlow = MutableStateFlow(KanbanViewMode.BOARD)

    val uiState: StateFlow<KanbanUiState> = combine(
        taskRepository.getAllActiveTasks(),
        habitRepository.getActiveHabits(),
        projectRepository.getAllProjects(),
        groupByFlow,
        viewModeFlow,
    ) { tasks, habits, projects, groupBy, viewMode ->
        currentProjectNames = projects.associate { it.id to it.title }
        currentHabits = habits
        KanbanUiState(
            groupBy = groupBy,
            viewMode = viewMode,
            columns = groupTasks(tasks, groupBy),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KanbanUiState())

    // Latest snapshots for grouping helpers (updated by the combine above).
    private var currentProjectNames: Map<String, String> = emptyMap()
    private var currentHabits: List<Habit> = emptyList()

    /** Habits attached to a project column (Batch B: habits in project view). */
    fun habitsForProject(projectId: String?): List<Habit> =
        currentHabits.filter { it.projectId == projectId }

    /** Kanban columns are keyed by title; resolve habits through it. */
    fun habitsForProjectByTitle(title: String): List<Habit> {
        if (title == "No Project") return currentHabits.filter { it.projectId == null }
        val projectId = currentProjectNames.entries
            .firstOrNull { it.value == title }?.key ?: return emptyList()
        return currentHabits.filter { it.projectId == projectId }
    }

    fun setGroupBy(group: GroupBy) {
        groupByFlow.value = group
    }

    fun setViewMode(mode: KanbanViewMode) {
        viewModeFlow.value = mode
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
                val title = currentProjectNames[projectId] ?: projectId.take(8)
                add(KanbanColumnData(title = title, tasks = projectTasks))
            }
            // Projects that have only habits (no tasks) still show a column.
            val taskProjectIds = grouped.keys.filter { it.isNotEmpty() }.toSet()
            currentHabits.mapNotNull { it.projectId }.distinct()
                .filter { it !in taskProjectIds }
                .forEach { projectId ->
                    val title = currentProjectNames[projectId] ?: projectId.take(8)
                    add(KanbanColumnData(title = title, tasks = emptyList()))
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
