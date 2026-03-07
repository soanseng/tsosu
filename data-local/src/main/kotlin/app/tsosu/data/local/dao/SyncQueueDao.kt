package app.tsosu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tsosu.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {

    @Insert
    suspend fun insert(entry: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun findByEntity(entityType: String, entityId: String): SyncQueueEntity?

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun incrementRetry(id: Long, error: String)

    @Query("DELETE FROM sync_queue WHERE retryCount >= :maxRetries")
    suspend fun deleteStale(maxRetries: Int = 5): Int

    @Query("UPDATE sync_queue SET operation = :operation WHERE id = :id")
    suspend fun updateOperation(id: Long, operation: String)
}
