package app.tsosu.ui.screens.habits

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.R
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.HabitStreakInfo
import app.tsosu.domain.model.Routine
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.repository.TaskRepository
import app.tsosu.domain.repository.RoutineRepository
import app.tsosu.domain.repository.GamificationRepository
import app.tsosu.domain.usecase.GetTodayHabitsUseCase
import app.tsosu.domain.usecase.CompleteHabitUseCase
import app.tsosu.domain.usecase.CreateTaskUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.domain.usecase.HabitWithStatus
import app.tsosu.notification.ReminderScheduler
import app.tsosu.notification.ReminderTriggerCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.atTime
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class HabitsUiState(
    val habits: List<HabitWithStatus> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val recurringTasks: List<Task> = emptyList(),
    val streaks: Map<String, HabitStreakInfo> = emptyMap(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    getTodayHabits: GetTodayHabitsUseCase,
    private val routineRepository: RoutineRepository,
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val createTaskUseCase: CreateTaskUseCase,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val completeHabit: CompleteHabitUseCase,
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

    val uiState: StateFlow<HabitsUiState> = combine(
        getTodayHabits(),
        routineRepository.getRoutines(),
        habitRepository.getAllStreakInfos(),
        taskRepository.getRecurringTasks(),
    ) { habits, routines, streaks, recurring ->
        HabitsUiState(
            habits = habits,
            routines = routines,
            streaks = streaks.associateBy { it.habitId },
            recurringTasks = recurring,
            completedCount = habits.count { it.isCompletedToday },
            totalCount = habits.size + recurring.size,
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


    fun onToggleHabit(habitId: String) {
        viewModelScope.launch {
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val wasCompleted = uiState.value.habits
                .find { it.habit.id == habitId }?.isCompletedToday ?: false
            if (wasCompleted) {
                habitRepository.uncompleteHabit(habitId, today)
            } else {
                completeHabit(habitId, today)
                _celebrateEvent.emit(Unit)
            }
        }
    }

    /** Toggling a recurring task completes today's occurrence; the repository resets it with the next due date. */
    fun onToggleRecurringTask(taskId: String) {
        viewModelScope.launch {
            toggleTaskDone(taskId).getOrNull()?.let { reminderScheduler.schedule(it) }
        }
    }

    /**
     * A habit IS a task with a recurrence rule (unified model): created as a daily
     * recurring task due today, with tiny/routine metadata carried in its note.
     */
    fun createRecurringTask(
        title: String,
        tinyVersion: String?,
        routineTime: RoutineTime,
        reminderTime: LocalTime? = null,
    ) {
        viewModelScope.launch {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val task = Task(
                title = title,
                tinyVersion = tinyVersion,
                routineTime = routineTime,
                reminderTime = reminderTime,
                dueDate = today.atTime(0, 0),
                recurrenceRule = "RRULE:FREQ=DAILY",
            )
            createTaskUseCase(task)
                .onSuccess { created -> reminderScheduler.schedule(created) }
                .onFailure { e ->
                    Log.e("HabitsViewModel", "Failed to create recurring task", e)
                    _errorEvent.emit(e.message ?: "Unknown error")
                }
        }
    }
}
