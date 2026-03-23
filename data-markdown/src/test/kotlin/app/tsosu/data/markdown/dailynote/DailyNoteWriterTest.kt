package app.tsosu.data.markdown.dailynote

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitFrequency
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DailyNoteWriterTest {

    private val writer = DailyNoteWriter()

    private val fixedCreatedAt = Instant.parse("2026-03-20T10:00:00Z")

    private fun habit(
        id: String = "h1",
        title: String = "Exercise",
        position: Double = 0.0,
        energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
        createdAt: Instant = fixedCreatedAt,
    ) = Habit(
        id = id,
        title = title,
        position = position,
        energyLevel = energyLevel,
        createdAt = createdAt,
    )

    @Test
    fun `generates daily note with habit checkboxes`() {
        val habits = listOf(
            habit(id = "h1", title = "Meditation"),
            habit(id = "h2", title = "Exercise"),
        )
        val completedIds = setOf("h1")
        val date = LocalDate.parse("2026-03-23")

        val result = writer.write(date, habits, completedIds)

        assertTrue(result.contains("date: 2026-03-23"))
        assertTrue(result.contains("## Habits"))
        assertTrue(result.contains("- [x] Meditation #habit <!-- id:h1 -->"))
        assertTrue(result.contains("- [ ] Exercise #habit <!-- id:h2 -->"))
    }

    @Test
    fun `filename is date-based`() {
        val filename = writer.filename(LocalDate.parse("2026-03-23"))
        assertEquals("2026-03-23.md", filename)
    }

    @Test
    fun `empty habits produces minimal daily note`() {
        val result = writer.write(LocalDate.parse("2026-03-23"), emptyList(), emptySet())

        assertTrue(result.contains("date: 2026-03-23"))
        assertTrue(result.contains("## Habits"))
        // No checkbox lines
        val lines = result.lines()
        assertTrue(lines.none { it.startsWith("- [") }, "Should have no habit checkbox lines")
    }

    @Test
    fun `all habits completed shows all as checked`() {
        val habits = listOf(
            habit(id = "h1", title = "Meditation"),
            habit(id = "h2", title = "Exercise"),
            habit(id = "h3", title = "Reading"),
        )
        val completedIds = setOf("h1", "h2", "h3")
        val date = LocalDate.parse("2026-03-23")

        val result = writer.write(date, habits, completedIds)

        assertTrue(result.contains("- [x] Meditation #habit <!-- id:h1 -->"))
        assertTrue(result.contains("- [x] Exercise #habit <!-- id:h2 -->"))
        assertTrue(result.contains("- [x] Reading #habit <!-- id:h3 -->"))
        // No unchecked lines
        assertTrue(result.lines().none { it.contains("- [ ]") }, "All habits should be checked")
    }

    @Test
    fun `no habits completed shows all as unchecked`() {
        val habits = listOf(
            habit(id = "h1", title = "Meditation"),
            habit(id = "h2", title = "Exercise"),
        )
        val date = LocalDate.parse("2026-03-23")

        val result = writer.write(date, habits, emptySet())

        assertTrue(result.contains("- [ ] Meditation #habit <!-- id:h1 -->"))
        assertTrue(result.contains("- [ ] Exercise #habit <!-- id:h2 -->"))
        // No checked lines
        assertTrue(result.lines().none { it.contains("- [x]") }, "No habits should be checked")
    }

    @Test
    fun `habits sorted by position`() {
        val habits = listOf(
            habit(id = "h3", title = "Reading", position = 3.0),
            habit(id = "h1", title = "Meditation", position = 1.0),
            habit(id = "h2", title = "Exercise", position = 2.0),
        )
        val date = LocalDate.parse("2026-03-23")

        val result = writer.write(date, habits, emptySet())

        val habitLines = result.lines().filter { it.startsWith("- [") }
        assertEquals(3, habitLines.size, "Should have 3 habit lines")
        assertTrue(habitLines[0].contains("Meditation"), "First habit should be Meditation (position 1.0)")
        assertTrue(habitLines[1].contains("Exercise"), "Second habit should be Exercise (position 2.0)")
        assertTrue(habitLines[2].contains("Reading"), "Third habit should be Reading (position 3.0)")
    }

    @Test
    fun `frontmatter contains date field between fences`() {
        val date = LocalDate.parse("2026-03-23")
        val result = writer.write(date, emptyList(), emptySet())

        val lines = result.lines()
        assertEquals("---", lines[0], "First line should be frontmatter opening fence")
        assertEquals("date: 2026-03-23", lines[1], "Second line should be date field")
        assertEquals("---", lines[2], "Third line should be frontmatter closing fence")
    }
}
