package app.tsosu.ui.screens.habits

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitStreakInfo
import app.tsosu.domain.model.Routine
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.repository.RoutineRepository
import app.tsosu.domain.usecase.CompleteHabitUseCase
import app.tsosu.domain.usecase.CreateHabitUseCase
import app.tsosu.domain.usecase.GetTodayHabitsUseCase
import app.tsosu.domain.usecase.HabitWithStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class HabitsUiState(
    val habits: List<HabitWithStatus> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val streaks: Map<String, HabitStreakInfo> = emptyMap(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    getTodayHabits: GetTodayHabitsUseCase,
    private val routineRepository: RoutineRepository,
    private val habitRepository: HabitRepository,
    private val createHabitUseCase: CreateHabitUseCase,
    private val completeHabit: CompleteHabitUseCase,
) : ViewModel() {

    private val routineMutex = Mutex()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    val uiState: StateFlow<HabitsUiState> = combine(
        getTodayHabits(),
        routineRepository.getRoutines(),
        habitRepository.getAllStreakInfos(),
    ) { habits, routines, streaks ->
        HabitsUiState(
            habits = habits,
            routines = routines,
            streaks = streaks.associateBy { it.habitId },
            completedCount = habits.count { it.isCompletedToday },
            totalCount = habits.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())

    fun onToggleHabit(habitId: String) {
        viewModelScope.launch {
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            completeHabit(habitId, today)
        }
    }

    fun createHabit(title: String, tinyVersion: String?, routineTime: RoutineTime) {
        viewModelScope.launch {
            val routineId = findOrCreateRoutine(routineTime)
            val habit = Habit(
                title = title,
                tinyVersion = tinyVersion,
                routineId = routineId,
            )
            createHabitUseCase(habit).onFailure { e ->
                Log.e("HabitsViewModel", "Failed to create habit", e)
                _errorEvent.emit(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun findOrCreateRoutine(time: RoutineTime): String = routineMutex.withLock {
        val routines = routineRepository.getRoutines().first()
        val existing = routines.find { it.timeOfDay == time }
        if (existing != null) return@withLock existing.id

        val routine = Routine(
            title = time.name.lowercase().replaceFirstChar { it.uppercase() },
            timeOfDay = time,
        )
        routineRepository.createRoutine(routine)
        routine.id
    }
}
