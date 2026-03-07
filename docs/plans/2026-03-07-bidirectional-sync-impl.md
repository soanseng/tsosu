# Bidirectional Vikunja Sync Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make every local task mutation (create, update, toggle done, delete) push to Vikunja immediately with queue-based retry, and pull from server on app resume.

**Architecture:** Hybrid push sync — immediate API call on mutation, fallback to `sync_queue` table on failure, WorkManager drains queue periodically. Pull on app foreground resume. Last-write-wins conflict resolution.

**Tech Stack:** Room (SyncQueueDao), WorkManager (`work-runtime-ktx` already in deps), Hilt DI, kotlinx-datetime for ISO 8601 conversion, Retrofit (VikunjaApi)

---

### Task 1: Add `getByIdSync` to TaskDao

We need a suspend (non-Flow) method to read a single task for push sync.

**Files:**
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/dao/TaskDao.kt`

**Step 1: Add the query method**

Add after line 66 (`getByServerId`):

```kotlin
@Query("SELECT * FROM tasks WHERE id = :taskId")
suspend fun getByIdSync(taskId: String): TaskEntity?
```

**Step 2: Build to verify**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-local:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add data-local/src/main/kotlin/app/tsosu/data/local/dao/TaskDao.kt
git commit -m "feat(dao): add getByIdSync suspend query to TaskDao"
```

---

### Task 2: Create SyncQueueDao

DAO for the existing `sync_queue` Room table. Includes insert, getAll, delete, retry increment, deduplication check, and stale cleanup.

