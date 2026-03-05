package app.tsosu.domain.usecase

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PickOneTaskUseCase(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(energyLevel: EnergyLevel): Flow<Task?> {
        return taskRepository.getTasksByEnergy(energyLevel).map { tasks ->
            val undone = tasks.filter { !it.done }
            undone.randomOrNull()
        }
    }
}
