package app.tsosu.data.markdown

import app.tsosu.domain.repository.SyncResult
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Single entry point for triggering markdown-vault syncs.
 *
 * - [onChange] fires on any vault file change (ContentObserver); debounced so
 *   bursts of writes (e.g. Obsidian writing several files) coalesce into one sync.
 * - [syncOnce] runs an immediate, guarded sync (manual triggers).
 * - [syncNow] is the fire-and-forget variant of [syncOnce] with a result callback.
 *
 * Re-entrancy guard: while a sync is running (which itself writes vault files),
 * further triggers are ignored — this prevents observer feedback loops.
 * Consequence: an external change arriving mid-sync is dropped until the next
 * change event or app resume (which pulls again via [syncOnce]).
 */
class VaultChangeCoordinator(
    private val scope: CoroutineScope,
    private val syncAction: suspend () -> Result<SyncResult>,
    private val debounceMillis: Long = 2_000,
) {
    private val syncing = AtomicBoolean(false)
    private var pendingJob: Job? = null

    fun onChange() {
        if (syncing.get()) return
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(debounceMillis)
            runSync()
        }
    }

    suspend fun syncOnce(): Result<SyncResult> = runSync()

    fun syncNow(onComplete: (Result<SyncResult>) -> Unit = {}): Job =
        scope.launch { onComplete(runSync()) }

    private suspend fun runSync(): Result<SyncResult> {
        if (!syncing.compareAndSet(false, true)) {
            return Result.failure(IllegalStateException("Sync already in progress"))
        }
        return try {
            syncAction()
        } finally {
            syncing.set(false)
        }
    }
}