**Files:**
- Create: `data-local/src/main/kotlin/app/tsosu/data/local/dao/SyncQueueDao.kt`
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/TsosuDatabase.kt`

**Step 1: Create the DAO**

Create `data-local/src/main/kotlin/app/tsosu/data/local/dao/SyncQueueDao.kt`:

```kotlin
package app.tsosu.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.tsosu.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {

    @Insert
    suspend fun insert(entry: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun findByEntity(entityType: String, entityId: String): SyncQueueEntity?

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun incrementRetry(id: Long, error: String)

    @Query("DELETE FROM sync_queue WHERE retryCount >= :maxRetries")
    suspend fun deleteStale(maxRetries: Int = 5): Int

    @Query("UPDATE sync_queue SET operation = :operation WHERE id = :id")
    suspend fun updateOperation(id: Long, operation: String)
}
```

**Step 2: Expose DAO from TsosuDatabase**

In `data-local/src/main/kotlin/app/tsosu/data/local/TsosuDatabase.kt`, add after line 40 (`abstract fun labelDao(): LabelDao`):

```kotlin
abstract fun syncQueueDao(): SyncQueueDao
```

Also add import at top:
```kotlin
import app.tsosu.data.local.dao.SyncQueueDao
```

**Step 3: Provide DAO from DatabaseModule**

In `app/src/main/java/app/tsosu/di/DatabaseModule.kt`, add after line 34 (`provideLabelDao`):

```kotlin
@Provides fun provideSyncQueueDao(db: TsosuDatabase): SyncQueueDao = db.syncQueueDao()
```

Also add import:
```kotlin
import app.tsosu.data.local.dao.SyncQueueDao
```

**Step 4: Build to verify**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-local:compileDebugKotlin && ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add data-local/src/main/kotlin/app/tsosu/data/local/dao/SyncQueueDao.kt \
       data-local/src/main/kotlin/app/tsosu/data/local/TsosuDatabase.kt \
       app/src/main/java/app/tsosu/di/DatabaseModule.kt
git commit -m "feat(sync): add SyncQueueDao for push sync queue"
```

---

### Task 3: Fix due date round-tripping in SyncManager and VikunjaTaskMapper

Currently both pull and push drop due dates (TODO at SyncManager lines 58 and 135). Add ISO 8601 <-> epoch millis conversion.

**Files:**
- Modify: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/mapper/VikunjaTaskMapper.kt`
- Modify: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncManager.kt`
- Modify: `data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/mapper/VikunjaTaskMapperTest.kt`

**Step 1: Add date conversion methods to VikunjaTaskMapper**

Add to `VikunjaTaskMapper` class:

```kotlin
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun parseDueDate(iso: String?): Long? {
    if (iso.isNullOrBlank() || iso == "0001-01-01T00:00:00Z") return null
    return try {
        Instant.parse(iso).toEpochMilliseconds()
    } catch (e: Exception) {
        null
    }
}

fun formatDueDate(millis: Long?): String? {
    if (millis == null) return null
    return Instant.fromEpochMilliseconds(millis).toString()
}
```

Note: Vikunja uses `"0001-01-01T00:00:00Z"` as the "no date" sentinel, so we treat that as null.

**Step 2: Wire into SyncManager pull**

In `SyncManager.upsertTaskFromDto()`, replace line 58:
```kotlin
dueDate = null, // TODO: parse ISO date from dto.dueDate
```
with:
```kotlin
dueDate = taskMapper.parseDueDate(dto.dueDate),
```

**Step 3: Wire into SyncManager push**

In `SyncManager.pushTask()`, replace line 135:
```kotlin
dueDate = null, // TODO: format ISO date from entity.dueDate
```
with:
```kotlin
dueDate = taskMapper.formatDueDate(entity.dueDate),
```

**Step 4: Add tests for date conversion**

Add to `data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/mapper/VikunjaTaskMapperTest.kt`:

```kotlin
@Test
fun `parseDueDate converts ISO 8601 to epoch millis`() {
    val mapper = VikunjaTaskMapper()
    val millis = mapper.parseDueDate("2026-03-10T00:00:00Z")
    assertNotNull(millis)
    assertEquals(1773100800000L, millis) // 2026-03-10T00:00:00Z
}

@Test
fun `parseDueDate returns null for Vikunja zero date`() {
    val mapper = VikunjaTaskMapper()
    assertNull(mapper.parseDueDate("0001-01-01T00:00:00Z"))
}

@Test
fun `parseDueDate returns null for null or blank`() {
    val mapper = VikunjaTaskMapper()
    assertNull(mapper.parseDueDate(null))
    assertNull(mapper.parseDueDate(""))
}

@Test
fun `formatDueDate converts epoch millis to ISO 8601`() {
    val mapper = VikunjaTaskMapper()
    val iso = mapper.formatDueDate(1773100800000L)
    assertEquals("2026-03-10T00:00:00Z", iso)
}

@Test
fun `formatDueDate returns null for null input`() {
    val mapper = VikunjaTaskMapper()
    assertNull(mapper.formatDueDate(null))
}
```

**Step 5: Run tests**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:test
```
Expected: All tests PASS

**Step 6: Commit**

```bash
git add data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/mapper/VikunjaTaskMapper.kt \
       data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncManager.kt \
       data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/mapper/VikunjaTaskMapperTest.kt
git commit -m "feat(sync): add due date ISO 8601 round-tripping for pull and push"
```

---

### Task 4: Add `deleteTask` to SyncManager

SyncManager can push (create/update) but not delete. Add delete support.

**Files:**
- Modify: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncManager.kt`

**Step 1: Add deleteTask method**

Add after `pushTask()` method (after line 159):

```kotlin
suspend fun deleteTask(serverId: Long) {
    api.deleteTask(serverId)
}
```

**Step 2: Build to verify**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncManager.kt
git commit -m "feat(sync): add deleteTask to SyncManager"
```

---

### Task 5: Create SyncOperation enum and SyncDispatcher

The core orchestrator: tries immediate push, falls back to queue on failure. Handles deduplication.

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncOperation.kt`
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncDispatcher.kt`
- Create: `data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/sync/SyncDispatcherTest.kt`

**Step 1: Create SyncOperation enum**

Create `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncOperation.kt`:

```kotlin
package app.tsosu.data.vikunja.sync

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE,
}
```

**Step 2: Create SyncDispatcher**

Create `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncDispatcher.kt`:

```kotlin
package app.tsosu.data.vikunja.sync

import android.util.Log
import app.tsosu.data.local.dao.SyncQueueDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.SyncQueueEntity
import kotlinx.datetime.Clock

