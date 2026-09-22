package app.tsosu.data.markdown

import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncResult
import app.tsosu.domain.repository.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MarkdownSyncRepository(
    private val preferences: MarkdownPreferences,
    private val syncManager: MarkdownSyncManager,
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
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

        // 2. Detect conflicts BEFORE the import overwrites the app-side state:
        //    both vault and Room changed since the last export → flag the task.
        val roomBefore = taskDao.getAllTasks().first().map { it.toDomain() }
        pendingConflictIds = conflictDetector.detect(
            importedTasks = importedTasks.tasks,
            appTasks = roomBefore,
            lastExportedHashes = preferences.getTaskHashes(),
        )
        lastImportedCount = importedTasks.tasks.size

        // 3. Merge: upsert imported data into Room (external edits win for
        //    conflicts). Completions are append-only history: the vault only
        //    carries the last HISTORY lines, so union with Room to never
        //    truncate older occurrences.
        val roomById = roomBefore.associateBy { it.id }
        for (task in importedTasks.tasks) {
            val existing = roomById[task.id]
            val merged = if (existing != null && task.completions.isNotEmpty()) {
                task.copy(
                    completions = (task.completions + existing.completions)
                        .distinct()
                        .sorted(),
                )
            } else {
                task
            }
            taskDao.upsert(merged.toEntity())
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
        // Ids we exported last time but no longer have: their notes must go
        // too, or the next import brings the deleted task back.
        val removedTaskIds = preferences.getTaskHashes().keys - tasks.map { it.id }.toSet()
        syncManager.exportTasks(tasks, projectNames, conflictIds, removedTaskIds)

        // Export today's daily note from the recurring-task (habit) series
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val recurring = taskDao.getRecurringTasks().first().map { it.toDomain() }
        val completedToday = recurring
            .filter { today in it.completions }
            .map { it.id }
            .toSet()
        syncManager.exportDailyNote(today, recurring, completedToday)

        // Refresh the last-exported baseline for future conflict detection
        preferences.setTaskHashes(
            tasks.associate { it.id to conflictDetector.serializer.formatTask(it) },
        )
        // Conflict markers were written; don't re-emit them on later pushes.
        pendingConflictIds = emptySet()

        preferences.setLastSync(System.currentTimeMillis())
        tasks.size
    }

    override suspend fun disconnect() {
        preferences.clear()
        _syncState.value = SyncState.IDLE
    }

    override suspend fun removeTaskNote(taskId: String) {
        runCatching { syncManager.removeTaskNote(taskId) }
    }

    private suspend fun <T> wrapSyncState(action: suspend () -> Result<T>): Result<T> {
        _syncState.value = SyncState.SYNCING
        val result = action()
        _syncState.value = if (result.isSuccess) SyncState.IDLE else SyncState.ERROR
        return result
    }
}
