package app.tsosu.data.local.repository

import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import app.tsosu.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val onTaskChanged: (suspend (entityId: String, operation: String, serverId: Long?) -> Unit)? = null,
) : TaskRepository {

    override fun getInboxTasks(): Flow<List<Task>> =
        taskDao.getInboxTasks().map { it.map { e -> e.toDomain() } }

    override fun getTodayTasks(): Flow<List<Task>> {
        val (start, end) = todayRange()
        return taskDao.getTodayTasks(start, end).map { it.map { e -> e.toDomain() } }
    }

    override fun getUpcomingTasks(days: Int): Flow<List<Task>> {
        val now = Clock.System.now()
        val start = now.toEpochMilliseconds()
        val end = (now + days.days).toEpochMilliseconds()
        return taskDao.getUpcomingTasks(start, end).map { it.map { e -> e.toDomain() } }
    }

    override fun getTasksForProject(projectId: String): Flow<List<Task>> =
        taskDao.getByProject(projectId).map { it.map { e -> e.toDomain() } }

    override fun getTask(taskId: String): Flow<Task?> =
        taskDao.getById(taskId).map { it?.toDomain() }

    override fun searchTasks(query: String): Flow<List<Task>> =
        taskDao.search(query).map { it.map { e -> e.toDomain() } }

    override fun getFocusTasks(date: LocalDate): Flow<List<Task>> {
        val (start, end) = dateRange(date)
        return taskDao.getFocusTasks(start, end).map { it.map { e -> e.toDomain() } }
    }

    override fun getTasksByEnergy(level: EnergyLevel): Flow<List<Task>> =
        taskDao.getByEnergyLevel(level.ordinal).map { it.map { e -> e.toDomain() } }

    override fun getAllActiveTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }.filter { !it.status.isTerminal }
        }

    override fun getStaleTaskIds(olderThanDays: Int): Flow<List<String>> {
        val threshold = (Clock.System.now() - olderThanDays.days).toEpochMilliseconds()
        return taskDao.getStaleTaskIds(threshold)
    }

    override suspend fun createTask(task: Task): Result<Task> = runCatching {
        taskDao.insert(task.toEntity())
        onTaskChanged?.invoke(task.id, "CREATE", null)
        task
    }

    override suspend fun updateTask(task: Task): Result<Task> = runCatching {
        val updated = task.copy(updatedAt = Clock.System.now())
        taskDao.update(updated.toEntity())
        onTaskChanged?.invoke(updated.id, "UPDATE", null)
        updated
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        val serverId = taskDao.getByIdSync(taskId)?.serverId
        taskDao.delete(taskId)
        onTaskChanged?.invoke(taskId, "DELETE", serverId)
    }

    override suspend fun toggleDone(taskId: String): Result<Task> = runCatching {
        val entity = taskDao.getById(taskId).first()
            ?: throw NoSuchElementException("Task $taskId not found")
        val now = Clock.System.now().toEpochMilliseconds()
        val currentStatus = TaskStatus.fromOrdinal(entity.status)
        val newStatus = if (currentStatus.isDone) TaskStatus.TODO else TaskStatus.DONE
        val completedDate = if (newStatus.isDone) now else null
        taskDao.setStatus(taskId, newStatus.ordinal, completedDate, now)
        onTaskChanged?.invoke(taskId, "UPDATE", null)
        entity.copy(
            status = newStatus.ordinal,
            done = newStatus.isDone,
            completedDate = completedDate,
            updatedAt = now,
        ).toDomain()
    }

    override suspend fun setStatus(taskId: String, status: TaskStatus): Result<Task> = runCatching {
        val entity = taskDao.getById(taskId).first()
            ?: throw NoSuchElementException("Task $taskId not found")
        val now = Clock.System.now().toEpochMilliseconds()
        val completedDate = if (status.isDone) now else null
        taskDao.setStatus(taskId, status.ordinal, completedDate, now)
        onTaskChanged?.invoke(taskId, "UPDATE", null)
        entity.copy(
            status = status.ordinal,
            done = status.isDone,
            completedDate = completedDate,
            updatedAt = now,
        ).toDomain()
    }

    override suspend fun reorder(taskId: String, newPosition: Double): Result<Unit> = runCatching {
        val task = taskDao.getById(taskId).first()
            ?: throw NoSuchElementException("Task $taskId not found")
        val now = Clock.System.now().toEpochMilliseconds()
        taskDao.update(task.copy(position = newPosition, updatedAt = now))
        onTaskChanged?.invoke(taskId, "UPDATE", null)
    }

    override suspend fun setFocus(taskId: String, isFocus: Boolean): Result<Task> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        taskDao.setFocus(taskId, isFocus, now)
        onTaskChanged?.invoke(taskId, "UPDATE", null)
        val task = taskDao.getById(taskId).first()
            ?: throw NoSuchElementException("Task $taskId not found")
        task.toDomain()
    }

    override suspend fun clearFocus(date: LocalDate): Result<Unit> = runCatching {
        val (start, end) = dateRange(date)
        val now = Clock.System.now().toEpochMilliseconds()
        taskDao.clearFocus(start, end, now)
    }

    override suspend fun archiveTasks(taskIds: List<String>): Result<Int> = runCatching {
        taskDao.deleteAll(taskIds)
    }

    private fun todayRange(): Pair<Long, Long> {
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDate(tz)
        return dateRange(today)
    }

    private fun dateRange(date: LocalDate): Pair<Long, Long> {
        val tz = TimeZone.currentSystemDefault()
        val start = date.atStartOfDayIn(tz).toEpochMilliseconds()
        val end = start + 86_400_000 - 1
        return start to end
    }

    private fun kotlinx.datetime.Instant.toLocalDate(tz: TimeZone): LocalDate =
        toLocalDateTime(tz).date
}
