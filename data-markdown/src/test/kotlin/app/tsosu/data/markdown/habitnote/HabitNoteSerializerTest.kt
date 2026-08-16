package app.tsosu.data.markdown.habitnote

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.model.RoutineTime
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HabitNoteSerializerTest {

    private val serializer = HabitNoteSerializer()

    private val habit = Habit(
        id = "h1",
        title = "Meditation",
        tinyVersion = "Take 3 deep breaths",
        frequency = HabitFrequency.DAILY,
        targetDaysPerWeek = 7,
        energyLevel = EnergyLevel.MEDIUM,
        color = "#4CAF50",
        createdAt = Instant.parse("2026-01-15T10:00:00Z"),
    )

    private val completions = listOf(
        HabitCompletion("h1", LocalDate.parse("2026-03-23"), Instant.parse("2026-03-23T08:00:00Z")),
        HabitCompletion("h1", LocalDate.parse("2026-03-22"), Instant.parse("2026-03-22T08:00:00Z")),
        HabitCompletion("h1", LocalDate.parse("2026-03-21"), Instant.parse("2026-03-21T08:00:00Z")),
    )

    @Test
    fun `serializes frontmatter with all fields`() {
        val result = serializer.serialize(habit, completions)

        assertTrue(result.contains("id: h1"))
        assertTrue(result.contains("frequency: daily"))
        assertTrue(result.contains("energy: medium"))
        assertTrue(result.contains("tiny: \"Take 3 deep breaths\""))
        assertTrue(result.contains("color: \"#4CAF50\""))
        assertTrue(result.contains("created: 2026-01-15"))
    }

    @Test
    fun `serializes title as h1 heading`() {
        val result = serializer.serialize(habit, completions)
        assertTrue(result.contains("# Meditation"))
    }

    @Test
    fun `serializes completions newest first`() {
        val result = serializer.serialize(habit, completions)
        assertTrue(result.contains("## Completions"))
        val lines = result.lines()
        val compLines = lines.filter { it.startsWith("- \u2705") }
        assertTrue(compLines[0].contains("2026-03-23"))
        assertTrue(compLines[1].contains("2026-03-22"))
        assertTrue(compLines[2].contains("2026-03-21"))
    }

    @Test
    fun `custom frequency includes target`() {
        val custom = habit.copy(frequency = HabitFrequency.CUSTOM, targetDaysPerWeek = 3)
        val result = serializer.serialize(custom, emptyList())
        assertTrue(result.contains("frequency: custom"))
        assertTrue(result.contains("target_days: 3"))
    }

    @Test
    fun `omits tiny when null`() {
        val noTiny = habit.copy(tinyVersion = null)
        val result = serializer.serialize(noTiny, emptyList())
        assertTrue(!result.contains("tiny:"))
    }

    @Test
    fun `generates slug filename from title`() {
        val slug = serializer.slugify("Meditation & Exercise (30m)")
        assertEquals("meditation-exercise-30m", slug)
    }

    @Test
    fun `slug handles unicode`() {
        val slug = serializer.slugify("\u51A5\u60F3 Morning")
        assertEquals("\u51A5\u60F3-morning", slug)
    }

    @Test
    fun `empty completions omits completions section`() {
        val result = serializer.serialize(habit, emptyList())
        assertTrue(!result.contains("## Completions"))
    }

    @Test
    fun `archived habit has archived field`() {
        val archived = habit.copy(isArchived = true)
        val result = serializer.serialize(archived, emptyList())
        assertTrue(result.contains("archived: true"))
    }


    @Test
    fun `serializes routine when provided`() {
        val result = serializer.serialize(habit, emptyList(), routineTime = RoutineTime.EVENING)
        assertTrue(result.contains("routine: evening"))
    }

    @Test
    fun `omits routine when null`() {
        val result = serializer.serialize(habit, emptyList())
        assertTrue(!result.contains("routine:"))
    }

    @Test
    fun `serializes reminder time as quoted HH mm`() {
        val withReminder = habit.copy(reminderTime = LocalTime(7, 5))
        val result = serializer.serialize(withReminder, emptyList())
        assertTrue(result.contains("reminder: \"07:05\""))
    }

    @Test
    fun `omits reminder when null`() {
        val result = serializer.serialize(habit, emptyList())
        assertTrue(!result.contains("reminder:"))
    }

    @Test
    fun `round trip preserves routine and reminder`() {
        val parser = HabitNoteParser()
        val original = habit.copy(reminderTime = LocalTime(21, 45))
        val serialized = serializer.serialize(original, emptyList(), routineTime = RoutineTime.MORNING)
        val parsed = parser.parse(serialized)

        assertEquals(RoutineTime.MORNING, parsed.routineTime)
        assertEquals(LocalTime(21, 45), parsed.habit.reminderTime)
    }

    @Test
    fun `round trip preserves project name`() {
        val parser = HabitNoteParser()
        val serialized = serializer.serialize(habit, emptyList(), projectName = "Work")
        val parsed = parser.parse(serialized)

        assertEquals("Work", parsed.projectName)
    }

    @Test
    fun `omits project when null`() {
        val result = serializer.serialize(habit, emptyList())
        assertTrue(!result.contains("project:"))
    }
    @Test
    fun `non-archived habit has archived false`() {
        val result = serializer.serialize(habit, emptyList())
        assertTrue(result.contains("archived: false"))
    }
}
