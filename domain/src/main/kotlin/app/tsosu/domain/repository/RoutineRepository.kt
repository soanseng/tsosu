package app.tsosu.domain.repository

import app.tsosu.domain.model.Routine
import kotlinx.coroutines.flow.Flow

interface RoutineRepository {
    fun getRoutines(): Flow<List<Routine>>
    fun getRoutine(routineId: String): Flow<Routine?>
    suspend fun createRoutine(routine: Routine): Result<Routine>
    suspend fun updateRoutine(routine: Routine): Result<Routine>
    suspend fun deleteRoutine(routineId: String): Result<Unit>
}
