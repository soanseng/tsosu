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
        coEvery { syncManager.deleteTask(any()) } throws RuntimeException("fail")
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