class SyncDispatcher(
    private val syncManagerProvider: () -> SyncManager?,
    private val syncQueueDao: SyncQueueDao,
    private val taskDao: TaskDao,
) {
    suspend fun dispatch(entityId: String, operation: SyncOperation, serverId: Long? = null) {
        val syncManager = syncManagerProvider() ?: return // not configured

        try {
            when (operation) {
                SyncOperation.CREATE, SyncOperation.UPDATE -> {
                    val entity = taskDao.getByIdSync(entityId) ?: return
                    syncManager.pushTask(entity)
                }
                SyncOperation.DELETE -> {
                    val sid = serverId ?: return // no server ID means never synced
                    syncManager.deleteTask(sid)
                }
            }
        } catch (e: Exception) {
            Log.w("SyncDispatcher", "Immediate push failed, enqueuing: ${e.message}")
            enqueue(entityId, operation, serverId)
        }
    }

    private suspend fun enqueue(entityId: String, operation: SyncOperation, serverId: Long?) {
        val existing = syncQueueDao.findByEntity("task", entityId)
        if (existing != null) {
            // Deduplication logic
            when {
                operation == SyncOperation.DELETE -> {
                    // Any previous op + DELETE = just DELETE
                    syncQueueDao.updateOperation(existing.id, SyncOperation.DELETE.name)
                }
                existing.operation == SyncOperation.CREATE.name && operation == SyncOperation.UPDATE -> {
                    // CREATE + UPDATE = keep CREATE (will send full entity)
                }
                else -> {
                    syncQueueDao.updateOperation(existing.id, operation.name)
                }
            }
        } else {
            syncQueueDao.insert(
                SyncQueueEntity(
                    entityType = "task",
                    entityId = entityId,
                    operation = operation.name,
                    payload = serverId?.toString() ?: "",
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                )
            )
        }
    }

    suspend fun drainQueue() {
        val syncManager = syncManagerProvider() ?: return
        syncQueueDao.deleteStale(5)
        val entries = syncQueueDao.getAll()

        for (entry in entries) {
            try {
                val op = SyncOperation.valueOf(entry.operation)
                when (op) {
                    SyncOperation.CREATE, SyncOperation.UPDATE -> {
                        val entity = taskDao.getByIdSync(entry.entityId)
                        if (entity != null) {
                            syncManager.pushTask(entity)
                        }
                    }
                    SyncOperation.DELETE -> {
                        val sid = entry.payload.toLongOrNull()
                        if (sid != null) {
                            syncManager.deleteTask(sid)
                        }
                    }
                }
                syncQueueDao.delete(entry.id)
            } catch (e: Exception) {
                Log.w("SyncDispatcher", "Queue drain failed for ${entry.id}: ${e.message}")
                syncQueueDao.incrementRetry(entry.id, e.message ?: "unknown")
            }
        }
    }
}
```

**Step 3: Write tests**

Create `data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/sync/SyncDispatcherTest.kt`:

```kotlin
package app.tsosu.data.vikunja.sync

import app.tsosu.data.local.dao.SyncQueueDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.SyncQueueEntity
import app.tsosu.data.local.entity.TaskEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SyncDispatcherTest {

    private val syncManager = mockk<SyncManager>(relaxed = true)
    private val syncQueueDao = mockk<SyncQueueDao>(relaxed = true)
    private val taskDao = mockk<TaskDao>(relaxed = true)

    private val dispatcher = SyncDispatcher(
        syncManagerProvider = { syncManager },
        syncQueueDao = syncQueueDao,
        taskDao = taskDao,
    )

    private val testEntity = TaskEntity(
        id = "task-1",
        serverId = 42L,
        title = "Test",
        createdAt = 1000L,
        updatedAt = 2000L,
    )

    @Test
    fun `dispatch UPDATE pushes immediately on success`() = runTest {
        coEvery { taskDao.getByIdSync("task-1") } returns testEntity

        dispatcher.dispatch("task-1", SyncOperation.UPDATE)

        coVerify(exactly = 1) { syncManager.pushTask(testEntity) }
        coVerify(exactly = 0) { syncQueueDao.insert(any()) }
    }

    @Test
    fun `dispatch enqueues on push failure`() = runTest {
        coEvery { taskDao.getByIdSync("task-1") } returns testEntity
        coEvery { syncManager.pushTask(any()) } throws RuntimeException("network error")
        coEvery { syncQueueDao.findByEntity("task", "task-1") } returns null

        dispatcher.dispatch("task-1", SyncOperation.UPDATE)

        coVerify(exactly = 1) { syncQueueDao.insert(match { it.entityId == "task-1" && it.operation == "UPDATE" }) }
    }

    @Test
    fun `dispatch DELETE calls deleteTask with serverId`() = runTest {
        dispatcher.dispatch("task-1", SyncOperation.DELETE, serverId = 42L)

        coVerify(exactly = 1) { syncManager.deleteTask(42L) }
    }

    @Test
    fun `dispatch does nothing when syncManager is null`() = runTest {
        val noSyncDispatcher = SyncDispatcher(
            syncManagerProvider = { null },
            syncQueueDao = syncQueueDao,
            taskDao = taskDao,
        )

        noSyncDispatcher.dispatch("task-1", SyncOperation.UPDATE)

        coVerify(exactly = 0) { syncManager.pushTask(any()) }
        coVerify(exactly = 0) { syncQueueDao.insert(any()) }
    }

    @Test
    fun `dedup replaces existing op with DELETE`() = runTest {
        coEvery { taskDao.getByIdSync("task-1") } returns testEntity
        coEvery { syncManager.pushTask(any()) } throws RuntimeException("fail")
        coEvery { syncQueueDao.findByEntity("task", "task-1") } returns SyncQueueEntity(
            id = 10, entityType = "task", entityId = "task-1",
            operation = "UPDATE", payload = "", createdAt = 1000L,
        )

        dispatcher.dispatch("task-1", SyncOperation.DELETE, serverId = 42L)

        coVerify { syncQueueDao.updateOperation(10, "DELETE") }
        coVerify(exactly = 0) { syncQueueDao.insert(any()) }
    }

    @Test
    fun `drainQueue processes and removes entries`() = runTest {
        val entry = SyncQueueEntity(
            id = 1, entityType = "task", entityId = "task-1",
            operation = "UPDATE", payload = "", createdAt = 1000L,
        )
        coEvery { syncQueueDao.getAll() } returns listOf(entry)
        coEvery { taskDao.getByIdSync("task-1") } returns testEntity

        dispatcher.drainQueue()

        coVerify(exactly = 1) { syncManager.pushTask(testEntity) }
        coVerify(exactly = 1) { syncQueueDao.delete(1) }
    }
}
```

**Step 4: Run tests**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-vikunja:test
```
Expected: All tests PASS

