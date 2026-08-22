package app.tsosu.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.usecase.SetTaskStatusUseCase
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.asStateFlow
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
    val externalEventsByDate: Map<LocalDate, List<String>> = emptyMap(),
    val subscriptions: Set<String> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val setTaskStatus: SetTaskStatusUseCase,
    private val icsSubscriptions: app.tsosu.data.calendar.IcsSubscriptionRepository,
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)

    private val _externalEvents = MutableStateFlow<Map<LocalDate, List<String>>>(emptyMap())
    val externalEvents: StateFlow<Map<LocalDate, List<String>>> = _externalEvents.asStateFlow()

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
        .combine(_selectedDate) { pair, selected ->
            val (ym, grouped) = pair
            ym to Triple(grouped, selected, _externalEvents.value)
        }
        .combine(_externalEvents) { (ym, triple), external ->
            val (grouped, selected, _) = triple
            CalendarUiState(
                yearMonth = ym,
                selectedDate = selected,
                tasksByDate = grouped,
                selectedDayTasks = selected?.let { grouped[it] } ?: emptyList(),
                externalEventsByDate = external,
                subscriptions = subscriptionUrls.value,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    private val _showExternal = MutableStateFlow(false)
    val showExternal: StateFlow<Boolean> = _showExternal.asStateFlow()

    init {
        // Refetch the subscription overlay whenever it is visible and the
        // visible month changes.
        viewModelScope.launch {
            combine(_showExternal, _yearMonth, icsSubscriptions.urls) { show, ym, _ -> show to ym }
                .collect { (show, ym) ->
                    if (show) {
                        val first = LocalDate(ym.year, ym.monthValue, 1)
                        val last = LocalDate(ym.year, ym.monthValue, ym.lengthOfMonth())
                        val events = icsSubscriptions.fetchEvents(first, last)
                        _externalEvents.value = events.groupBy({ it.start }, { it.summary })
                    } else {
                        _externalEvents.value = emptyMap()
                    }
                }
        }
    }

    val subscriptionUrls: StateFlow<Set<String>> = icsSubscriptions.urls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setShowExternal(show: Boolean) {
        _showExternal.value = show
    }

    fun addSubscription(url: String) {
        viewModelScope.launch { icsSubscriptions.addUrl(url) }
    }

    fun removeSubscription(url: String) {
        viewModelScope.launch { icsSubscriptions.removeUrl(url) }
    }

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
            toggleTaskDone(taskId).getOrNull()?.let { reminderScheduler.schedule(it) }
        }
    }

    fun setStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            setTaskStatus(taskId, status)
        }
    }
}
