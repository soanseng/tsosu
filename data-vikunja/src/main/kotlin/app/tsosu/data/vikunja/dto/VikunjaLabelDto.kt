package app.tsosu.data.vikunja.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VikunjaLabelDto(
    val id: Long = 0,
    val title: String = "",
    @SerialName("hex_color") val hexColor: String = "",
    val description: String = "",
)

@Serializable
data class VikunjaLabelTaskDto(
    @SerialName("label_id") val labelId: Long,
)

@Serializable
data class VikunjaBulkLabelsDto(
    val labels: List<VikunjaLabelDto>,
)
