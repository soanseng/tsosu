package app.tsosu.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    taskRepository: TaskRepository,
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = taskRepository.getInboxTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
