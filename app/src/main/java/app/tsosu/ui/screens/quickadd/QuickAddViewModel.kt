package app.tsosu.ui.screens.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.usecase.CreateTaskUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
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

    fun createTask(
        title: String,
        priority: Priority,
        dueDate: LocalDateTime?,
        reminderTime: LocalTime? = null,
        recurrenceRule: String? = null,
        projectName: String? = null,
        routineTime: RoutineTime? = null,
        tinyVersion: String? = null,
    ) {
        viewModelScope.launch {
            // @project token: file into an existing project matched by name
            // (case-insensitive); unknown names are ignored, not created.
            val projectId = projectName?.let { name ->
                projectRepository.getAllProjects().first()
                    .firstOrNull { it.title.equals(name, ignoreCase = true) }?.id
            }
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
                projectId = projectId,
                routineTime = routineTime,
                tinyVersion = tinyVersion,
            )
            createTaskUseCase(task).getOrNull()?.let { reminderScheduler.schedule(it) }
        }
    }
}
