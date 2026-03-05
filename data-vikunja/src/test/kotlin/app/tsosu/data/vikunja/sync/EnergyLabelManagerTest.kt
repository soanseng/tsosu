package app.tsosu.data.vikunja.sync

import app.tsosu.data.vikunja.api.VikunjaApi
import app.tsosu.data.vikunja.dto.VikunjaLabelDto
import app.tsosu.domain.model.EnergyLevel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EnergyLabelManagerTest {

    private val api = mockk<VikunjaApi>()
    private val manager = EnergyLabelManager(api)

    @Test
    fun `ensureLabelsExist creates missing labels`() = runTest {
        coEvery { api.getLabels(any(), any()) } returns emptyList()
        coEvery { api.createLabel(any()) } answers {
            val input = firstArg<VikunjaLabelDto>()
            input.copy(id = 100)
        }

        val ids = manager.ensureLabelsExist()

        assertEquals(3, ids.size)
        coVerify(exactly = 3) { api.createLabel(any()) }
    }

    @Test
    fun `ensureLabelsExist reuses existing labels`() = runTest {
        val existing = EnergyLevel.entries.mapIndexed { i, level ->
            VikunjaLabelDto(id = (i + 1).toLong(), title = level.labelTitle, hexColor = "")
        }
        coEvery { api.getLabels(any(), any()) } returns existing

        val ids = manager.ensureLabelsExist()

        assertEquals(3, ids.size)
        coVerify(exactly = 0) { api.createLabel(any()) }
    }

    @Test
    fun `getLabelId returns id after initialization`() = runTest {
        val existing = EnergyLevel.entries.mapIndexed { i, level ->
            VikunjaLabelDto(id = (i + 10).toLong(), title = level.labelTitle, hexColor = "")
        }
        coEvery { api.getLabels(any(), any()) } returns existing

        manager.ensureLabelsExist()

        assertEquals(10L, manager.getLabelId(EnergyLevel.LOW))
        assertEquals(11L, manager.getLabelId(EnergyLevel.MEDIUM))
        assertEquals(12L, manager.getLabelId(EnergyLevel.HIGH))
    }
}
