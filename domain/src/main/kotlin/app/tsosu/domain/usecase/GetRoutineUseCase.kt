package app.tsosu.domain.usecase

import app.tsosu.domain.model.Routine
import app.tsosu.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow

class GetRoutineUseCase(
    private val routineRepository: RoutineRepository,
) {
    operator fun invoke(routineId: String): Flow<Routine?> {
        return routineRepository.getRoutine(routineId)
    }
}
