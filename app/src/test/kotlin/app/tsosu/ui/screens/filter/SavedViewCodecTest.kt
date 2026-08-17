package app.tsosu.ui.screens.filter

import android.content.Context
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.FilterSpec
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.SortField
import app.tsosu.domain.model.SortSpec
import app.tsosu.domain.model.TaskStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SavedViewCodecTest {

    private val prefs = SavedViewPreferences(mockk<Context>(relaxed = true))

    @Test
    fun `full spec round trips`() {
        val filter = FilterSpec(
            statuses = setOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS),
            minPriority = Priority.HIGH,
            energyLevels = setOf(EnergyLevel.LOW, EnergyLevel.MEDIUM),
            projectIds = setOf("p1", "p2"),
            dueDateFrom = LocalDate(2026, 8, 1),
            dueDateTo = LocalDate(2026, 8, 31),
            titleContains = "report",
        )
        val sort = SortSpec(field = SortField.PRIORITY, ascending = false)
        val decoded = prefs.decode(prefs.encode("Work week", filter, sort))
        assertEquals("Work week", decoded?.name)
        assertEquals(filter, decoded?.filter)
        assertEquals(sort, decoded?.sort)
    }

    @Test
    fun `empty spec round trips to defaults`() {
        val decoded = prefs.decode(prefs.encode("All", FilterSpec(), SortSpec()))
        assertEquals("All", decoded?.name)
        assertNull(decoded?.filter?.statuses)
        assertNull(decoded?.filter?.minPriority)
        assertNull(decoded?.filter?.titleContains)
        assertEquals(SortSpec(), decoded?.sort)
    }

    @Test
    fun `query pipes are sanitized not structural`() {
        val filter = FilterSpec(titleContains = "a|b")
        val decoded = prefs.decode(prefs.encode("q", filter, SortSpec()))
        assertEquals("a b", decoded?.filter?.titleContains)
    }
}