**Step 5: Commit**

```bash
git add data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncOperation.kt \
       data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncDispatcher.kt \
       data-vikunja/src/test/kotlin/app/tsosu/data/vikunja/sync/SyncDispatcherTest.kt
git commit -m "feat(sync): add SyncDispatcher with immediate push and queue fallback"
```

---

### Task 6: Wire SyncDispatcher into TaskRepositoryImpl

Modify TaskRepositoryImpl to accept an optional SyncDispatcher and call it after every mutation.

**Files:**
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/repository/TaskRepositoryImpl.kt`

**Step 1: Add SyncDispatcher parameter and dispatch calls**

The SyncDispatcher lives in `data-vikunja` module, but `TaskRepositoryImpl` is in `data-local` which does not depend on `data-vikunja`. To avoid a circular dependency, we'll use a function type interface.

Change the constructor and mutation methods:

```kotlin
class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val onTaskChanged: (suspend (entityId: String, operation: String, serverId: Long?) -> Unit)? = null,
) : TaskRepository {
```

Then add dispatch calls at the end of each mutation method:

In `createTask()` (after `taskDao.insert`):
```kotlin
onTaskChanged?.invoke(task.id, "CREATE", null)
```

In `updateTask()` (after `taskDao.update`):
```kotlin
onTaskChanged?.invoke(updated.id, "UPDATE", null)
```

In `deleteTask()` — need to read serverId BEFORE deleting:
```kotlin
override suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
    val serverId = taskDao.getByIdSync(taskId)?.serverId
    taskDao.delete(taskId)
    onTaskChanged?.invoke(taskId, "DELETE", serverId)
}
```

In `toggleDone()` (after `taskDao.setDone`):
```kotlin
onTaskChanged?.invoke(taskId, "UPDATE", null)
```

In `setFocus()` (after `taskDao.setFocus`):
```kotlin
onTaskChanged?.invoke(taskId, "UPDATE", null)
```

In `reorder()` (after `taskDao.update`):
```kotlin
onTaskChanged?.invoke(taskId, "UPDATE", null)
```

**Step 2: Build to verify**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-local:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add data-local/src/main/kotlin/app/tsosu/data/local/repository/TaskRepositoryImpl.kt
git commit -m "feat(sync): add onTaskChanged callback to TaskRepositoryImpl for push sync"
```

---

### Task 7: Wire DI — connect SyncDispatcher to TaskRepositoryImpl

Update Hilt modules to create SyncDispatcher and pass it to TaskRepositoryImpl.

