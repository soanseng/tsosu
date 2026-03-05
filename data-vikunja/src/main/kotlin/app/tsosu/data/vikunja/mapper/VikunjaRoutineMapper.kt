package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaProjectDto
import app.tsosu.domain.model.RoutineTime

class VikunjaRoutineMapper {

    private val routineRegex = Regex("<!-- tsosu-routine:(\\w+) -->")

    fun routineToProjectDto(
        title: String,
        timeOfDay: RoutineTime,
    ): VikunjaProjectDto {
        return VikunjaProjectDto(
            title = title,
            description = "<!-- tsosu-routine:${timeOfDay.name} -->",
        )
    }

    fun extractRoutineTime(description: String?): RoutineTime? {
        if (description == null) return null
        val match = routineRegex.find(description) ?: return null
        return try {
            RoutineTime.valueOf(match.groupValues[1])
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun isRoutineProject(dto: VikunjaProjectDto): Boolean {
        return dto.description.contains("<!-- tsosu-routine:")
    }
}
