package app.tsosu.data.markdown

import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.RoutineDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.RoutineEntity
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncResult
import app.tsosu.domain.repository.SyncState
import java.util.UUID
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
    private val routineDao: RoutineDao,
) : SyncRepository {

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    private val conflictDetector = ConflictDetector()

    /** Conflict ids detected by the last pull, consumed by the next push. */
    private var pendingConflictIds: Set<String> = emptySet()
    private var lastImportedCount: Int = 0

    override fun syncState(): Flow<SyncState> = _syncState

    override fun isConfigured(): Flow<Boolean> = preferences.isConfigured()

    override suspend fun sync(): Result<SyncResult> = wrapSyncState {
        pullInternal().getOrThrow()
        val exportedCount = pushInternal().getOrThrow()

        Result.success(
            SyncResult(
                exported = exportedCount,
                imported = lastImportedCount,
            ),
        )
    }

    override suspend fun pull(): Result<Unit> = wrapSyncState { pullInternal() }

    private suspend fun pullInternal(): Result<Unit> = runCatching {
        // 1. IMPORT (capture external edits before overwriting)
        val importedTasks = syncManager.importTasks()
        val importedHabits = syncManager.importHabits()

        // 2. Detect conflicts BEFORE the import overwrites the app-side state:
        //    both vault and Room changed since the last export → flag the task.
        val roomBefore = taskDao.getAllTasks().first().map { it.toDomain() }
        pendingConflictIds = conflictDetector.detect(
            importedTasks = importedTasks.tasks,
            appTasks = roomBefore,
            lastExportedHashes = preferences.getTaskHashes(),
        )
        lastImportedCount = importedTasks.tasks.size + importedHabits.habits.size

        // 3. Merge: upsert imported data into Room (external edits win for conflicts)
        for (task in importedTasks.tasks) {
            taskDao.upsert(task.toEntity())
        }
        // Import every habit (parsedNotes covers note files; index-only lines
        // come through habits/completions with the index routine map).
        val routineTimeByNote = importedHabits.parsedNotes.associate { (note, time) ->
            note.habit.id to time
        }
        for (habit in importedHabits.habits) {
            val routineTime = routineTimeByNote[habit.id]
                ?: importedHabits.routineTimeByHabitId[habit.id]
            val routineId = routineTime?.let { resolveRoutineId(it) } ?: habit.routineId
            habitDao.insert(habit.copy(routineId = routineId).toEntity())
        }
        for (completion in importedHabits.completions) {
            habitDao.insertCompletion(completion.toEntity())
        }

        preferences.setLastSync(System.currentTimeMillis())
    }

    override suspend fun push(): Result<Unit> = wrapSyncState { pushInternal().map { } }

    private suspend fun pushInternal(): Result<Int> = runCatching {
        val conflictIds = pendingConflictIds

        // Export current Room state (which now includes external edits)
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
        // exportHabits expects habitId → RoutineTime; resolve through each habit's routineId.
        val routinesById = routineDao.getAll().first()
            .associate { it.id to RoutineTime.fromOrdinal(it.timeOfDay) }
        val routineTimeByHabitId = habits.mapNotNull { habit ->
            habit.routineId?.let { rid -> routinesById[rid]?.let { habit.id to it } }
        }.toMap()
        syncManager.exportHabits(habits, completions, routineTimeByHabitId)

        // Export today's daily note
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val todayCompletions = completions.filter { it.date == today }
            .map { it.habitId }.toSet()
        syncManager.exportDailyNote(today, habits, todayCompletions)

        // Refresh the last-exported baseline for future conflict detection
        preferences.setTaskHashes(
            tasks.associate { it.id to conflictDetector.serializer.formatTask(it) },
        )
        // Conflict markers were written; don't re-emit them on later pushes.
        pendingConflictIds = emptySet()

        preferences.setLastSync(System.currentTimeMillis())
        tasks.size + habits.size
    }

    override suspend fun disconnect() {
        preferences.clear()
        _syncState.value = SyncState.IDLE
    }

    private suspend fun <T> wrapSyncState(action: suspend () -> Result<T>): Result<T> {
        _syncState.value = SyncState.SYNCING
        return action().also {
            _syncState.value = if (it.isSuccess) SyncState.IDLE else SyncState.ERROR
        }
    }

    private suspend fun resolveRoutineId(time: RoutineTime): String {
        val existing = routineDao.getAll().first().find { it.timeOfDay == time.ordinal }
        if (existing != null) return existing.id

        val entity = RoutineEntity(
            id = UUID.randomUUID().toString(),
            title = time.name.lowercase().replaceFirstChar { it.uppercase() },
            timeOfDay = time.ordinal,
        )
        routineDao.insert(entity)
        return entity.id
    }
}
