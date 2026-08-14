package app.tsosu.data.markdown

import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncResult
import app.tsosu.domain.repository.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class MarkdownSyncRepository(
    private val preferences: MarkdownPreferences,
    private val syncManager: MarkdownSyncManager,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val projectDao: ProjectDao,
) : SyncRepository {

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    private val conflictDetector = ConflictDetector()

    override fun syncState(): Flow<SyncState> = _syncState

    override fun isConfigured(): Flow<Boolean> = preferences.isConfigured()

    override suspend fun sync(): Result<SyncResult> = runCatching {
        _syncState.value = SyncState.SYNCING

        // 1. IMPORT first (capture external edits before overwriting)
        val importedTasks = syncManager.importTasks()
        val importedHabits = syncManager.importHabits()

        // 2. Detect conflicts BEFORE the import overwrites the app-side state:
        //    both vault and Room changed since the last export → flag the task.
        val roomBefore = taskDao.getAllTasks().first().map { it.toDomain() }
        val conflictIds = conflictDetector.detect(
            importedTasks = importedTasks.tasks,
            appTasks = roomBefore,
            lastExportedHashes = preferences.getTaskHashes(),
        )

        // 3. Merge: upsert imported data into Room (external edits win for conflicts)
        for (task in importedTasks.tasks) {
            taskDao.upsert(task.toEntity())
        }
        for (habit in importedHabits.habits) {
            habitDao.insert(habit.toEntity())
        }
        for (completion in importedHabits.completions) {
            habitDao.insertCompletion(completion.toEntity())
        }

        // 4. EXPORT (write current Room state, which now includes external edits)
        val tasks = taskDao.getAllTasks().first().map { it.toDomain() }
        val projects = projectDao.getAll().first()
        val projectNames = projects.associate { it.id to it.title }
        syncManager.exportTasks(tasks, projectNames, conflictIds)

        val habits = habitDao.getActiveHabits().first().map { it.toDomain() }
        val completions = mutableListOf<HabitCompletion>()
        for (habit in habits) {
            val hc = habitDao.getAllCompletionsForHabit(habit.id).first()
            completions.addAll(hc.map { it.toDomain() })
        }
        syncManager.exportHabits(habits, completions)

        // Export today's daily note
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayCompletions = completions.filter { it.date == today }
            .map { it.habitId }.toSet()
        syncManager.exportDailyNote(today, habits, todayCompletions)

        // Refresh the last-exported baseline for future conflict detection
        preferences.setTaskHashes(
            tasks.associate { it.id to conflictDetector.serializer.formatTask(it) },
        )

        preferences.setLastSync(System.currentTimeMillis())
        _syncState.value = SyncState.IDLE

        SyncResult(
            exported = tasks.size + habits.size,
            imported = importedTasks.tasks.size + importedHabits.habits.size,
        )
    }.onFailure {
        _syncState.value = SyncState.ERROR
    }

    override suspend fun disconnect() {
        preferences.clear()
        _syncState.value = SyncState.IDLE
    }
}
