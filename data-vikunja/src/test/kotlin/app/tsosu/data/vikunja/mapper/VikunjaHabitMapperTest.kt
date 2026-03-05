package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import app.tsosu.domain.model.HabitFrequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VikunjaHabitMapperTest {

    private val mapper = VikunjaHabitMapper()

    @Test
    fun `habitToTaskDto sets repeatAfter to 86400 for daily`() {
        val dto = mapper.habitToTaskDto(
            title = "Meditate",
            tinyVersion = "Sit, take 3 breaths",
            frequency = HabitFrequency.DAILY,
            routineProjectId = 10L,
            position = 1.0,
            hexColor = "4CAF50",
        )
        assertEquals(86400L, dto.repeatAfter)
        assertEquals("Meditate", dto.title)
        assertEquals(10L, dto.projectId)
        assertTrue(dto.description.contains("Tsosu Habit"))
        assertTrue(dto.description.contains("Sit, take 3 breaths"))
    }

    @Test
    fun `habitToTaskDto without tinyVersion still has marker`() {
        val dto = mapper.habitToTaskDto(
            title = "Exercise",
            tinyVersion = null,
            frequency = HabitFrequency.DAILY,
            routineProjectId = 10L,
            position = 0.0,
            hexColor = "4CAF50",
        )
        assertTrue(dto.description.contains("-- Tsosu Habit"))
        assertFalse(dto.description.contains("Tiny version"))
    }

    @Test
    fun `extractTinyVersion parses from description`() {
        val desc = "Tiny version: Sit, take 3 breaths\n\n-- Tsosu Habit"
        assertEquals("Sit, take 3 breaths", mapper.extractTinyVersion(desc))
    }

    @Test
    fun `extractTinyVersion returns null when absent`() {
        assertNull(mapper.extractTinyVersion("-- Tsosu Habit"))
    }

    @Test
    fun `isHabitTask identifies habit by all three criteria`() {
        val dto = VikunjaTaskDto(
            id = 1,
            repeatAfter = 86400,
            projectId = 10,
            description = "Some text\n\n-- Tsosu Habit",
        )
        assertTrue(mapper.isHabitTask(dto, setOf(10L)))
    }

    @Test
    fun `isHabitTask rejects task without marker`() {
        val dto = VikunjaTaskDto(
            id = 1,
            repeatAfter = 86400,
            projectId = 10,
            description = "Just a repeating task",
        )
        assertFalse(mapper.isHabitTask(dto, setOf(10L)))
    }

    @Test
    fun `isHabitTask rejects task not in routine project`() {
        val dto = VikunjaTaskDto(
            id = 1,
            repeatAfter = 86400,
            projectId = 99,
            description = "-- Tsosu Habit",
        )
        assertFalse(mapper.isHabitTask(dto, setOf(10L)))
    }
}
