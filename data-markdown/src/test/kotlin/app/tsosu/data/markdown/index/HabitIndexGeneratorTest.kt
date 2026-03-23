package app.tsosu.data.markdown.index

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HabitIndexGeneratorTest {

    private val generator = HabitIndexGenerator()

    private val fixedCreatedAt = Instant.parse("2026-03-01T10:00:00Z")
    private val today = Clock.System.todayIn(TimeZone.UTC)

    private fun habit(
        id: String = "h1",
        title: String = "Exercise",
        frequency: HabitFrequency = HabitFrequency.DAILY,
        targetDaysPerWeek: Int = 7,
        energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
        position: Double = 0.0,
    ) = Habit(
        id = id,
        title = title,
        frequency = frequency,
        targetDaysPerWeek = targetDaysPerWeek,
        energyLevel = energyLevel,
        position = position,
        createdAt = fixedCreatedAt,
    )

    private fun completionAt(habitId: String, daysAgo: Int): HabitCompletion {
        val date = today.minus(daysAgo, DateTimeUnit.DAY)
        return HabitCompletion(
            habitId = habitId,
            date = date,
            completedAt = Instant.parse("${date}T08:00:00Z"),
        )
    }

    // --- Frontmatter ---

    @Test
    fun `generates frontmatter with version and generated flag`() {
        val result = generator.generate(emptyList(), emptyList())

        assertTrue(result.startsWith("---\n"))
        assertTrue(result.contains("tsosu: v1"))
        assertTrue(result.contains("updated:"))
        assertTrue(result.contains("generated: true"))
        assertTrue(result.contains("---"))
    }

    // --- Grouping ---

    @Test
    fun `groups habits by frequency into sections`() {
        val habits = listOf(
            habit(id = "h1", title = "Morning run", frequency = HabitFrequency.DAILY),
            habit(id = "h2", title = "Standup", frequency = HabitFrequency.WEEKDAYS),
            habit(id = "h3", title = "Read tech blog", frequency = HabitFrequency.CUSTOM, targetDaysPerWeek = 3),
        )

        val result = generator.generate(habits, emptyList())

        assertTrue(result.contains("## Daily"))
        assertTrue(result.contains("## Weekdays"))
        assertTrue(result.contains("## Custom"))

        val dailySection = extractSection(result, "Daily")
        assertTrue(dailySection.contains("Morning run"))

        val weekdaysSection = extractSection(result, "Weekdays")
        assertTrue(weekdaysSection.contains("Standup"))

        val customSection = extractSection(result, "Custom")
        assertTrue(customSection.contains("Read tech blog"))
    }

    @Test
    fun `empty sections are skipped`() {
        val habits = listOf(
            habit(id = "h1", title = "Morning run", frequency = HabitFrequency.DAILY),
        )

        val result = generator.generate(habits, emptyList())

        assertTrue(result.contains("## Daily"))
        assertFalse(result.contains("## Weekdays"))
        assertFalse(result.contains("## Custom"))
    }

    @Test
    fun `no habits produces frontmatter only with no sections`() {
        val result = generator.generate(emptyList(), emptyList())

        assertTrue(result.contains("tsosu: v1"))
        assertFalse(result.contains("## "))
    }

    // --- Habit line format ---

    @Test
    fun `habit line contains energy emoji and id comment`() {
        val h = habit(id = "h1", title = "Meditation", energyLevel = EnergyLevel.MEDIUM)
        val result = generator.generate(listOf(h), emptyList())

        val line = result.lines().first { it.contains("<!-- id:h1 -->") }
        assertTrue(line.startsWith("- "))
        assertTrue(line.contains("Meditation"))
        assertTrue(line.contains("\u26A1medium"))
        assertTrue(line.contains("<!-- id:h1 -->"))
    }

    @Test
    fun `all energy levels serialize correctly`() {
        for ((energy, label) in listOf(
            EnergyLevel.LOW to "\u26A1low",
            EnergyLevel.MEDIUM to "\u26A1medium",
            EnergyLevel.HIGH to "\u26A1high",
        )) {
            val h = habit(id = "e-${energy.name}", energyLevel = energy)
            val result = generator.generate(listOf(h), emptyList())
            val line = result.lines().first { it.contains("<!-- id:e-${energy.name} -->") }
            assertTrue(line.contains(label), "Energy $energy should produce '$label', got: $line")
        }
    }

    // --- Wikilinks ---

    @Test
    fun `habit line includes wikilink from noteFilenames map`() {
        val h = habit(id = "h1", title = "Meditation")
        val filenames = mapOf("h1" to "meditation")
        val result = generator.generate(listOf(h), emptyList(), filenames)

        val line = result.lines().first { it.contains("<!-- id:h1 -->") }
        assertTrue(line.contains("[[habits/meditation]]"), "Should have wikilink, got: $line")
    }

    @Test
    fun `habit line includes wikilink with auto-slug when no filename provided`() {
        val h = habit(id = "h1", title = "Morning Exercise")
        val result = generator.generate(listOf(h), emptyList())

        val line = result.lines().first { it.contains("<!-- id:h1 -->") }
        assertTrue(
            line.contains("[[habits/morning-exercise]]"),
            "Should auto-slug to morning-exercise, got: $line",
        )
    }

    // --- Streak ---

    @Test
    fun `streak shown when habit has consecutive completions`() {
        val completions = listOf(
            completionAt("h1", 0), // today
            completionAt("h1", 1), // yesterday
            completionAt("h1", 2), // 2 days ago
        )
        val h = habit(id = "h1", title = "Exercise")
        val result = generator.generate(listOf(h), completions)

        val line = result.lines().first { it.contains("<!-- id:h1 -->") }
        assertTrue(line.contains("\uD83D\uDD253"), "Should show streak of 3, got: $line")
    }

    @Test
    fun `streak breaks on gap day`() {
        // today and yesterday completed, but day before yesterday missed
        val completions = listOf(
            completionAt("h1", 0), // today
            completionAt("h1", 1), // yesterday
            completionAt("h1", 3), // 3 days ago (gap at day 2)
        )
        val h = habit(id = "h1", title = "Exercise")
        val result = generator.generate(listOf(h), completions)

        val line = result.lines().first { it.contains("<!-- id:h1 -->") }
        assertTrue(line.contains("\uD83D\uDD252"), "Should show streak of 2, got: $line")
    }

    @Test
    fun `no streak when no completions`() {
        val h = habit(id = "h1", title = "Exercise")
        val result = generator.generate(listOf(h), emptyList())

        val line = result.lines().first { it.contains("<!-- id:h1 -->") }
        assertFalse(line.contains("\uD83D\uDD25"), "Should have no streak emoji, got: $line")
    }

    @Test
    fun `streak counts from most recent completion when not today`() {
        // Completed 1 and 2 days ago but not today
        val completions = listOf(
            completionAt("h1", 1),
            completionAt("h1", 2),
            completionAt("h1", 3),
        )
        val h = habit(id = "h1", title = "Exercise")
        val result = generator.generate(listOf(h), completions)

        val line = result.lines().first { it.contains("<!-- id:h1 -->") }
        assertTrue(line.contains("\uD83D\uDD253"), "Should show streak of 3, got: $line")
    }

    // --- Custom frequency: completion ratio ---

    @Test
    fun `custom frequency shows completion ratio`() {
        val h = habit(
            id = "h1",
            title = "Read tech blog",
            frequency = HabitFrequency.CUSTOM,
            targetDaysPerWeek = 3,
        )
        // 2 completions in last 7 days
        val completions = listOf(
            completionAt("h1", 0),
            completionAt("h1", 2),
        )
        val result = generator.generate(listOf(h), completions)

        val line = result.lines().first { it.contains("<!-- id:h1 -->") }
        assertTrue(
            line.contains("\uD83D\uDD012/3"),
            "Should show 2/3 ratio for custom frequency, got: $line",
        )
    }

    @Test
    fun `custom frequency with zero completions shows 0 slash target`() {
        val h = habit(
            id = "h1",
            title = "Read tech blog",
            frequency = HabitFrequency.CUSTOM,
            targetDaysPerWeek = 5,
        )
        val result = generator.generate(listOf(h), emptyList())

        val line = result.lines().first { it.contains("<!-- id:h1 -->") }
        assertTrue(
            line.contains("\uD83D\uDD010/5"),
            "Should show 0/5 ratio for custom frequency, got: $line",
        )
    }

    // --- Sorting ---

    @Test
    fun `habits sorted by position within section`() {
        val habits = listOf(
            habit(id = "h2", title = "Second", position = 2.0),
            habit(id = "h1", title = "First", position = 1.0),
            habit(id = "h3", title = "Third", position = 3.0),
        )
        val result = generator.generate(habits, emptyList())

        val habitLines = result.lines().filter { it.startsWith("- ") }
        assertTrue(habitLines[0].contains("First"), "First habit by position, got: ${habitLines[0]}")
        assertTrue(habitLines[1].contains("Second"), "Second habit by position, got: ${habitLines[1]}")
        assertTrue(habitLines[2].contains("Third"), "Third habit by position, got: ${habitLines[2]}")
    }

    // --- Full integration ---

    @Test
    fun `full generation matches expected format`() {
        val habits = listOf(
            habit(id = "h1", title = "Meditation", energyLevel = EnergyLevel.MEDIUM, position = 0.0),
            habit(id = "h2", title = "Exercise", energyLevel = EnergyLevel.HIGH, position = 1.0),
            habit(id = "h5", title = "Standup", energyLevel = EnergyLevel.LOW, frequency = HabitFrequency.WEEKDAYS),
            habit(
                id = "h3",
                title = "Read tech blog",
                energyLevel = EnergyLevel.LOW,
                frequency = HabitFrequency.CUSTOM,
                targetDaysPerWeek = 3,
            ),
        )
        val completions = listOf(
            // Meditation: 12-day streak (for streak to be 12, we need 12 consecutive days)
            completionAt("h1", 0),
            completionAt("h1", 1),
            completionAt("h1", 2),
            // Exercise: 3-day streak
            completionAt("h2", 0),
            completionAt("h2", 1),
            completionAt("h2", 2),
            // Read tech blog: 2 completions in last 7 days
            completionAt("h3", 1),
            completionAt("h3", 4),
        )
        val slugs = mapOf(
            "h1" to "meditation",
            "h2" to "exercise",
            "h5" to "standup",
            "h3" to "reading",
        )

        val result = generator.generate(habits, completions, slugs)

        // Daily section
        val dailySection = extractSection(result, "Daily")
        assertTrue(dailySection.contains("Meditation \u26A1medium \uD83D\uDD253 [[habits/meditation]] <!-- id:h1 -->"))
        assertTrue(dailySection.contains("Exercise \u26A1high \uD83D\uDD253 [[habits/exercise]] <!-- id:h2 -->"))

        // Weekdays section
        val weekdaysSection = extractSection(result, "Weekdays")
        assertTrue(weekdaysSection.contains("Standup \u26A1low [[habits/standup]] <!-- id:h5 -->"))

        // Custom section
        val customSection = extractSection(result, "Custom")
        assertTrue(customSection.contains("Read tech blog \uD83D\uDD012/3 \u26A1low [[habits/reading]] <!-- id:h3 -->"))
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
