package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaProjectDto
import app.tsosu.domain.model.RoutineTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VikunjaRoutineMapperTest {

    private val mapper = VikunjaRoutineMapper()

    @Test
    fun `routineToProjectDto includes metadata marker`() {
        val dto = mapper.routineToProjectDto(
            title = "Morning Routine",
            timeOfDay = RoutineTime.MORNING,
        )
        assertTrue(dto.description.contains("<!-- tsosu-routine:MORNING -->"))
        assertEquals("Morning Routine", dto.title)
    }

    @Test
    fun `extractRoutineTime parses MORNING from description`() {
        val desc = "<!-- tsosu-routine:MORNING -->"
        assertEquals(RoutineTime.MORNING, mapper.extractRoutineTime(desc))
    }

    @Test
    fun `extractRoutineTime parses EVENING from description`() {
        val desc = "Some notes\n<!-- tsosu-routine:EVENING -->"
        assertEquals(RoutineTime.EVENING, mapper.extractRoutineTime(desc))
    }

    @Test
    fun `extractRoutineTime returns null for non-routine project`() {
        assertNull(mapper.extractRoutineTime("Just a regular project"))
    }

    @Test
    fun `isRoutineProject detects routine marker`() {
        val dto = VikunjaProjectDto(description = "<!-- tsosu-routine:AFTERNOON -->")
        assertTrue(mapper.isRoutineProject(dto))
    }

    @Test
    fun `isRoutineProject rejects normal project`() {
        val dto = VikunjaProjectDto(description = "Regular project")
        assertFalse(mapper.isRoutineProject(dto))
    }
}
