package app.tsosu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import app.tsosu.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun delete(taskId: String)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getById(taskId: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE projectId IS NULL AND dueDate IS NULL AND status < 4 ORDER BY position")
    fun getInboxTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :startOfDay AND :endOfDay AND status < 4 ORDER BY position")
    fun getTodayTasks(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :start AND :end AND status < 4 ORDER BY dueDate, position")
    fun getUpcomingTasks(start: Long, end: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY position")
    fun getByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun search(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isFocus = 1 AND dueDate BETWEEN :startOfDay AND :endOfDay ORDER BY position")
    fun getFocusTasks(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE energyLevel = :level AND status < 4 ORDER BY position")
    fun getByEnergyLevel(level: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE recurrenceRule IS NOT NULL AND status < 4 ORDER BY dueDate, position")
    fun getRecurringTasks(): Flow<List<TaskEntity>>

    @Query("SELECT id FROM tasks WHERE status < 4 AND updatedAt < :threshold ORDER BY updatedAt")
    fun getStaleTaskIds(threshold: Long): Flow<List<String>>

    @Query("SELECT * FROM tasks WHERE dueDate < :now AND status < 4 ORDER BY dueDate")
    fun getOverdueTasks(now: Long): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET isFocus = :isFocus, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun setFocus(taskId: String, isFocus: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET isFocus = 0, updatedAt = :updatedAt WHERE dueDate BETWEEN :startOfDay AND :endOfDay")
    suspend fun clearFocus(startOfDay: Long, endOfDay: Long, updatedAt: Long)

    @Query("UPDATE tasks SET status = :status, done = CASE WHEN :status = 4 THEN 1 ELSE 0 END, completedDate = :completedDate, cancelledDate = :cancelledDate, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun setStatus(taskId: String, status: Int, completedDate: Long?, cancelledDate: Long?, updatedAt: Long)

    @Query("DELETE FROM tasks WHERE id IN (:taskIds)")
    suspend fun deleteAll(taskIds: List<String>): Int

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("SELECT * FROM tasks ORDER BY position")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getByIdSync(taskId: String): TaskEntity?

    @Query("UPDATE tasks SET timeSpentMinutes = timeSpentMinutes + :minutes, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun addTimeSpent(taskId: String, minutes: Int, updatedAt: Long)

    @Query("UPDATE tasks SET serverId = :serverId WHERE id = :id")
    suspend fun updateServerId(id: String, serverId: Long)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()

}
