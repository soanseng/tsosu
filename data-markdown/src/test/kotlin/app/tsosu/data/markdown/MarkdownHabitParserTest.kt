package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarkdownHabitParserTest {

    private val parser = MarkdownHabitParser()

    @Test
    fun `empty markdown produces empty habits and completions`() {
        val result = parser.parse("")

        assertTrue(result.habits.isEmpty(), "Should have no habits")
        assertTrue(result.completions.isEmpty(), "Should have no completions")
    }

    @Test
    fun `frontmatter only produces empty habits and completions`() {
        val md = """
            |---
            |tsosu: v1
            |updated: 2026-03-23T10:00:00
            |---
        """.trimMargin()

        val result = parser.parse(md)

        assertTrue(result.habits.isEmpty(), "Should have no habits")
        assertTrue(result.completions.isEmpty(), "Should have no completions")
    }

    @Test
    fun `parses habit with completions`() {
        val md = """
            |---
            |tsosu: v1
            |updated: 2026-03-23T10:00:00
            |---
            |
            |## Daily
            |
            |- [ ] Exercise (tiny: do 1 pushup) 🔁daily ⚡medium <!-- id:h1 -->
            |  - ✅ 2026-03-23
            |  - ✅ 2026-03-22
        """.trimMargin()

        val result = parser.parse(md)

        assertEquals(1, result.habits.size, "Should have 1 habit")
        val habit = result.habits[0]
        assertEquals("h1", habit.id)
        assertEquals("Exercise", habit.title)
        assertEquals("do 1 pushup", habit.tinyVersion)
        assertEquals(HabitFrequency.DAILY, habit.frequency)
        assertEquals(EnergyLevel.MEDIUM, habit.energyLevel)

        assertEquals(2, result.completions.size, "Should have 2 completions")
        assertEquals("h1", result.completions[0].habitId)
        assertEquals(LocalDate.parse("2026-03-23"), result.completions[0].date)
        assertEquals("h1", result.completions[1].habitId)
        assertEquals(LocalDate.parse("2026-03-22"), result.completions[1].date)
    }

    @Test
    fun `parses weekdays frequency`() {
        val md = """
            |---
            |tsosu: v1
            |updated: 2026-03-23T10:00:00
            |---
            |
            |## Weekdays
            |
            |- [ ] Morning standup 🔁weekdays ⚡low <!-- id:h2 -->
        """.trimMargin()

        val result = parser.parse(md)

        assertEquals(1, result.habits.size)
        val habit = result.habits[0]
        assertEquals("h2", habit.id)
        assertEquals("Morning standup", habit.title)
        assertNull(habit.tinyVersion)
        assertEquals(HabitFrequency.WEEKDAYS, habit.frequency)
        assertEquals(EnergyLevel.LOW, habit.energyLevel)
    }

    @Test
    fun `parses custom frequency 3x per week`() {
        val md = """
            |---
            |tsosu: v1
            |updated: 2026-03-23T10:00:00
            |---
            |
            |## Custom
            |
            |- [ ] Grocery run 🔁3x/week ⚡medium <!-- id:h3 -->
            |  - ✅ 2026-03-22
        """.trimMargin()

        val result = parser.parse(md)

        assertEquals(1, result.habits.size)
        val habit = result.habits[0]
        assertEquals("h3", habit.id)
        assertEquals("Grocery run", habit.title)
        assertEquals(HabitFrequency.CUSTOM, habit.frequency)
        assertEquals(3, habit.targetDaysPerWeek)
        assertEquals(EnergyLevel.MEDIUM, habit.energyLevel)

        assertEquals(1, result.completions.size)
        assertEquals(LocalDate.parse("2026-03-22"), result.completions[0].date)
    }

    @Test
    fun `round-trip serialize then parse preserves data`() {
        val serializer = MarkdownHabitSerializer()

        val originalHabits = listOf(
            Habit(
                id = "h1",
                title = "Exercise",
                tinyVersion = "do 1 pushup",
                frequency = HabitFrequency.DAILY,
                targetDaysPerWeek = 7,
                energyLevel = EnergyLevel.MEDIUM,
                createdAt = Instant.parse("2026-03-20T10:00:00Z"),
            ),
            Habit(
                id = "h2",
                title = "Morning standup",
                tinyVersion = null,
                frequency = HabitFrequency.WEEKDAYS,
                targetDaysPerWeek = 7,
                energyLevel = EnergyLevel.LOW,
                createdAt = Instant.parse("2026-03-20T10:00:00Z"),
            ),
            Habit(
                id = "h3",
                title = "Grocery run",
                tinyVersion = null,
                frequency = HabitFrequency.CUSTOM,
                targetDaysPerWeek = 3,
                energyLevel = EnergyLevel.HIGH,
                createdAt = Instant.parse("2026-03-20T10:00:00Z"),
            ),
        )
        val originalCompletions = listOf(
            HabitCompletion("h1", LocalDate.parse("2026-03-23"), Instant.parse("2026-03-23T08:00:00Z")),
            HabitCompletion("h1", LocalDate.parse("2026-03-22"), Instant.parse("2026-03-22T08:00:00Z")),
            HabitCompletion("h3", LocalDate.parse("2026-03-22"), Instant.parse("2026-03-22T09:00:00Z")),
        )

        val markdown = serializer.serialize(originalHabits, originalCompletions)
        val parsed = parser.parse(markdown)

        // Verify habits
        assertEquals(3, parsed.habits.size, "Should preserve 3 habits")

        val parsedH1 = parsed.habits.first { it.id == "h1" }
        assertEquals("Exercise", parsedH1.title)
        assertEquals("do 1 pushup", parsedH1.tinyVersion)
        assertEquals(HabitFrequency.DAILY, parsedH1.frequency)
        assertEquals(EnergyLevel.MEDIUM, parsedH1.energyLevel)

        val parsedH2 = parsed.habits.first { it.id == "h2" }
        assertEquals("Morning standup", parsedH2.title)
        assertNull(parsedH2.tinyVersion)
        assertEquals(HabitFrequency.WEEKDAYS, parsedH2.frequency)
        assertEquals(EnergyLevel.LOW, parsedH2.energyLevel)

        val parsedH3 = parsed.habits.first { it.id == "h3" }
        assertEquals("Grocery run", parsedH3.title)
        assertEquals(HabitFrequency.CUSTOM, parsedH3.frequency)
        assertEquals(3, parsedH3.targetDaysPerWeek)
        assertEquals(EnergyLevel.HIGH, parsedH3.energyLevel)

        // Verify completions
        assertEquals(3, parsed.completions.size, "Should preserve 3 completions")
        val h1Completions = parsed.completions.filter { it.habitId == "h1" }
        assertEquals(2, h1Completions.size)
        assertTrue(
            h1Completions.any { it.date == LocalDate.parse("2026-03-23") },
            "Should have 2026-03-23 completion",
        )
        assertTrue(
            h1Completions.any { it.date == LocalDate.parse("2026-03-22") },
            "Should have 2026-03-22 completion",
        )

        val h3Completions = parsed.completions.filter { it.habitId == "h3" }
        assertEquals(1, h3Completions.size)
        assertEquals(LocalDate.parse("2026-03-22"), h3Completions[0].date)
    }

    @Test
    fun `parses habit with high energy`() {
        val md = """
            |---
            |tsosu: v1
            |updated: 2026-03-23T10:00:00
            |---
            |
            |## Daily
            |
            |- [ ] Deep work 🔁daily ⚡high <!-- id:h4 -->
        """.trimMargin()

        val result = parser.parse(md)

        assertEquals(1, result.habits.size)
        assertEquals(EnergyLevel.HIGH, result.habits[0].energyLevel)
    }

    @Test
    fun `parses multiple habits across sections`() {
        val md = """
            |---
            |tsosu: v1
            |updated: 2026-03-23T10:00:00
            |---
            |
            |## Daily
            |
            |- [ ] Exercise 🔁daily ⚡medium <!-- id:h1 -->
            |- [ ] Read 🔁daily ⚡low <!-- id:h2 -->
            |
            |## Weekdays
            |
            |- [ ] Standup 🔁weekdays ⚡low <!-- id:h3 -->
        """.trimMargin()

        val result = parser.parse(md)

        assertEquals(3, result.habits.size)
        assertEquals(HabitFrequency.DAILY, result.habits.first { it.id == "h1" }.frequency)
        assertEquals(HabitFrequency.DAILY, result.habits.first { it.id == "h2" }.frequency)
        assertEquals(HabitFrequency.WEEKDAYS, result.habits.first { it.id == "h3" }.frequency)
    }
}
