package app.tsosu.data.local.repository

import app.tsosu.data.local.dao.GamificationDao
import app.tsosu.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GamificationRepositoryImpl(
    private val gamificationDao: GamificationDao,
) : GamificationRepository {

    override fun energy(): Flow<Int> = gamificationDao.energy().map { it ?: 0 }

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
}
