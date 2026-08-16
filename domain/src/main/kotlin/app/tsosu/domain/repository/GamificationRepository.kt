package app.tsosu.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Local gamification state (energy earned by completing tasks/habits).
 * Not synced to the vault — pure device-local game currency.
 */
interface GamificationRepository {
    fun energy(): Flow<Int>

    /** Adds [points]; returns the new balance. */
    suspend fun awardEnergy(points: Int): Int

    /**
     * Spends [points] if the balance allows; returns success.
     * Never goes negative.
     */
    suspend fun spendEnergy(points: Int): Boolean

    suspend fun getEnergy(): Int
}
