package app.tsosu.data.markdown

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SyncQueueTest {

    @Test
    fun `bursts within debounce coalesce into one action`() = runTest {
        val queue = SyncQueue(this, debounceMillis = 3_000)
        var runs = 0

        repeat(5) {
            queue.enqueueDebounced { runs++ }
            advanceTimeBy(1_000)
        }
        advanceTimeBy(3_000)
        advanceUntilIdle()

        assertEquals(1, runs, "Five enqueues inside the debounce window must run the action once")
    }

    @Test
    fun `enqueue after debounce fires runs earlier action`() = runTest {
        val queue = SyncQueue(this, debounceMillis = 3_000)
        var runs = 0

        queue.enqueueDebounced { runs++ }
        advanceTimeBy(3_000)
        advanceUntilIdle()
        assertEquals(1, runs)

        queue.enqueueDebounced { runs++ }
        advanceTimeBy(3_000)
        advanceUntilIdle()
        assertEquals(2, runs, "A later enqueue after the window ran must fire again")
    }

    @Test
    fun `run serializes concurrent actions`() = runTest {
        val queue = SyncQueue(this)
        val log = mutableListOf<String>()

        val first = async {
            queue.run {
                delay(100)
                log.add("first-start")
                delay(100)
                log.add("first-end")
            }
        }
        val second = async {
            queue.run {
                log.add("second-start")
                log.add("second-end")
            }
        }

        first.await()
        second.await()

        assertEquals(listOf("first-start", "first-end", "second-start", "second-end"), log)
        assertTrue(true)
    }

    @Test
    fun `debounced action runs under the mutex`() = runTest {
        val queue = SyncQueue(this, debounceMillis = 1_000)
        val log = mutableListOf<String>()

        val held = launch {
            queue.run {
                log.add("long-start")
                delay(5_000)
                log.add("long-end")
            }
        }
        advanceTimeBy(100)

        queue.enqueueDebounced { log.add("debounced") }
        advanceTimeBy(1_000)
        advanceUntilIdle()
        held.join()

        assertEquals(listOf("long-start", "long-end", "debounced"), log)
    }
}
