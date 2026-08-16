package app.tsosu.data.markdown

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serialization + debounce helper for vault sync actions.
 *
 * - [run] serializes sync actions so concurrent triggers never interleave
 *   imports and exports.
 * - [enqueueDebounced] coalesces bursts of mutation hooks into a single
 *   deferred action (e.g. three task edits in 3s → one vault write-back).
 */
class SyncQueue(
    private val scope: CoroutineScope,
    private val debounceMillis: Long = 3_000,
) {
    private val mutex = Mutex()
    private var debounceJob: Job? = null

    suspend fun <T> run(action: suspend () -> T): T = mutex.withLock { action() }

    fun enqueueDebounced(action: suspend () -> Unit) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounceMillis)
            run(action)
        }
    }
}
