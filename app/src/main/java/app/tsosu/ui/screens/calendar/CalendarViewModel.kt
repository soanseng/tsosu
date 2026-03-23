package app.tsosu.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val tasksByDate: Map<LocalDate, List<Task>> = emptyMap(),
    val selectedDayTasks: List<Task> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val setTaskStatus: SetTaskStatusUseCase,
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)

    val uiState: StateFlow<CalendarUiState> = _yearMonth
        .flatMapLatest { ym ->
            val tz = TimeZone.currentSystemDefault()
            val firstDay = LocalDate(ym.year, ym.monthValue, 1)
            val lastDay = LocalDate(ym.year, ym.monthValue, ym.lengthOfMonth())
            val start = firstDay.atStartOfDayIn(tz).toEpochMilliseconds()
            val end = lastDay.atStartOfDayIn(tz).toEpochMilliseconds() + 86_400_000 - 1

            taskDao.getUpcomingTasks(start, end).map { entities ->
                val tasks = entities.map { it.toDomain() }
                val grouped = tasks.groupBy { it.dueDate?.date }
                    .filterKeys { it != null }
                    .mapKeys { (k, _) -> k!! }
                ym to grouped
            }
        }
        .combine(_selectedDate) { (ym, grouped), selected ->
            CalendarUiState(
                yearMonth = ym,
                selectedDate = selected,
                tasksByDate = grouped,
                selectedDayTasks = selected?.let { grouped[it] } ?: emptyList(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun previousMonth() {
        _yearMonth.value = _yearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _yearMonth.value = _yearMonth.value.plusMonths(1)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleDone(taskId: String) {
        viewModelScope.launch {
            toggleTaskDone(taskId)
        }
    }

    fun setStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            setTaskStatus(taskId, status)
        }
    }
}
