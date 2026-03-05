package app.tsosu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.tsosu.data.local.entity.DailyFocusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(focus: DailyFocusEntity)

    @Query("SELECT * FROM daily_focus WHERE date = :date")
    fun getByDate(date: Long): Flow<DailyFocusEntity?>

    @Query("SELECT COUNT(*) FROM daily_focus WHERE date BETWEEN :startDate AND :endDate AND (taskId1 IS NOT NULL OR taskId2 IS NOT NULL OR taskId3 IS NOT NULL)")
    fun getFocusDaysCount(startDate: Long, endDate: Long): Flow<Int>
}
