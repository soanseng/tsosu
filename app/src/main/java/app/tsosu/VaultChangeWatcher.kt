package app.tsosu

import app.tsosu.data.markdown.SyncQueue
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncResult
import app.tsosu.notification.ReminderResync
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Single funnel for vault sync actions. The app reads the vault on resume
 * ([pullOnce]), writes it back after a full round-trip ([syncOnce] — folder
 * setup, manual sync, Todoist import), and flushes local mutations a few
 * seconds after they happen ([pushSoon]). There is no background observer:
 * external edits are picked up on the next app resume.
 */
@Singleton
class VaultChangeWatcher @Inject constructor(
    private val syncRepository: SyncRepository,
    private val reminderResync: ReminderResync,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val queue = SyncQueue(scope)

    /** Import external vault edits into Room; external edits win. */
    suspend fun pullOnce(): Result<Unit> = queue.run {
        syncRepository.pull()
    }.also {
        if (it.isSuccess) reminderResync.afterSync()
    }

    /** Full round-trip: pull then push (folder setup, manual sync, import). */
    suspend fun syncOnce(): Result<SyncResult> = queue.run {
        syncRepository.sync()
    }.also {
        if (it.isSuccess) reminderResync.afterSync()
    }

    /** Debounced export of local mutations to the vault. */
    fun pushSoon() {
        queue.enqueueDebounced { syncRepository.push() }
    }
}
