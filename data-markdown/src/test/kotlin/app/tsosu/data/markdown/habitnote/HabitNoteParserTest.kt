package app.tsosu.data.markdown.habitnote

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.model.RoutineTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HabitNoteParserTest {

    private val parser = HabitNoteParser()

    @Test
    fun `parses full HabitNote with all fields`() {
        val content = """
            |---
            |id: h1
            |frequency: daily
            |target_days: 7
            |energy: medium
            |tiny: "Take 3 deep breaths"
            |color: "#4CAF50"
            |archived: false
            |created: 2026-01-15
            |---
            |
            |# Meditation
            |
            |## Completions
            |- ✅ 2026-03-23
            |- ✅ 2026-03-22
        """.trimMargin()

        val result = parser.parse(content)

        val habit = result.habit
        assertEquals("h1", habit.id)
        assertEquals("Meditation", habit.title)
        assertEquals(HabitFrequency.DAILY, habit.frequency)
        assertEquals(7, habit.targetDaysPerWeek)
        assertEquals(EnergyLevel.MEDIUM, habit.energyLevel)
        assertEquals("Take 3 deep breaths", habit.tinyVersion)
        assertEquals("#4CAF50", habit.color)
        assertEquals(false, habit.isArchived)
        assertEquals(
            LocalDate.parse("2026-01-15"),
            habit.createdAt.toLocalDateTime(TimeZone.UTC).date,
        )

        assertEquals(2, result.completions.size)
        assertEquals("h1", result.completions[0].habitId)
        assertEquals(LocalDate.parse("2026-03-23"), result.completions[0].date)
        assertEquals(LocalDate.parse("2026-03-22"), result.completions[1].date)
    }

    @Test
    fun `parses minimal HabitNote with only required fields`() {
        val content = """
            |---
            |id: h2
            |frequency: daily
            |---
            |
            |# Read Books
        """.trimMargin()

        val result = parser.parse(content)

        val habit = result.habit
        assertEquals("h2", habit.id)
        assertEquals("Read Books", habit.title)
        assertEquals(HabitFrequency.DAILY, habit.frequency)
        assertNull(habit.tinyVersion)
        assertTrue(result.completions.isEmpty())
    }

    @Test
    fun `parses completions list`() {
        val content = """
            |---
            |id: h3
            |frequency: weekdays
            |---
            |
            |# Morning standup
            |
            |## Completions
            |- ✅ 2026-03-23
            |- ✅ 2026-03-22
            |- ✅ 2026-03-21
        """.trimMargin()

        val result = parser.parse(content)

        assertEquals(3, result.completions.size)
        assertEquals("h3", result.completions[0].habitId)
        assertEquals(LocalDate.parse("2026-03-23"), result.completions[0].date)
        assertEquals(LocalDate.parse("2026-03-22"), result.completions[1].date)
        assertEquals(LocalDate.parse("2026-03-21"), result.completions[2].date)
    }

    @Test
    fun `parses custom frequency with target_days`() {
        val content = """
            |---
            |id: h4
            |frequency: custom
            |target_days: 3
            |energy: high
            |---
            |
            |# Grocery run
        """.trimMargin()

        val result = parser.parse(content)

        val habit = result.habit
        assertEquals(HabitFrequency.CUSTOM, habit.frequency)
        assertEquals(3, habit.targetDaysPerWeek)
        assertEquals(EnergyLevel.HIGH, habit.energyLevel)
    }

    @Test
    fun `extracts title from H1 heading`() {
        val content = """
            |---
            |id: h5
            |frequency: daily
            |---
            |
            |# Deep Work Session
        """.trimMargin()

        val result = parser.parse(content)
        assertEquals("Deep Work Session", result.habit.title)
    }

    @Test
    fun `parses weekdays frequency`() {
        val content = """
            |---
            |id: h6
            |frequency: weekdays
            |energy: low
            |---
            |
            |# Standup
        """.trimMargin()

        val result = parser.parse(content)
        assertEquals(HabitFrequency.WEEKDAYS, result.habit.frequency)
        assertEquals(EnergyLevel.LOW, result.habit.energyLevel)
    }

    @Test
    fun `parses archived habit`() {
        val content = """
            |---
            |id: h7
            |frequency: daily
            |archived: true
            |---
            |
            |# Old Habit
        """.trimMargin()

        val result = parser.parse(content)
        assertEquals(true, result.habit.isArchived)
    }

    @Test
    fun `defaults archived to false when missing`() {
        val content = """
            |---
            |id: h8
            |frequency: daily
            |---
            |
            |# New Habit
        """.trimMargin()

        val result = parser.parse(content)
        assertEquals(false, result.habit.isArchived)
    }

    @Test
    fun `round-trip serialize then parse preserves data`() {
        val serializer = HabitNoteSerializer()

        val originalHabit = app.tsosu.domain.model.Habit(
            id = "h1",
            title = "Meditation",
            tinyVersion = "Take 3 deep breaths",
            frequency = HabitFrequency.DAILY,
            targetDaysPerWeek = 7,
            energyLevel = EnergyLevel.MEDIUM,
            color = "#4CAF50",
            isArchived = false,
            createdAt = Instant.parse("2026-01-15T10:00:00Z"),
        )
        val originalCompletions = listOf(
            app.tsosu.domain.model.HabitCompletion(
                "h1",
                LocalDate.parse("2026-03-23"),
                Instant.parse("2026-03-23T08:00:00Z"),
            ),
            app.tsosu.domain.model.HabitCompletion(
                "h1",
                LocalDate.parse("2026-03-22"),
                Instant.parse("2026-03-22T08:00:00Z"),
            ),
        )

        val markdown = serializer.serialize(originalHabit, originalCompletions)
        val parsed = parser.parse(markdown)

        assertEquals(originalHabit.id, parsed.habit.id)
        assertEquals(originalHabit.title, parsed.habit.title)
        assertEquals(originalHabit.tinyVersion, parsed.habit.tinyVersion)
        assertEquals(originalHabit.frequency, parsed.habit.frequency)
        assertEquals(originalHabit.targetDaysPerWeek, parsed.habit.targetDaysPerWeek)
        assertEquals(originalHabit.energyLevel, parsed.habit.energyLevel)
        assertEquals(originalHabit.color, parsed.habit.color)
        assertEquals(originalHabit.isArchived, parsed.habit.isArchived)

        assertEquals(2, parsed.completions.size)
        assertEquals(LocalDate.parse("2026-03-23"), parsed.completions[0].date)
        assertEquals(LocalDate.parse("2026-03-22"), parsed.completions[1].date)
    }

    @Test
    fun `round-trip custom frequency preserves target_days`() {
        val serializer = HabitNoteSerializer()

        val originalHabit = app.tsosu.domain.model.Habit(
            id = "h9",
            title = "Gym",
            frequency = HabitFrequency.CUSTOM,
            targetDaysPerWeek = 4,
            energyLevel = EnergyLevel.HIGH,
            color = "#FF5722",
            createdAt = Instant.parse("2026-02-01T12:00:00Z"),
        )

        val markdown = serializer.serialize(originalHabit, emptyList())
        val parsed = parser.parse(markdown)

        assertEquals(HabitFrequency.CUSTOM, parsed.habit.frequency)
        assertEquals(4, parsed.habit.targetDaysPerWeek)
    }

    @Test
    fun `no completions section produces empty list`() {
        val content = """
            |---
            |id: h10
            |frequency: daily
            |---
            |
            |# Solo Habit
        """.trimMargin()

        val result = parser.parse(content)
        assertTrue(result.completions.isEmpty())
    }

    @Test
    fun `parses routine and reminder fields`() {
        val content = """
            |---
            |id: h1
            |frequency: daily
            |routine: evening
            |reminder: "07:30"
            |energy: low
            |created: 2026-01-15
            |---
            |
            |# Evening stretch
        """.trimMargin()

        val result = parser.parse(content)

        assertEquals(RoutineTime.EVENING, result.routineTime)
        assertEquals(LocalTime(7, 30), result.habit.reminderTime)
    }

    @Test
    fun `routine is case insensitive and unknown values become null`() {
        val content = """
            |---
            |id: h1
            |frequency: daily
            |routine: Morning
            |energy: low
            |created: 2026-01-15
            |---
            |
            |# Stretch
        """.trimMargin()

        assertEquals(RoutineTime.MORNING, parser.parse(content).routineTime)

        val unknown = content.replace("routine: Morning", "routine: midnight")
        assertNull(parser.parse(unknown).routineTime)
    }

    @Test
    fun `missing routine and reminder parse as null`() {
        val content = """
            |---
            |id: h1
            |frequency: daily
            |energy: low
            |created: 2026-01-15
            |---
            |
            |# Stretch
        """.trimMargin()

        val result = parser.parse(content)

        assertNull(result.routineTime)
        assertNull(result.habit.reminderTime)
    }

    @Test
    fun `invalid reminder time parses as null`() {
        val content = """
            |---
            |id: h1
            |frequency: daily
            |reminder: "25:99"
            |energy: low
            |created: 2026-01-15
            |---
            |
            |# Stretch
        """.trimMargin()

        assertNull(parser.parse(content).habit.reminderTime)
    }

    @Test
    fun `empty completions section produces empty list`() {
        val content = """
            |---
            |id: h11
            |frequency: daily
            |---
            |
            |# Fresh Habit
            |
            |## Completions
        """.trimMargin()

        val result = parser.parse(content)
        assertTrue(result.completions.isEmpty())
    }
}
