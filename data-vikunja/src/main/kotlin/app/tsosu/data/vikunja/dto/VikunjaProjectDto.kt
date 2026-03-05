package app.tsosu.data.vikunja.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VikunjaProjectDto(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    @SerialName("hex_color") val hexColor: String = "",
    @SerialName("parent_project_id") val parentProjectId: Long = 0,
    val position: Double = 0.0,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
)
