package app.tsosu.data.local.repository

import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.RoutineDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.model.Routine
import app.tsosu.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoutineRepositoryImpl(
    private val routineDao: RoutineDao,
    private val habitDao: HabitDao,
) : RoutineRepository {

    override fun getRoutines(): Flow<List<Routine>> =
        routineDao.getAll().map { routines ->
            routines.map { entity ->
                entity.toDomain()
            }
        }

    override fun getRoutine(routineId: String): Flow<Routine?> =
        combine(
            routineDao.getById(routineId),
            habitDao.getByRoutine(routineId),
        ) { routine, habits ->
            routine?.toDomain(habits.map { it.toDomain() })
        }

    override suspend fun createRoutine(routine: Routine): Result<Routine> = runCatching {
        routineDao.insert(routine.toEntity())
        routine
    }

    override suspend fun updateRoutine(routine: Routine): Result<Routine> = runCatching {
        routineDao.update(routine.toEntity())
        routine
    }

    override suspend fun deleteRoutine(routineId: String): Result<Unit> = runCatching {
        routineDao.delete(routineId)
    }
}
