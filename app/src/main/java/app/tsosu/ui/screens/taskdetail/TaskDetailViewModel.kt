package app.tsosu.ui.screens.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Project
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.DeleteTaskUseCase
import app.tsosu.domain.usecase.UpdateTaskUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class TaskDetailState(
    val task: Task? = null,
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val priority: Priority = Priority.NONE,
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    val dueDate: LocalDateTime? = null,
    val scheduledDate: LocalDateTime? = null,
    val startDate: LocalDateTime? = null,
    val estimatedMinutes: Int = 0,
    val recurrenceRule: String? = null,
    val dependsOn: List<String> = emptyList(),
    val completions: List<LocalDate> = emptyList(),
    val reminderTime: LocalTime? = null,
    val routineTime: RoutineTime? = null,
    val tinyVersion: String? = null,
    val projectId: String? = null,
    val projects: List<Project> = emptyList(),
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailState())
    val state: StateFlow<TaskDetailState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            projectRepository.getAllProjects().collect { projects ->
                _state.value = _state.value.copy(projects = projects)
            }
        }
    }

    fun loadTask(taskId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = TaskDetailState()
            taskRepository.getTask(taskId).filterNotNull().collect { task ->
                if (!_state.value.saved && !_state.value.deleted) {
                    _state.value = TaskDetailState(
                        projects = _state.value.projects,
                        task = task,
                        title = task.title,
                        description = task.description,
                        status = task.status,
                        priority = task.priority,
                        energyLevel = task.energyLevel,
                        estimatedMinutes = task.estimatedMinutes ?: 0,
                        dueDate = task.dueDate,
                        scheduledDate = task.scheduledDate,
                        startDate = task.startDate,
                        recurrenceRule = task.recurrenceRule,
                        completions = task.completions,
                        dependsOn = task.dependsOn,
                        reminderTime = task.reminderTime,
                        routineTime = task.routineTime,
                        tinyVersion = task.tinyVersion,
                        projectId = task.projectId,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value)
    }

    fun onDescriptionChange(value: String) {
        _state.value = _state.value.copy(description = value)
    }

    fun onStatusChange(value: TaskStatus) {
        _state.value = _state.value.copy(status = value)
    }

    fun onPriorityChange(value: Priority) {
        _state.value = _state.value.copy(priority = value)
    }

    fun onEnergyChange(value: EnergyLevel) {
        _state.value = _state.value.copy(energyLevel = value)
    }

    fun onEstimatedMinutesChange(value: Int) {
        _state.value = _state.value.copy(estimatedMinutes = value)
    }

    fun onDueDateChange(value: LocalDateTime?) {
        _state.value = _state.value.copy(dueDate = value)
    }

    fun onScheduledDateChange(value: LocalDateTime?) {
        _state.value = _state.value.copy(scheduledDate = value)
    }

    fun onStartDateChange(value: LocalDateTime?) {
        _state.value = _state.value.copy(startDate = value)
    }

    fun onRecurrenceRuleChange(value: String?) {
        _state.value = _state.value.copy(recurrenceRule = value)
    }

    fun onReminderTimeChange(value: LocalTime?) {
        _state.value = _state.value.copy(reminderTime = value)
    }

    fun onRoutineTimeChange(value: RoutineTime?) {
        _state.value = _state.value.copy(routineTime = value)
    }

    fun onCategoryChange(projectId: String?) {
        _state.value = _state.value.copy(projectId = projectId)
    }

    /** Creates a category and selects it; a duplicate name reuses the existing one. */
    fun createCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = _state.value.projects
                .firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
            val project = existing
                ?: projectRepository.createProject(Project(title = trimmed)).getOrNull()
            if (project != null) {
                _state.value = _state.value.copy(projectId = project.id)
            }
        }
    }

    fun save() {
        val task = _state.value.task ?: return
        viewModelScope.launch {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val newStatus = _state.value.status
            val completedDate = if (newStatus.isDone) {
                task.completedDate ?: now
            } else {
                null
            }
            val cancelledDate = if (newStatus == TaskStatus.CANCELLED) {
                task.cancelledDate ?: now
            } else {
                null
            }

            // A rule with no date yet gets its first occurrence due today.
            val effectiveDueDate = _state.value.dueDate ?: _state.value.recurrenceRule?.let {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.atTime(0, 0)
            }
            val updated = task.copy(
                title = _state.value.title,
                description = _state.value.description,
                status = newStatus,
                priority = _state.value.priority,
                energyLevel = _state.value.energyLevel,
                estimatedMinutes = _state.value.estimatedMinutes.takeIf { it > 0 },
                dueDate = effectiveDueDate,
                recurrenceRule = _state.value.recurrenceRule,
                scheduledDate = _state.value.scheduledDate,
                startDate = _state.value.startDate,
                dependsOn = _state.value.dependsOn,
                reminderTime = _state.value.reminderTime,
                routineTime = _state.value.routineTime.takeIf { _state.value.recurrenceRule != null },
                projectId = _state.value.projectId,
                completedDate = completedDate,
                cancelledDate = cancelledDate,
            )
            updateTaskUseCase(updated)
            reminderScheduler.schedule(updated)
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun delete() {
        val task = _state.value.task ?: return
        viewModelScope.launch {
            deleteTaskUseCase(task.id)
            reminderScheduler.cancel(task.id)
            _state.value = _state.value.copy(deleted = true)
        }
    }
}
