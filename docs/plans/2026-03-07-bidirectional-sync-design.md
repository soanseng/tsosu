# Bidirectional Vikunja Sync Design Document

**Date:** 2026-03-07
**Status:** Approved

## Goals

Add push sync so every local mutation (create, update, toggle done, delete) syncs to Vikunja immediately, with queue-based retry on failure. Pull happens on app foreground resume.

## Architecture

```
                     immediate push
TaskRepositoryImpl ──────────────────► SyncDispatcher ──► SyncManager ──► Vikunja API
  (Room write)          on failure           │
                        enqueue              ▼
                                       SyncQueueDao
                                       (sync_queue)
                                             ▲
                                             │
App foreground resume ──► sync()        SyncWorker
                          push+pull     (WorkManager, drain queue)
```

### Key Components

1. **SyncQueueDao** — New DAO for the existing `sync_queue` Room table
2. **SyncDispatcher** — Orchestrates immediate push + queue fallback
3. **SyncWorker** — WorkManager periodic worker that drains the queue
4. **TaskRepositoryImpl** — Modified to call SyncDispatcher after every mutation
5. **MainActivity** — Triggers pull sync on resume via Lifecycle.Event.ON_RESUME

### Conflict Strategy

Last-write-wins based on `updatedAt` timestamp. Single user on self-hosted instance makes real conflicts rare.

## SyncQueue & SyncDispatcher

### SyncQueueDao

CRUD for the existing `sync_queue` table:
- `insert(entry)` — enqueue a pending operation
- `getAll()` — fetch all pending entries ordered by `createdAt`
- `delete(id)` — remove after successful push
- `incrementRetry(id, error)` — bump `retryCount`, store last error
- `deleteStale(maxRetries)` — purge entries that exceeded retry limit (default: 5)

### SyncQueueEntry Fields

- `entityType` = `"task"` (extensible to `"project"` later)
- `operation` = `"CREATE"` | `"UPDATE"` | `"DELETE"`
- `entityId` = local task UUID
- `payload` = JSON with `serverId` for DELETE operations; empty otherwise (entity read fresh from Room at push time)

### SyncDispatcher

Injected into TaskRepositoryImpl:

```kotlin
class SyncDispatcher(
    private val syncManager: SyncManager,
    private val syncQueueDao: SyncQueueDao,
    private val taskDao: TaskDao,
) {
    suspend fun dispatch(entityId: String, operation: SyncOperation) {
        try {
            when (operation) {
                CREATE, UPDATE -> {
                    val entity = taskDao.getByIdSync(entityId) ?: return
                    syncManager.pushTask(entity)
                }
                DELETE -> {
                    // serverId must be captured before Room deletion
                    syncManager.deleteTask(serverId)
                }
            }
        } catch (e: Exception) {
            syncQueueDao.insert(SyncQueueEntity(
                entityType = "task",
                entityId = entityId,
                operation = operation.name,
                payload = "",  // or JSON with serverId for DELETE
                createdAt = now,
            ))
        }
    }
}
```

### Deduplication

Before enqueuing, check if same `entityId` + `entityType` already exists in queue:
- CREATE then UPDATE -> keep CREATE
- Any then DELETE -> replace with DELETE
- UPDATE then UPDATE -> keep single UPDATE

## TaskRepositoryImpl Changes

After each successful Room write, call `SyncDispatcher.dispatch()`:

| TaskRepositoryImpl method | SyncOperation |
|---------------------------|---------------|
| `createTask()` | CREATE |
| `updateTask()` | UPDATE |
| `toggleDone()` | UPDATE |
| `deleteTask()` | DELETE |
| `setFocus()` | UPDATE |
| `reorder()` | UPDATE |

SyncDispatcher is optional (`SyncDispatcher?`). When Vikunja is not configured, it's null (zero overhead).

For DELETE: capture `serverId` before Room deletion and pass to SyncDispatcher. Store `serverId` in queue `payload` field for retry.

## SyncWorker & App Resume Pull

### SyncWorker (WorkManager)

- Periodic worker, minimum interval 15 minutes
- Constraint: requires network connectivity
- Drains all entries from `sync_queue`, oldest first
- For each entry: read entity from Room -> push -> delete from queue on success -> increment retry on failure
- Entries with `retryCount >= 5` are purged with warning log
- Registered at app startup if Vikunja is configured; cancelled on disconnect

### App Resume Pull

- `MainActivity` observes `Lifecycle.Event.ON_RESUME`
- If Vikunja is configured and last sync was >30 seconds ago, triggers `SyncRepository.sync()`
- `lastSyncTimestamp` stored in DataStore prevents excessive pulls

### Sync Order in SyncRepositoryImpl.sync()

1. Drain push queue (local changes go up first)
2. Pull projects
3. Pull labels
4. Pull tasks
5. Update `lastSyncTimestamp`

Push-before-pull ensures local changes aren't overwritten (supports last-write-wins).

## SyncManager Fixes

### Due Date Round-Tripping

Currently both pull and push drop due dates (TODO comments at lines 58 and 135 of SyncManager.kt).

- Add `parseDueDate(iso: String?): Long?` — ISO 8601 to epoch millis
- Add `formatDueDate(millis: Long?): String?` — epoch millis to ISO 8601
- Wire into `upsertTaskFromDto()` (pull) and `domainToDto()` (push)

### Delete Support

Add `SyncManager.deleteTask(serverId: Long)` calling `api.deleteTask(serverId)`.

## Dependencies

- `androidx.work:work-runtime-ktx` — WorkManager for periodic sync worker
- No other new dependencies needed
