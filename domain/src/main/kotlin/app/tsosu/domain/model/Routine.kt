package app.tsosu.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Routine(
    val id: String = generateId(),
    val serverId: Long? = null,
    val title: String,
    val timeOfDay: RoutineTime,
    val habits: List<Habit> = emptyList(),
)

@OptIn(ExperimentalUuidApi::class)
private fun generateId(): String = Uuid.random().toString()
