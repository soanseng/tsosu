package app.tsosu.data.local.repository

import app.tsosu.data.local.dao.GamificationDao
import app.tsosu.data.local.dao.StreakShieldDao
import app.tsosu.data.local.entity.StreakShieldEntity
import app.tsosu.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GamificationRepositoryImpl(
    private val gamificationDao: GamificationDao,
    private val streakShieldDao: StreakShieldDao,
) : GamificationRepository {

    override fun energy(): Flow<Int> = gamificationDao.energy().map { it ?: 0 }

    override fun freezes(): Flow<Int> = gamificationDao.freezes().map { it ?: 0 }

    override suspend fun awardEnergy(points: Int): Int {
        if (points <= 0) return getEnergy()
        gamificationDao.ensureRow()
        gamificationDao.awardEnergy(points)
        return getEnergy()
    }

    override suspend fun spendEnergy(points: Int): Boolean {
        if (points <= 0) return true
        return gamificationDao.spendEnergy(points) > 0
    }

    override suspend fun getEnergy(): Int = gamificationDao.getEnergy() ?: 0

    override suspend fun buyFreeze(): Boolean {
        gamificationDao.ensureRow()
        return gamificationDao.buyFreeze(
            GamificationRepository.FREEZE_COST,
            GamificationRepository.MAX_FREEZES,
        ) > 0
    }

    override suspend fun shieldGap(habitId: String, gapEpochDays: Long): Boolean {
        // Idempotent per (id, day); a freeze is only spent when this call is
        // the one that actually bridges — and never when the balance is empty.
        if (streakShieldDao.exists(habitId, gapEpochDays) > 0) return true
        if (gamificationDao.spendFreeze() <= 0) return false
        streakShieldDao.insert(StreakShieldEntity(habitId, gapEpochDays))
        return true
    }

    override fun shieldedDates(habitId: String): Flow<List<Long>> =
        streakShieldDao.datesForHabit(habitId)

    override fun allShieldedDates(): Flow<Map<String, List<Long>>> =
        streakShieldDao.all().map { rows -> rows.groupBy({ it.habitId }, { it.date }) }
}
