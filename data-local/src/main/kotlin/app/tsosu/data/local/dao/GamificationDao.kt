package app.tsosu.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import app.tsosu.data.local.entity.GamificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {
    @Query("SELECT energy FROM gamification WHERE id = 1")
    fun energy(): Flow<Int?>

    @Query("SELECT energy FROM gamification WHERE id = 1")
    suspend fun getEnergy(): Int?

    @Query("UPDATE gamification SET energy = energy + :points WHERE id = 1")
    suspend fun awardEnergy(points: Int)

    /**
     * Spends points atomically; returns the number of rows updated
     * (0 = insufficient balance or missing row).
     */
    @Query("UPDATE gamification SET energy = energy - :points WHERE id = 1 AND energy >= :points")
    suspend fun spendEnergy(points: Int): Int

    @Query("INSERT OR IGNORE INTO gamification (id, energy) VALUES (1, 0)")
    suspend fun ensureRow()
}
