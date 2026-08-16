package app.tsosu.domain.repository

import kotlinx.coroutines.flow.Flow
interface GamificationRepository {
    fun energy(): Flow<Int>
    fun freezes(): Flow<Int>

    /** Adds [points]; returns the new balance. */
    suspend fun awardEnergy(points: Int): Int

    /**
     * Spends [points] if the balance allows; returns success.
     * Never goes negative.
     */
    suspend fun spendEnergy(points: Int): Boolean

    suspend fun getEnergy(): Int

    /** Buys a streak freeze for energy (capped); returns success. */
    suspend fun buyFreeze(): Boolean

    /**
     * Consumes one freeze to bridge [gapEpochMillis] for [habitId].
     * Idempotent per (habitId, date). Returns success.
     */
    suspend fun shieldGap(habitId: String, gapEpochMillis: Long): Boolean

    /** Shielded (bridged) gap days for a habit, as epoch millis. */
    fun shieldedDates(habitId: String): Flow<List<Long>>

    companion object {
        const val FREEZE_COST = 30
        const val MAX_FREEZES = 2
    }
}
