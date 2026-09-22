package app.tsosu.ui.screens.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Project
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.usecase.CreateTaskUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalTime
import javax.inject.Inject

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val createTaskUseCase: CreateTaskUseCase,
    private val projectRepository: ProjectRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    /** Existing categories, for the picker row. */
    val projects: StateFlow<List<Project>> = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createTask(
        title: String,
        priority: Priority,
        dueDate: LocalDateTime?,
        reminderTime: LocalTime? = null,
        recurrenceRule: String? = null,
        projectName: String? = null,
        routineTime: RoutineTime? = null,
        tinyVersion: String? = null,
        projectId: String? = null,
        newCategoryName: String? = null,
    ) {
        viewModelScope.launch {
            val categoryId = projectId
                ?: newCategoryName?.let { resolveCategoryId(it) }
                ?: projectName?.let { resolveCategoryId(it) }
            // A rule with no date yet gets its first occurrence due today.
            val effectiveDueDate = dueDate ?: recurrenceRule?.let {
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.atTime(0, 0)
            }
            val task = Task(
                title = title,
                priority = priority,
                dueDate = effectiveDueDate,
                reminderTime = reminderTime,
                recurrenceRule = recurrenceRule,
                projectId = categoryId,
                routineTime = routineTime,
                tinyVersion = tinyVersion,
            )
            createTaskUseCase(task).getOrNull()?.let { reminderScheduler.schedule(it) }
        }
    }

    /**
     * Resolves a category name to an id: reuses a case-insensitive match,
     * otherwise creates it. Unknown names used to be dropped, which made
     * typing a new category a silent no-op.
     */
    private suspend fun resolveCategoryId(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val existing = projectRepository.getAllProjects().first()
            .firstOrNull { it.title.equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing.id
        return projectRepository.createProject(Project(title = trimmed)).getOrNull()?.id
    }
}