**Files:**
- Modify: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/di/VikunjaModule.kt`
- Modify: `app/src/main/java/app/tsosu/di/RepositoryModule.kt`

**Step 1: Provide SyncDispatcher from VikunjaModule**

Add to `VikunjaModule.kt`:

```kotlin
@Provides
@Singleton
fun provideSyncDispatcher(
    credentialStore: VikunjaCredentialStore,
    syncQueueDao: SyncQueueDao,
    taskDao: TaskDao,
    projectDao: ProjectDao,
    labelDao: LabelDao,
    taskMapper: VikunjaTaskMapper,
): SyncDispatcher {
    return SyncDispatcher(
        syncManagerProvider = {
            val url = kotlinx.coroutines.runBlocking { credentialStore.getServerUrl() } ?: return@SyncDispatcher null
            val token = kotlinx.coroutines.runBlocking { credentialStore.getToken() } ?: return@SyncDispatcher null
            val api = app.tsosu.data.vikunja.api.VikunjaApiProvider.create(url) { token }
            SyncManager(api, taskDao, projectDao, labelDao, EnergyLabelManager(api), taskMapper)
        },
        syncQueueDao = syncQueueDao,
        taskDao = taskDao,
    )
}
```

Add required imports:
```kotlin
import app.tsosu.data.local.dao.SyncQueueDao
import app.tsosu.data.vikunja.sync.SyncDispatcher
```

**Step 2: Update RepositoryModule to inject SyncDispatcher**

In `app/src/main/java/app/tsosu/di/RepositoryModule.kt`, change `provideTaskRepository`:

```kotlin
@Provides
@Singleton
fun provideTaskRepository(taskDao: TaskDao, syncDispatcher: SyncDispatcher): TaskRepository =
    TaskRepositoryImpl(taskDao) { entityId, operation, serverId ->
        val op = app.tsosu.data.vikunja.sync.SyncOperation.valueOf(operation)
        syncDispatcher.dispatch(entityId, op, serverId)
    }
```

Add imports:
```kotlin
import app.tsosu.data.vikunja.sync.SyncDispatcher
```

**Step 3: Build to verify**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/di/VikunjaModule.kt \
       app/src/main/java/app/tsosu/di/RepositoryModule.kt
git commit -m "feat(sync): wire SyncDispatcher into DI and TaskRepositoryImpl"
```

---

### Task 8: Update SyncRepositoryImpl to push before pull

Modify `sync()` to drain the push queue before pulling from server.

