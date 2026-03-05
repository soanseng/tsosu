package app.tsosu.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Task
import app.tsosu.domain.usecase.GetTodayOverviewUseCase
import app.tsosu.domain.usecase.TodayOverview
import app.tsosu.domain.usecase.ToggleTaskDoneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val focusTasks: List<Task> = emptyList(),
    val otherTasks: List<Task> = emptyList(),
    val totalEstimatedMinutes: Int = 0,
    val focusCount: Int = 0,
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    getTodayOverview: GetTodayOverviewUseCase,
    private val toggleTaskDone: ToggleTaskDoneUseCase,
) : ViewModel() {

    val uiState: StateFlow<FocusUiState> = getTodayOverview()
        .map { overview ->
            FocusUiState(
                focusTasks = overview.tasks.filter { it.isFocus },
                otherTasks = overview.tasks.filter { !it.isFocus },
                totalEstimatedMinutes = overview.totalEstimatedMinutes,
                focusCount = overview.focusCount,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusUiState())

    fun onToggleDone(taskId: String) {
        viewModelScope.launch {
            toggleTaskDone(taskId)
        }
    }
}
