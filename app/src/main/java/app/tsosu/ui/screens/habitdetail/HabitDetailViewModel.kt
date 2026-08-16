package app.tsosu.ui.screens.habitdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.repository.RoutineRepository
import app.tsosu.notification.ReminderScheduler
import app.tsosu.notification.ReminderTriggerCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalTime
import javax.inject.Inject

/** Which routine group the habit belongs to; null = "Other". */
enum class HabitRoutineChoice(val time: RoutineTime?) {
    MORNING(RoutineTime.MORNING),
    ANYTIME(RoutineTime.AFTERNOON),
    EVENING(RoutineTime.EVENING),
    OTHER(null),
}

data class HabitDetailState(
    val habit: Habit? = null,
    val title: String = "",
    val tinyVersion: String = "",
    val routine: HabitRoutineChoice = HabitRoutineChoice.ANYTIME,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val targetDaysPerWeek: Int = 3,
    val energyLevel: EnergyLevel = EnergyLevel.LOW,
    val reminderTime: LocalTime? = null,
    val saved: Boolean = false,
    val archived: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class HabitDetailViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val routineRepository: RoutineRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(HabitDetailState())
    val state: StateFlow<HabitDetailState> = _state.asStateFlow()

    private val routineMutex = Mutex()
    private var loadJob: Job? = null

    fun loadHabit(habitId: String) {
        // Reset synchronously before the DB suspension so a stale
        // saved/archived flag can't auto-dismiss the sheet on re-open.
        loadJob?.cancel()
        _state.value = HabitDetailState()
        loadJob = viewModelScope.launch {
            val habit = habitRepository.getHabit(habitId).first() ?: return@launch
            val routineTime = habit.routineId?.let { id ->
                routineRepository.getRoutines().first()
                    .find { it.id == id }?.timeOfDay
            }
            _state.value = HabitDetailState(
                habit = habit,
                title = habit.title,
                tinyVersion = habit.tinyVersion ?: "",
                routine = when (routineTime) {
                    RoutineTime.MORNING -> HabitRoutineChoice.MORNING
                    RoutineTime.AFTERNOON -> HabitRoutineChoice.ANYTIME
                    RoutineTime.EVENING -> HabitRoutineChoice.EVENING
                    null -> HabitRoutineChoice.OTHER
                },
                frequency = habit.frequency,
                targetDaysPerWeek = habit.targetDaysPerWeek,
                energyLevel = habit.energyLevel,
                reminderTime = habit.reminderTime,
            )
        }
    }

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value)
    }

    fun onTinyVersionChange(value: String) {
        _state.value = _state.value.copy(tinyVersion = value)
    }

    fun onRoutineChange(value: HabitRoutineChoice) {
        _state.value = _state.value.copy(routine = value)
    }

    fun onFrequencyChange(value: HabitFrequency) {
        _state.value = _state.value.copy(frequency = value)
    }

    fun onTargetDaysChange(value: Int) {
        _state.value = _state.value.copy(targetDaysPerWeek = value.coerceIn(1, 7))
    }

    fun onEnergyChange(value: EnergyLevel) {
        _state.value = _state.value.copy(energyLevel = value)
    }

    fun onReminderChange(value: LocalTime?) {
        _state.value = _state.value.copy(reminderTime = value)
    }

    fun save() {
        val current = _state.value
        val habit = current.habit ?: return
        if (current.title.isBlank()) {
            _state.value = current.copy(error = true)
            return
        }
        viewModelScope.launch {
            val routineId = current.routine.time?.let { findOrCreateRoutine(it) }
            val updated = habit.copy(
                title = current.title.trim(),
                tinyVersion = current.tinyVersion.trim().ifBlank { null },
                routineId = routineId,
                frequency = current.frequency,
                targetDaysPerWeek = current.targetDaysPerWeek,
                energyLevel = current.energyLevel,
                reminderTime = current.reminderTime,
            )
            habitRepository.updateHabit(updated)
                .onSuccess {
                    syncAlarm(updated.id, current.reminderTime, isArchived = false)
                    _state.value = _state.value.copy(saved = true)
                }
                .onFailure { _state.value = _state.value.copy(error = true) }
        }
    }

    fun archive() {
        val habit = _state.value.habit ?: return
        viewModelScope.launch {
            habitRepository.archiveHabit(habit.id)
                .onSuccess {
                    reminderScheduler.cancelHabit(habit.id)
                    _state.value = _state.value.copy(archived = true)
                }
                .onFailure { _state.value = _state.value.copy(error = true) }
        }
    }

    private suspend fun findOrCreateRoutine(time: RoutineTime): String = routineMutex.withLock {
        val existing = routineRepository.getRoutines().first().find { it.timeOfDay == time }
        if (existing != null) return@withLock existing.id

        val routine = app.tsosu.domain.model.Routine(
            title = time.name.lowercase().replaceFirstChar { it.uppercase() },
            timeOfDay = time,
        )
        routineRepository.createRoutine(routine)
        routine.id
    }

    private fun syncAlarm(habitId: String, reminderTime: LocalTime?, isArchived: Boolean) {
        val trigger = ReminderTriggerCalculator.triggerMillisForHabit(
            reminderMinutes = reminderTime?.let { it.hour * 60 + it.minute },
            isArchived = isArchived,
        )
        if (trigger != null) reminderScheduler.scheduleHabit(habitId, trigger)
        else reminderScheduler.cancelHabit(habitId)
    }
}
