package app.tsosu.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Label(
    val id: String = generateId(),
    val serverId: Long? = null,
    val title: String,
    val color: String = "#4287f5",
)

@OptIn(ExperimentalUuidApi::class)
private fun generateId(): String = Uuid.random().toString()
