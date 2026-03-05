package app.tsosu.data.vikunja.mapper

import app.tsosu.data.vikunja.dto.VikunjaTaskDto
import app.tsosu.domain.model.HabitFrequency

class VikunjaHabitMapper {

    companion object {
        const val HABIT_MARKER = "-- Tsosu Habit"
    }

    fun habitToTaskDto(
        title: String,
        tinyVersion: String?,
        frequency: HabitFrequency,
        routineProjectId: Long,
        position: Double,
        hexColor: String,
    ): VikunjaTaskDto {
        return VikunjaTaskDto(
            title = title,
            description = buildHabitDescription(tinyVersion),
            repeatAfter = frequency.repeatAfterSeconds,
            repeatMode = 0,
            projectId = routineProjectId,
            position = position,
            hexColor = hexColor,
        )
    }

    private fun buildHabitDescription(tinyVersion: String?): String {
        return buildString {
            if (tinyVersion != null) append("Tiny version: $tinyVersion\n\n")
            append(HABIT_MARKER)
        }
    }

    fun extractTinyVersion(description: String?): String? {
        if (description == null) return null
        val match = Regex("Tiny version: (.+)").find(description)
        return match?.groupValues?.get(1)?.trim()
    }

    fun isHabitTask(dto: VikunjaTaskDto, routineProjectIds: Set<Long>): Boolean {
        return dto.repeatAfter > 0
            && dto.projectId in routineProjectIds
            && dto.description.contains(HABIT_MARKER)
    }
}
