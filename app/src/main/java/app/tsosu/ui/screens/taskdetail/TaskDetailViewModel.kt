package app.tsosu.ui.screens.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.DeleteTaskUseCase
import app.tsosu.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import javax.inject.Inject

data class TaskDetailState(
    val task: Task? = null,
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.NONE,
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    val estimatedMinutes: Int = 0,
    val dueDate: LocalDateTime? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailState())
    val state: StateFlow<TaskDetailState> = _state.asStateFlow()

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.getTask(taskId).filterNotNull().collect { task ->
                if (!_state.value.saved && !_state.value.deleted) {
                    _state.value = TaskDetailState(
                        task = task,
                        title = task.title,
                        description = task.description,
                        priority = task.priority,
                        energyLevel = task.energyLevel,
                        estimatedMinutes = task.estimatedMinutes ?: 0,
                        dueDate = task.dueDate,
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

    fun save() {
        val task = _state.value.task ?: return
        viewModelScope.launch {
            val updated = task.copy(
                title = _state.value.title,
                description = _state.value.description,
                priority = _state.value.priority,
                energyLevel = _state.value.energyLevel,
                estimatedMinutes = _state.value.estimatedMinutes.takeIf { it > 0 },
                dueDate = _state.value.dueDate,
            )
            updateTaskUseCase(updated)
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun delete() {
        val task = _state.value.task ?: return
        viewModelScope.launch {
            deleteTaskUseCase(task.id)
            _state.value = _state.value.copy(deleted = true)
        }
    }
}
