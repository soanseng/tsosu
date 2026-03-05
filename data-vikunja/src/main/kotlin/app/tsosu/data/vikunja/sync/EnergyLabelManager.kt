package app.tsosu.data.vikunja.sync

import app.tsosu.data.vikunja.api.VikunjaApi
import app.tsosu.data.vikunja.dto.VikunjaLabelDto
import app.tsosu.domain.model.EnergyLevel

class EnergyLabelManager(private val api: VikunjaApi) {

    private val labelIds = mutableMapOf<EnergyLevel, Long>()

    fun getLabelId(level: EnergyLevel): Long? = labelIds[level]

    suspend fun ensureLabelsExist(): Map<EnergyLevel, Long> {
        val existingLabels = api.getLabels(page = 1, perPage = 200)

        for (level in EnergyLevel.entries) {
            val existing = existingLabels.firstOrNull { it.title == level.labelTitle }
            if (existing != null) {
                labelIds[level] = existing.id
            } else {
                val colorHex = when (level) {
                    EnergyLevel.HIGH -> "4CAF50"
                    EnergyLevel.MEDIUM -> "FFC107"
                    EnergyLevel.LOW -> "90A4AE"
                }
                val created = api.createLabel(
                    VikunjaLabelDto(
                        title = level.labelTitle,
                        hexColor = colorHex,
                    )
                )
                labelIds[level] = created.id
            }
        }

        return labelIds.toMap()
    }
}
