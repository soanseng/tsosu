package app.tsosu.data.local

import androidx.room.withTransaction
import app.tsosu.data.local.dao.FocusDao
import app.tsosu.data.local.dao.GamificationDao
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.RoutineDao
import app.tsosu.data.local.dao.StreakShieldDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.DailyFocusEntity
import app.tsosu.data.local.entity.GamificationEntity
import app.tsosu.data.local.entity.HabitCompletionEntity
import app.tsosu.data.local.entity.HabitEntity
import app.tsosu.data.local.entity.ProjectEntity
import app.tsosu.data.local.entity.RoutineEntity
import app.tsosu.data.local.entity.StreakShieldEntity
import app.tsosu.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val version: Int = BACKUP_VERSION,
    val exportedAtEpochMillis: Long,
    val tasks: List<TaskEntity>,
    val habits: List<HabitEntity>,
    val habitCompletions: List<HabitCompletionEntity>,
    val streakShields: List<StreakShieldEntity>,
    val gamification: GamificationEntity?,
    val dailyFocus: List<DailyFocusEntity>,
    val projects: List<ProjectEntity>,
    val routines: List<RoutineEntity>,
) {
    companion object {
        const val BACKUP_VERSION = 1
    }
}

/**
 * Full JSON backup/restore of the local Room store. Restore is transactional:
 * all tables are cleared and re-inserted in one transaction, so a failure
 * leaves the previous state intact.
 */
class BackupRepository(
    private val db: TsosuDatabase,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val focusDao: FocusDao,
    private val gamificationDao: GamificationDao,
    private val streakShieldDao: StreakShieldDao,
    private val projectDao: ProjectDao,
    private val routineDao: RoutineDao,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    suspend fun exportJson(): String {
        val data = BackupData(
            exportedAtEpochMillis = System.currentTimeMillis(),
            tasks = taskDao.getAllTasks().first(),
            habits = habitDao.getAllHabits().first(),
            habitCompletions = habitDao.getAllCompletions().first(),
            streakShields = streakShieldDao.all().first(),
            gamification = gamificationDao.getEnergyRow(),
            dailyFocus = focusDao.getAll(),
            projects = projectDao.getAll().first(),
            routines = routineDao.getAll().first(),
        )
        return json.encodeToString(data)
    }

    fun decode(jsonText: String): BackupData {
        val data = json.decodeFromString<BackupData>(jsonText)
        require(data.version <= BackupData.BACKUP_VERSION) { "Unsupported backup version" }
        return data
    }

    suspend fun restore(data: BackupData) {
        db.withTransaction {
            taskDao.clearAll()
            habitDao.clearAll()
            habitDao.clearCompletions()
            streakShieldDao.clearAll()
            focusDao.clearAll()
            projectDao.clearAll()
            routineDao.clearAll()
            gamificationDao.clearAll()

            data.projects.forEach { projectDao.insert(it) }
            data.routines.forEach { routineDao.insert(it) }
            data.habits.forEach { habitDao.insert(it) }
            data.habitCompletions.forEach { habitDao.insertCompletion(it) }
            data.streakShields.forEach { streakShieldDao.insert(it) }
            data.tasks.forEach { taskDao.insert(it) }
            data.dailyFocus.forEach { focusDao.insert(it) }
            data.gamification?.let { gamificationDao.insertRow(it) }
        }
    }
}
