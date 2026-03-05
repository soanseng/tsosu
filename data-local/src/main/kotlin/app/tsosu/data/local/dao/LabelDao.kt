package app.tsosu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import app.tsosu.data.local.entity.LabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(label: LabelEntity)

    @Update
    suspend fun update(label: LabelEntity)

    @Query("SELECT * FROM labels ORDER BY title")
    fun getAll(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE id = :labelId")
    fun getById(labelId: String): Flow<LabelEntity?>

    @Upsert
    suspend fun upsert(label: LabelEntity)

    @Query("SELECT * FROM labels WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: Long): LabelEntity?
}
