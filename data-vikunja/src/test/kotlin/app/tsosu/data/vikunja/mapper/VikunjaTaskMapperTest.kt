package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaLabelDto
import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VikunjaTaskMapperTest {

    private val mapper = VikunjaTaskMapper()

    @Test
    fun `appendEstimate adds metadata comment to description`() {
        val result = mapper.appendEstimate("Buy groceries", 30)
        assertEquals("Buy groceries\n<!-- tsosu:{\"est\":30} -->", result)
    }

    @Test
    fun `appendEstimate with null minutes returns clean description`() {
        val result = mapper.appendEstimate("Buy groceries", null)
        assertEquals("Buy groceries", result)
    }

    @Test
    fun `appendEstimate replaces existing metadata`() {
        val desc = "Buy groceries\n<!-- tsosu:{\"est\":15} -->"
        val result = mapper.appendEstimate(desc, 30)
        assertEquals("Buy groceries\n<!-- tsosu:{\"est\":30} -->", result)
    }

    @Test
    fun `extractEstimate parses minutes from metadata`() {
        val desc = "Buy groceries\n<!-- tsosu:{\"est\":30} -->"
        assertEquals(30, mapper.extractEstimate(desc))
    }

    @Test
    fun `extractEstimate returns null when no metadata`() {
        assertNull(mapper.extractEstimate("Buy groceries"))
    }

    @Test
    fun `stripMetadata removes tsosu comment`() {
        val desc = "Buy groceries\n<!-- tsosu:{\"est\":30} -->"
        assertEquals("Buy groceries", mapper.stripMetadata(desc))
    }

    @Test
    fun `extractEnergyFromLabels finds high energy label`() {
        val labels = listOf(
            VikunjaLabelDto(id = 1, title = "urgent"),
            VikunjaLabelDto(id = 2, title = EnergyLevel.HIGH.labelTitle),
        )
        assertEquals(EnergyLevel.HIGH, mapper.extractEnergyFromLabels(labels))
    }

    @Test
    fun `extractEnergyFromLabels finds medium energy label`() {
        val labels = listOf(VikunjaLabelDto(id = 3, title = EnergyLevel.MEDIUM.labelTitle))
        assertEquals(EnergyLevel.MEDIUM, mapper.extractEnergyFromLabels(labels))
    }

    @Test
    fun `extractEnergyFromLabels finds low energy label`() {
        val labels = listOf(VikunjaLabelDto(id = 4, title = EnergyLevel.LOW.labelTitle))
        assertEquals(EnergyLevel.LOW, mapper.extractEnergyFromLabels(labels))
    }

    @Test
    fun `extractEnergyFromLabels returns null when no energy label`() {
        val labels = listOf(VikunjaLabelDto(id = 1, title = "urgent"))
        assertNull(mapper.extractEnergyFromLabels(labels))
    }

    @Test
    fun `domainToDto maps all task fields correctly`() {
        val dto = mapper.domainToDto(
            title = "Test task",
            description = "Some notes",
            done = false,
            dueDate = "2026-03-10T09:00:00+08:00",
            priority = 3,
            projectId = 5L,
            position = 1.5,
            estimatedMinutes = 30,
            repeatAfterSeconds = null,
            hexColor = "",
        )
        assertEquals("Test task", dto.title)
        assertEquals("Some notes\n<!-- tsosu:{\"est\":30} -->", dto.description)
        assertEquals(3L, dto.priority)
        assertEquals(5L, dto.projectId)
        assertEquals(1.5, dto.position)
    }

    @Test
    fun `dtoToDomain extracts estimatedMinutes from description`() {
        val dto = VikunjaTaskDto(
            id = 1,
            title = "Test",
            description = "Notes\n<!-- tsosu:{\"est\":45} -->",
            priority = 2,
        )
        val fields = mapper.dtoToDomainFields(dto)
        assertEquals(45, fields.estimatedMinutes)
        assertEquals("Notes", fields.cleanDescription)
        assertEquals(Priority.MEDIUM, fields.priority)
    }

    @Test
    fun `dtoToDomain preserves null estimate when no metadata`() {
        val dto = VikunjaTaskDto(id = 1, title = "Test", description = "Plain notes")
        val fields = mapper.dtoToDomainFields(dto)
        assertNull(fields.estimatedMinutes)
        assertEquals("Plain notes", fields.cleanDescription)
    }

    @Test
    fun `non-energy labels are preserved in extraction`() {
        val labels = listOf(
            VikunjaLabelDto(id = 1, title = "work"),
            VikunjaLabelDto(id = 2, title = EnergyLevel.HIGH.labelTitle),
        )
        val nonEnergy = mapper.getNonEnergyLabels(labels)
        assertEquals(1, nonEnergy.size)
        assertEquals("work", nonEnergy[0].title)
    }
}
