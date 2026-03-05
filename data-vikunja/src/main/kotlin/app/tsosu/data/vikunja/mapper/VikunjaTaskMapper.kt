package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaLabelDto
import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority

class VikunjaTaskMapper {

    private val metadataRegex = Regex("\\s*<!-- tsosu:\\{.*?} -->\\s*")
    private val energyLabelTitles = EnergyLevel.entries.associateBy { it.labelTitle }

    fun appendEstimate(description: String, minutes: Int?): String {
        val cleaned = description.replace(metadataRegex, "").trimEnd()
        return if (minutes != null) {
            "$cleaned\n<!-- tsosu:{\"est\":$minutes} -->"
        } else {
            cleaned
        }
    }

    fun extractEstimate(description: String): Int? {
        return Regex("<!-- tsosu:\\{\"est\":(\\d+)} -->")
            .find(description)
            ?.groupValues?.get(1)?.toIntOrNull()
    }

    fun stripMetadata(description: String): String {
        return description.replace(metadataRegex, "").trimEnd()
    }

    fun extractEnergyFromLabels(labels: List<VikunjaLabelDto>): EnergyLevel? {
        return labels.firstNotNullOfOrNull { label ->
            energyLabelTitles[label.title]
        }
    }

    fun getNonEnergyLabels(labels: List<VikunjaLabelDto>): List<VikunjaLabelDto> {
        return labels.filter { label -> label.title !in energyLabelTitles }
    }

    fun domainToDto(
        title: String,
        description: String,
        done: Boolean,
        dueDate: String?,
        priority: Int,
        projectId: Long,
        position: Double,
        estimatedMinutes: Int?,
        repeatAfterSeconds: Long?,
        hexColor: String,
    ): VikunjaTaskDto {
        return VikunjaTaskDto(
            title = title,
            description = appendEstimate(description, estimatedMinutes),
            done = done,
            dueDate = dueDate,
            priority = priority.toLong(),
            projectId = projectId,
            position = position,
            repeatAfter = repeatAfterSeconds ?: 0,
            hexColor = hexColor,
        )
    }

    data class DomainFields(
        val cleanDescription: String,
        val estimatedMinutes: Int?,
        val energyLevel: EnergyLevel?,
        val priority: Priority,
    )

    fun dtoToDomainFields(dto: VikunjaTaskDto): DomainFields {
        return DomainFields(
            cleanDescription = stripMetadata(dto.description),
            estimatedMinutes = extractEstimate(dto.description),
            energyLevel = extractEnergyFromLabels(dto.labels),
            priority = Priority.fromValue(dto.priority.toInt()),
        )
    }
}
