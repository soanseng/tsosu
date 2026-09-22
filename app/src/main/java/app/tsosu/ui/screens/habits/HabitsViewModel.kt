package app.tsosu.ui.screens.habits

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.R
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.repository.GamificationRepository
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class HabitsUiState(
    val tasks: List<Task> = emptyList(),
    val shieldedByTask: Map<String, Set<LocalDate>> = emptyMap(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    taskRepository: TaskRepository,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val gamification: GamificationRepository,
) : ViewModel() {

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    /** Snackbar messages as string resource ids (localizable). */
    private val _messageEvent = MutableSharedFlow<Int>()
    val messageEvent = _messageEvent.asSharedFlow()

    private val _celebrateEvent = MutableSharedFlow<Unit>()
    val celebrateEvent = _celebrateEvent.asSharedFlow()

    /** A habit IS a recurring task (unified model): this tab lists them all. */
    val uiState: StateFlow<HabitsUiState> = combine(
        taskRepository.getRecurringTasks(),
        gamification.allShieldedDates(),
    ) { tasks, allShields ->
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        HabitsUiState(
            tasks = tasks,
            shieldedByTask = allShields.mapValues { (_, days) ->
                days.map { LocalDate.fromEpochDays(it.toInt()) }.toSet()
            },
            completedCount = tasks.count { today in it.completions },
            totalCount = tasks.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())

    val freezes: StateFlow<Int> = gamification.freezes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun buyFreeze() {
        viewModelScope.launch {
            val ok = gamification.buyFreeze()
            if (!ok) {
                _messageEvent.emit(R.string.habits_not_enough_energy)
            }
        }
    }

    /**
     * Completing today's occurrence records the date and resets the task to its
     * next due date. Tapping an already-completed day is a no-op so the series
     * never skips an occurrence.
     */
    fun onToggleRecurringTask(taskId: String) {
        viewModelScope.launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val task = uiState.value.tasks.find { it.id == taskId } ?: return@launch
            if (today in task.completions) return@launch
            // A check-in can consume a bought shield to bridge a gap; say so,
            // otherwise the streak silently repairs itself.
            val freezesBefore = gamification.freezes().first()
            toggleTaskDone(taskId)
                .onSuccess { updated ->
                    reminderScheduler.schedule(updated)
                    if (gamification.freezes().first() < freezesBefore) {
                        _messageEvent.emit(R.string.habits_shield_used)
                    }
                    _celebrateEvent.emit(Unit)
                }
                .onFailure { e ->
                    Log.e("HabitsViewModel", "Failed to complete recurring task", e)
                    _errorEvent.emit(e.message ?: "Unknown error")
                }
        }
    }
}
