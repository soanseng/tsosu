package app.tsosu.data.markdown

import app.tsosu.domain.repository.SyncResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultChangeCoordinatorTest {

    @Test
    fun `onChange debounces burst into single sync`() = runTest {
        var syncCalls = 0
        val coordinator = VaultChangeCoordinator(
            scope = this,
            debounceMillis = 2_000,
            syncAction = {
                syncCalls++
                Result.success(SyncResult(1, 1))
            },
        )

        coordinator.onChange()
        coordinator.onChange()
        coordinator.onChange()

        advanceTimeBy(1_999)
        assertEquals(0, syncCalls, "Burst should not sync before debounce elapses")

        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, syncCalls, "Burst should coalesce into exactly one sync")
    }

    @Test
    fun `onChange during running sync is ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var syncCalls = 0
        val coordinator = VaultChangeCoordinator(
            scope = this,
            debounceMillis = 2_000,
            syncAction = {
                syncCalls++
                gate.await()
                Result.success(SyncResult(1, 1))
            },
        )

        val first = launch { coordinator.syncOnce() }
        runCurrent()
        assertEquals(1, syncCalls, "syncOnce should start immediately")

        // Our own vault writes fire the observer while the sync is running
        coordinator.onChange()
        advanceTimeBy(2_000)
        runCurrent()

        gate.complete(Unit)
        first.join()
        assertEquals(1, syncCalls, "Change during sync must not trigger a second sync")
    }

    @Test
    fun `concurrent syncOnce fails fast while another sync runs`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var syncCalls = 0
        val coordinator = VaultChangeCoordinator(
            scope = this,
            syncAction = {
                syncCalls++
                gate.await()
                Result.success(SyncResult(1, 1))
            },
        )

        val first = launch { coordinator.syncOnce() }
        runCurrent()
        val second = async { coordinator.syncOnce() }
        val secondResult = second.await()

        assertTrue(secondResult.isFailure, "Concurrent sync must be rejected, not run twice")
        gate.complete(Unit)
        first.join()
        assertEquals(1, syncCalls)
    }

    @Test
    fun `onChange after sync completes triggers a new sync`() = runTest {
        var syncCalls = 0
        val coordinator = VaultChangeCoordinator(
            scope = this,
            debounceMillis = 2_000,
            syncAction = {
                syncCalls++
                Result.success(SyncResult(1, 1))
            },
        )

        coordinator.onChange()
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(1, syncCalls)

        // A later external edit is a fresh change event
        coordinator.onChange()
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(2, syncCalls)
    }
}
