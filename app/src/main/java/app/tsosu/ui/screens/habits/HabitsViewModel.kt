package app.tsosu.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Routine
import app.tsosu.domain.repository.RoutineRepository
import app.tsosu.domain.usecase.CompleteHabitUseCase
import app.tsosu.domain.usecase.GetTodayHabitsUseCase
import app.tsosu.domain.usecase.HabitWithStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class HabitsUiState(
    val habits: List<HabitWithStatus> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    getTodayHabits: GetTodayHabitsUseCase,
    routineRepository: RoutineRepository,
    private val completeHabit: CompleteHabitUseCase,
) : ViewModel() {

    val uiState: StateFlow<HabitsUiState> = combine(
        getTodayHabits(),
        routineRepository.getRoutines(),
    ) { habits, routines ->
        HabitsUiState(
            habits = habits,
            routines = routines,
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
}
