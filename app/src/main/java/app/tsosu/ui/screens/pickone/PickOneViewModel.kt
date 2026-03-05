package app.tsosu.ui.screens.pickone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Task
import app.tsosu.domain.usecase.PickOneTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PickOneViewModel @Inject constructor(
    private val pickOneTask: PickOneTaskUseCase,
) : ViewModel() {

    val selectedEnergy = MutableStateFlow(EnergyLevel.MEDIUM)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pickedTask: StateFlow<Task?> = selectedEnergy
        .flatMapLatest { energy -> pickOneTask(energy) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectEnergy(level: EnergyLevel) {
        selectedEnergy.value = level
    }

    fun pickAnother() {
        // Re-emit to trigger a new random pick
        selectedEnergy.value = selectedEnergy.value
    }
}
