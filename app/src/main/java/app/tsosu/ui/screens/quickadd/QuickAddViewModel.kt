package app.tsosu.ui.screens.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.usecase.CreateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import javax.inject.Inject

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val createTaskUseCase: CreateTaskUseCase,
) : ViewModel() {

    fun createTask(
        title: String,
        priority: Priority,
        energy: EnergyLevel,
        estimatedMinutes: Int?,
        dueDate: LocalDateTime?,
        reminderTime: LocalTime? = null,
        recurrenceRule: String? = null,
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                priority = priority,
                energyLevel = energy,
                estimatedMinutes = estimatedMinutes,
                dueDate = dueDate,
                reminderTime = reminderTime,
                recurrenceRule = recurrenceRule,
            )
            createTaskUseCase(task)
        }
    }
}
