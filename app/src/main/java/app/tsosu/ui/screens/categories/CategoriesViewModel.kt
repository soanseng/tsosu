package app.tsosu.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Project
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One category and its open tasks. [project] is null for the trailing
 * "uncategorized" bucket, which also absorbs tasks whose category was
 * deleted while the vault was synced elsewhere.
 */
data class CategoryGroup(
    val project: Project?,
    val tasks: List<Task>,
)

data class CategoriesUiState(
    val groups: List<CategoryGroup> = emptyList(),
    val taskCount: Int = 0,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    taskRepository: TaskRepository,
    projectRepository: ProjectRepository,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = combine(
        taskRepository.getAllActiveTasks(),
        projectRepository.getAllProjects(),
    ) { tasks, projects ->
        val byProject = tasks.groupBy { it.projectId }
        val known = projects.map { project ->
            CategoryGroup(project, byProject[project.id].orEmpty())
        }
        val listed = projects.mapNotNull { it.id }.toSet()
        val uncategorized = byProject
            .filterKeys { it == null || it !in listed }
            .values
            .flatten()
        CategoriesUiState(
            groups = known + listOf(CategoryGroup(null, uncategorized))
                .filter { it.tasks.isNotEmpty() },
            taskCount = tasks.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun toggleDone(taskId: String) {
        viewModelScope.launch {
            toggleTaskDone(taskId).getOrNull()?.let { reminderScheduler.schedule(it) }
        }
    }
}
