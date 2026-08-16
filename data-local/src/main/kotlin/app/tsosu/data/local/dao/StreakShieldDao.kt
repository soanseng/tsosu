package app.tsosu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.tsosu.data.local.entity.StreakShieldEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakShieldDao {
    @Query("SELECT date FROM streak_shields WHERE habitId = :habitId")
    fun datesForHabit(habitId: String): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM streak_shields WHERE habitId = :habitId")
    suspend fun countForHabit(habitId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(shield: StreakShieldEntity)
}