**Files:**
- Modify: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/repository/SyncRepositoryImpl.kt`

**Step 1: Add SyncDispatcher parameter and push-before-pull**

Update constructor to accept `SyncDispatcher`:

```kotlin
class SyncRepositoryImpl(
    private val credentialStore: VikunjaCredentialStore,
    private val syncManagerFactory: (VikunjaApi) -> SyncManager,
    private val energyLabelManagerFactory: (VikunjaApi) -> EnergyLabelManager,
    private val syncDispatcher: SyncDispatcher,
) : SyncRepository {
```

Update `sync()` method — add queue drain before pull:

```kotlin
override suspend fun sync(): Result<SyncResult> {
    val api = getOrRestoreApi()
        ?: return Result.failure(IllegalStateException("Not configured"))

    _syncState.value = SyncState.SYNCING
    return try {
        // Push local changes first
        syncDispatcher.drainQueue()

        val energyManager = energyLabelManagerFactory(api)
        energyManager.ensureLabelsExist()

        val syncManager = syncManagerFactory(api)
        val pulledProjects = syncManager.pullProjects()
        syncManager.pullLabels()
        val pulledTasks = syncManager.pullTasks()

        _syncState.value = SyncState.IDLE
        Result.success(SyncResult(pushed = 0, pulled = pulledTasks + pulledProjects, conflicts = 0))
    } catch (e: Exception) {
        _syncState.value = SyncState.ERROR
        Result.failure(e)
    }
}
```

**Step 2: Update VikunjaModule to pass SyncDispatcher to SyncRepositoryImpl**

In `VikunjaModule.provideSyncRepository()`, add `syncDispatcher` parameter:

```kotlin
@Provides
@Singleton
fun provideSyncRepository(
    credentialStore: VikunjaCredentialStore,
    taskDao: TaskDao,
    projectDao: ProjectDao,
    labelDao: LabelDao,
    taskMapper: VikunjaTaskMapper,
    syncDispatcher: SyncDispatcher,
): SyncRepository {
    return SyncRepositoryImpl(
        credentialStore = credentialStore,
        syncManagerFactory = { api ->
            SyncManager(api, taskDao, projectDao, labelDao, EnergyLabelManager(api), taskMapper)
        },
        energyLabelManagerFactory = { api -> EnergyLabelManager(api) },
        syncDispatcher = syncDispatcher,
    )
}
```

**Step 3: Build to verify**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/repository/SyncRepositoryImpl.kt \
       data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/di/VikunjaModule.kt
git commit -m "feat(sync): push queue drain before pull in SyncRepositoryImpl"
```

---

### Task 9: Create SyncWorker (WorkManager periodic worker)

Periodic background worker that drains the sync queue.

**Files:**
- Create: `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncWorker.kt`

**Step 1: Create the worker**

Create `data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncWorker.kt`:

```kotlin
package app.tsosu.data.vikunja.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncDispatcher: SyncDispatcher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            syncDispatcher.drainQueue()
            Result.success()
        } catch (e: Exception) {
            Log.w("SyncWorker", "Queue drain failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "tsosu_sync_worker"
    }
}
```

**Step 2: Add work-hilt dependency**

The `@HiltWorker` annotation requires `androidx.hilt:hilt-work`. Check if it's already in deps.

Add to `gradle/libs.versions.toml` in `[libraries]` section:
```toml
hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hilt-navigation-compose" }
```

Add to `data-vikunja/build.gradle.kts` dependencies:
```kotlin
implementation(libs.hilt.work)
```

Also add to `app/build.gradle.kts` dependencies:
```kotlin
implementation(libs.hilt.work)
```

**Step 3: Build to verify**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add data-vikunja/src/main/kotlin/app/tsosu/data/vikunja/sync/SyncWorker.kt \
       gradle/libs.versions.toml \
       data-vikunja/build.gradle.kts \
       app/build.gradle.kts
git commit -m "feat(sync): add SyncWorker periodic background queue drain"
```

---

### Task 10: Register SyncWorker and add resume-pull to MainActivity

Schedule the WorkManager periodic worker on app start. Trigger pull on resume.

**Files:**
- Modify: `app/src/main/java/app/tsosu/MainActivity.kt`

**Step 1: Add WorkManager scheduling and resume pull**

Add imports:
```kotlin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.tsosu.data.vikunja.sync.SyncWorker
import app.tsosu.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
```

Add SyncRepository injection:
```kotlin
@Inject lateinit var syncRepository: SyncRepository
```

In `onCreate`, after `enableEdgeToEdge()`, add WorkManager scheduling:

```kotlin
scheduleSyncWorker()
setupResumePull()
```

Add these methods to `MainActivity`:

```kotlin
private fun scheduleSyncWorker() {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .build()
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        SyncWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}

private var lastSyncTime = 0L

private fun setupResumePull() {
    lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            val now = System.currentTimeMillis()
            if (now - lastSyncTime > 30_000) { // 30 second cooldown
                lastSyncTime = now
                CoroutineScope(Dispatchers.IO).launch {
                    val isConfigured = syncRepository.isRemoteConfigured().first()
                    if (isConfigured) {
                        syncRepository.sync()
                    }
                }
            }
        }
    })
}
```

**Step 2: Initialize WorkManager with HiltWorkerFactory**

Create `app/src/main/java/app/tsosu/TsosuApplication.kt` (if it doesn't already exist):

```kotlin
package app.tsosu

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TsosuApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

Check if `TsosuApplication` already exists — if it does, just add the `Configuration.Provider` implementation.

Also verify `AndroidManifest.xml` has `android:name=".TsosuApplication"` on the `<application>` tag. If not, add it.

Disable default WorkManager initializer in `AndroidManifest.xml` by adding inside `<application>`:
```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:authorities="${applicationId}"
        tools:node="remove" />
</provider>
```

**Step 3: Build to verify**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add app/src/main/java/app/tsosu/MainActivity.kt \
       app/src/main/java/app/tsosu/TsosuApplication.kt \
       app/src/main/AndroidManifest.xml
git commit -m "feat(sync): register SyncWorker and pull on app resume"
```

---

### Task 11: Run all tests and verify build

Final verification that everything compiles and all existing tests still pass.

**Step 1: Run all tests**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew test
```
Expected: All tests PASS

**Step 2: Build debug APK**

```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug
```
Expected: BUILD SUCCESSFUL

**Step 3: Verify no regressions**

Check that domain tests still pass:
```bash
ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :domain:test
```
Expected: All 11 tests PASS
