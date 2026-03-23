package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarkdownHabitSerializerTest {

    private val serializer = MarkdownHabitSerializer()

    private val fixedCreatedAt = Instant.parse("2026-03-20T10:00:00Z")

    private fun habit(
        id: String = "h1",
        title: String = "Exercise",
        tinyVersion: String? = null,
        frequency: HabitFrequency = HabitFrequency.DAILY,
        targetDaysPerWeek: Int = 7,
        energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
        position: Double = 0.0,
        createdAt: Instant = fixedCreatedAt,
    ) = Habit(
        id = id,
        title = title,
        tinyVersion = tinyVersion,
        frequency = frequency,
        targetDaysPerWeek = targetDaysPerWeek,
        energyLevel = energyLevel,
        position = position,
        createdAt = createdAt,
    )

    @Test
    fun `empty habits produces frontmatter only`() {
        val result = serializer.serialize(emptyList(), emptyList())

        assertTrue(result.startsWith("---\n"), "Should start with frontmatter")
        assertTrue(result.contains("tsosu: v1"), "Should contain version marker")
        assertTrue(result.contains("updated:"), "Should contain updated timestamp")
        assertTrue(result.contains("---\n"), "Should close frontmatter")
        // No habit lines
        val nonFrontmatter = result.substringAfter("---\n").substringAfter("---")
        assertTrue(
            nonFrontmatter.lines().none { it.startsWith("- [") },
            "Should have no habit lines",
        )
    }

    @Test
    fun `habit with completions produces correct format with indented completion lines`() {
        val h = habit(
            id = "h1",
            title = "Exercise",
            tinyVersion = "do 1 pushup",
            frequency = HabitFrequency.DAILY,
            energyLevel = EnergyLevel.MEDIUM,
        )
        val completions = listOf(
            HabitCompletion("h1", LocalDate.parse("2026-03-23"), Instant.parse("2026-03-23T08:00:00Z")),
            HabitCompletion("h1", LocalDate.parse("2026-03-22"), Instant.parse("2026-03-22T08:00:00Z")),
        )

        val result = serializer.serialize(listOf(h), completions)

        // Habit line
        assertTrue(
            result.contains("- [ ] Exercise (tiny: do 1 pushup) \uD83D\uDD01daily \u26A1medium <!-- id:h1 -->"),
            "Habit line format mismatch. Lines:\n${result.lines().filter { it.startsWith("- [") }.joinToString("\n")}",
        )
        // Completion lines indented, newest first
        val lines = result.lines()
        val habitIdx = lines.indexOfFirst { it.contains("<!-- id:h1 -->") }
        assertTrue(habitIdx >= 0, "Habit line should exist")
        assertEquals("  - \u2705 2026-03-23", lines[habitIdx + 1])
        assertEquals("  - \u2705 2026-03-22", lines[habitIdx + 2])
    }

    @Test
    fun `groups by frequency with Daily and Weekdays sections`() {
        val habits = listOf(
            habit(id = "h1", title = "Morning run", frequency = HabitFrequency.DAILY),
            habit(id = "h2", title = "Standup", frequency = HabitFrequency.WEEKDAYS),
            habit(id = "h3", title = "Grocery run", frequency = HabitFrequency.CUSTOM, targetDaysPerWeek = 3),
        )

        val result = serializer.serialize(habits, emptyList())

        assertTrue(result.contains("## Daily"), "Should have Daily section")
        assertTrue(result.contains("## Weekdays"), "Should have Weekdays section")
        assertTrue(result.contains("## Custom"), "Should have Custom section")

        // Daily section contains daily habit
        val dailySection = extractSection(result, "Daily")
        assertTrue(dailySection.contains("Morning run"), "Daily section has daily habit")

        // Weekdays section contains weekdays habit
        val weekdaysSection = extractSection(result, "Weekdays")
        assertTrue(weekdaysSection.contains("Standup"), "Weekdays section has weekdays habit")

        // Custom section contains custom habit with Nx/week format
        val customSection = extractSection(result, "Custom")
        assertTrue(customSection.contains("Grocery run"), "Custom section has custom habit")
        assertTrue(customSection.contains("\uD83D\uDD013x/week"), "Custom habit shows 3x/week")
    }

    @Test
    fun `habit without tinyVersion omits parenthetical`() {
        val h = habit(id = "h1", title = "Meditate", tinyVersion = null)
        val result = serializer.serialize(listOf(h), emptyList())

        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(!taskLine.contains("(tiny:"), "Should not contain tiny version")
        assertTrue(taskLine.contains("- [ ] Meditate \uD83D\uDD01daily"), "Should have title and frequency")
    }

    @Test
    fun `energy levels serialize with lightning bolt prefix`() {
        for ((energy, expected) in listOf(
            EnergyLevel.LOW to "\u26A1low",
            EnergyLevel.MEDIUM to "\u26A1medium",
            EnergyLevel.HIGH to "\u26A1high",
        )) {
            val h = habit(id = "e-${energy.name}", energyLevel = energy)
            val result = serializer.serialize(listOf(h), emptyList())
            val taskLine = result.lines().first { it.startsWith("- [") }
            assertTrue(
                taskLine.contains(expected),
                "Energy $energy should produce '$expected', got: $taskLine",
            )
        }
    }

    @Test
    fun `empty sections are omitted`() {
        val habits = listOf(
            habit(id = "h1", title = "Daily habit", frequency = HabitFrequency.DAILY),
        )
        val result = serializer.serialize(habits, emptyList())

        assertTrue(result.contains("## Daily"), "Should have Daily section")
        assertTrue(!result.contains("## Weekdays"), "Should not have Weekdays section")
        assertTrue(!result.contains("## Custom"), "Should not have Custom section")
    }

    private fun extractSection(markdown: String, sectionName: String): String {
        val lines = markdown.lines()
        val startIdx = lines.indexOfFirst { it.trim() == "## $sectionName" }
        if (startIdx == -1) return ""
        val endIdx = lines.drop(startIdx + 1).indexOfFirst { it.startsWith("## ") }
        return if (endIdx == -1) {
            lines.drop(startIdx).joinToString("\n")
        } else {
            lines.subList(startIdx, startIdx + 1 + endIdx).joinToString("\n")
        }
    }
}
