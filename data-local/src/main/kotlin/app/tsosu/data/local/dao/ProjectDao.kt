package app.tsosu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import app.tsosu.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("SELECT * FROM projects ORDER BY position")
    fun getAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getById(projectId: String): Flow<ProjectEntity?>

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getByIdSync(id: String): ProjectEntity?

    @Query("DELETE FROM projects")
    suspend fun clearAll()

}
