package app.tsosu.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskStatusTest {

    @Test
    fun `all statuses have unique checkbox markers`() {
        val markers = TaskStatus.entries.map { it.checkboxMarker }
        assertEquals(markers.size, markers.toSet().size, "All markers must be unique")
    }

    @Test
    fun `fromCheckboxChar parses all known markers`() {
        assertEquals(TaskStatus.TODO, TaskStatus.fromCheckboxChar(' '))
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.fromCheckboxChar('/'))
        assertEquals(TaskStatus.ON_HOLD, TaskStatus.fromCheckboxChar('!'))
        assertEquals(TaskStatus.PLANNED, TaskStatus.fromCheckboxChar('>'))
        assertEquals(TaskStatus.DONE, TaskStatus.fromCheckboxChar('x'))
        assertEquals(TaskStatus.DONE, TaskStatus.fromCheckboxChar('X'))
        assertEquals(TaskStatus.CANCELLED, TaskStatus.fromCheckboxChar('-'))
    }

    @Test
    fun `unknown char defaults to TODO`() {
        assertEquals(TaskStatus.TODO, TaskStatus.fromCheckboxChar('?'))
    }

    @Test
    fun `isDone returns true only for DONE`() {
        assertTrue(TaskStatus.DONE.isDone)
        for (status in TaskStatus.entries.filter { it != TaskStatus.DONE }) {
            assertTrue(!status.isDone, "$status should not be done")
        }
    }

    @Test
    fun `isTerminal returns true for DONE and CANCELLED`() {
        assertTrue(TaskStatus.DONE.isTerminal)
        assertTrue(TaskStatus.CANCELLED.isTerminal)
        for (status in TaskStatus.entries.filter { it != TaskStatus.DONE && it != TaskStatus.CANCELLED }) {
            assertTrue(!status.isTerminal, "$status should not be terminal")
        }
    }

    @Test
    fun `ordinal values are sequential for Room storage`() {
        assertEquals(0, TaskStatus.TODO.ordinal)
        assertEquals(1, TaskStatus.IN_PROGRESS.ordinal)
        assertEquals(2, TaskStatus.ON_HOLD.ordinal)
        assertEquals(3, TaskStatus.PLANNED.ordinal)
        assertEquals(4, TaskStatus.DONE.ordinal)
        assertEquals(5, TaskStatus.CANCELLED.ordinal)
    }
}
