package app.tsosu.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Project(
    val id: String = generateId(),
    val serverId: Long? = null,
    val title: String,
    val color: String = "#808080",
    val parentProjectId: String? = null,
    val position: Double = 0.0,
    val isFavorite: Boolean = false,
    val isRoutine: Boolean = false,
)

@OptIn(ExperimentalUuidApi::class)
private fun generateId(): String = Uuid.random().toString()
