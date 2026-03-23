package app.tsosu.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FilterSpecTest {

    private val tasks = listOf(
        Task(
            id = "1", title = "Urgent work", status = TaskStatus.TODO,
            priority = Priority.URGENT, energyLevel = EnergyLevel.HIGH,
            dueDate = LocalDateTime.parse("2026-03-25T09:00:00"),
            projectId = "proj-1",
            createdAt = Instant.parse("2026-03-20T10:00:00Z"),
            updatedAt = Instant.parse("2026-03-20T10:00:00Z"),
        ),
        Task(
            id = "2", title = "Low energy read", status = TaskStatus.IN_PROGRESS,
            priority = Priority.LOW, energyLevel = EnergyLevel.LOW,
            projectId = "proj-2",
            createdAt = Instant.parse("2026-03-21T10:00:00Z"),
            updatedAt = Instant.parse("2026-03-21T10:00:00Z"),
        ),
        Task(
            id = "3", title = "Done task", status = TaskStatus.DONE,
            priority = Priority.MEDIUM,
            createdAt = Instant.parse("2026-03-22T10:00:00Z"),
            updatedAt = Instant.parse("2026-03-22T10:00:00Z"),
        ),
    )

    @Test
    fun `no filters returns all tasks`() {
        val spec = FilterSpec()
        val result = spec.apply(tasks)
        assertEquals(3, result.size)
    }

    @Test
    fun `filter by status`() {
        val spec = FilterSpec(statuses = setOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS))
        val result = spec.apply(tasks)
        assertEquals(2, result.size)
        assertTrue(result.none { it.status == TaskStatus.DONE })
    }

    @Test
    fun `filter by minimum priority`() {
        val spec = FilterSpec(minPriority = Priority.MEDIUM)
        val result = spec.apply(tasks)
        assertEquals(2, result.size) // URGENT + MEDIUM
    }

    @Test
    fun `filter by energy levels`() {
        val spec = FilterSpec(energyLevels = setOf(EnergyLevel.HIGH))
        val result = spec.apply(tasks)
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `filter by due date range`() {
        val spec = FilterSpec(
            dueDateFrom = LocalDate.parse("2026-03-24"),
            dueDateTo = LocalDate.parse("2026-03-26"),
        )
        val result = spec.apply(tasks)
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `filter by project IDs`() {
        val spec = FilterSpec(projectIds = setOf("proj-1"))
        val result = spec.apply(tasks)
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `filter by title search is case-insensitive`() {
        val spec = FilterSpec(titleContains = "READ")
        val result = spec.apply(tasks)
        assertEquals(1, result.size)
        assertEquals("2", result[0].id)
    }

    @Test
    fun `combined AND filters`() {
        val spec = FilterSpec(
            statuses = setOf(TaskStatus.TODO),
            energyLevels = setOf(EnergyLevel.HIGH),
        )
        val result = spec.apply(tasks)
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `combined filters exclude non-matching tasks`() {
        val spec = FilterSpec(
            statuses = setOf(TaskStatus.TODO),
            energyLevels = setOf(EnergyLevel.LOW), // task 1 is HIGH, not LOW
        )
        val result = spec.apply(tasks)
        assertEquals(0, result.size)
    }

    @Test
    fun `sort by priority descending`() {
        val sort = SortSpec(field = SortField.PRIORITY, ascending = false)
        val result = sort.apply(tasks)
        assertEquals(Priority.URGENT, result[0].priority)
        assertEquals(Priority.MEDIUM, result[1].priority)
        assertEquals(Priority.LOW, result[2].priority)
    }

    @Test
    fun `sort by due date ascending with nulls last`() {
        val sort = SortSpec(field = SortField.DUE_DATE, ascending = true)
        val result = sort.apply(tasks)
        assertEquals("1", result[0].id) // has due date 2026-03-25
        // tasks without due date come after
        assertTrue(result[0].dueDate != null)
    }

    @Test
    fun `sort by title alphabetical`() {
        val sort = SortSpec(field = SortField.TITLE, ascending = true)
        val result = sort.apply(tasks)
        assertEquals("Done task", result[0].title)
        assertEquals("Low energy read", result[1].title)
        assertEquals("Urgent work", result[2].title)
    }
}
