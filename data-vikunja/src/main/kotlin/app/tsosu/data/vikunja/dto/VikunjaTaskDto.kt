package app.tsosu.data.vikunja.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VikunjaTaskDto(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val done: Boolean = false,
    @SerialName("done_at") val doneAt: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val priority: Long = 0,
    @SerialName("project_id") val projectId: Long = 0,
    val position: Double = 0.0,
    @SerialName("repeat_after") val repeatAfter: Long = 0,
    @SerialName("repeat_mode") val repeatMode: Int = 0,
    @SerialName("hex_color") val hexColor: String = "",
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    val labels: List<VikunjaLabelDto> = emptyList(),
    @SerialName("percent_done") val percentDone: Double = 0.0,
    val created: String? = null,
    val updated: String? = null,
)
