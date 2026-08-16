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

    @Query("SELECT freezes FROM gamification WHERE id = 1")
    fun freezes(): Flow<Int?>

    @Query("SELECT freezes FROM gamification WHERE id = 1")
    suspend fun getFreezes(): Int?

    /** Atomically spends one freeze; returns rows updated (0 = none left). */
    @Query("UPDATE gamification SET freezes = freezes - 1 WHERE id = 1 AND freezes > 0")
    suspend fun spendFreeze(): Int

    /** Buys a freeze with energy; atomic only when both conditions hold. */
    @Query(
        """UPDATE gamification SET energy = energy - :cost, freezes = freezes + 1
           WHERE id = 1 AND energy >= :cost AND freezes < :maxFreezes""",
    )
    suspend fun buyFreeze(cost: Int, maxFreezes: Int): Int

    /** Spends points atomically; returns rows updated
     * (0 = insufficient balance or missing row). */
    @Query("UPDATE gamification SET energy = energy - :points WHERE id = 1 AND energy >= :points")
    suspend fun spendEnergy(points: Int): Int

    @Query("INSERT OR IGNORE INTO gamification (id, energy) VALUES (1, 0)")
    suspend fun ensureRow()
}
