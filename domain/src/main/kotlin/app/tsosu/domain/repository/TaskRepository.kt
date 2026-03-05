package app.tsosu.domain.repository

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TaskRepository {
    fun getInboxTasks(): Flow<List<Task>>
    fun getTodayTasks(): Flow<List<Task>>
    fun getUpcomingTasks(days: Int = 7): Flow<List<Task>>
    fun getTasksForProject(projectId: String): Flow<List<Task>>
    fun getTask(taskId: String): Flow<Task?>
    fun searchTasks(query: String): Flow<List<Task>>
    fun getFocusTasks(date: LocalDate): Flow<List<Task>>
    fun getTasksByEnergy(level: EnergyLevel): Flow<List<Task>>
    fun getStaleTaskIds(olderThanDays: Int = 14): Flow<List<String>>
    suspend fun createTask(task: Task): Result<Task>
    suspend fun updateTask(task: Task): Result<Task>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun toggleDone(taskId: String): Result<Task>
    suspend fun reorder(taskId: String, newPosition: Double): Result<Unit>
    suspend fun setFocus(taskId: String, isFocus: Boolean): Result<Task>
    suspend fun clearFocus(date: LocalDate): Result<Unit>
    suspend fun archiveTasks(taskIds: List<String>): Result<Int>
}
