package app.tsosu.data.vikunja.sync

import app.tsosu.data.local.dao.SyncQueueDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.SyncQueueEntity
import kotlinx.datetime.Clock
import java.util.logging.Logger

class SyncDispatcher(
    private val syncManagerProvider: () -> SyncManager?,
    private val syncQueueDao: SyncQueueDao,
    private val taskDao: TaskDao,
) {
    private val logger = Logger.getLogger("SyncDispatcher")
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
            logger.warning( "Immediate push failed, enqueuing: ${e.message}")
            enqueue(entityId, operation, serverId)
        }
    }

    private suspend fun enqueue(entityId: String, operation: SyncOperation, serverId: Long?) {
        val existing = syncQueueDao.findByEntity("task", entityId)
        if (existing != null) {
            when {
                operation == SyncOperation.DELETE -> {
                    syncQueueDao.updateOperation(existing.id, SyncOperation.DELETE.name)
                }
                existing.operation == SyncOperation.CREATE.name && operation == SyncOperation.UPDATE -> {
                    // CREATE + UPDATE = keep CREATE
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
                logger.warning( "Queue drain failed for ${entry.id}: ${e.message}")
                syncQueueDao.incrementRetry(entry.id, e.message ?: "unknown")
            }
        }
    }
}
