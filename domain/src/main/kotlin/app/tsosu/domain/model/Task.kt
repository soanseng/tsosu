package app.tsosu.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Task(
    val id: String = generateId(),
    val serverId: Long? = null,
    val title: String,
    val description: String = "",
    val done: Boolean = false,
    val dueDate: LocalDateTime? = null,
    val priority: Priority = Priority.NONE,
    val labels: List<Label> = emptyList(),
    val projectId: String? = null,
    val position: Double = 0.0,
    val subtasks: List<Task> = emptyList(),
    val repeatAfter: Duration? = null,
    val calendarEventId: String? = null,
    val estimatedMinutes: Int? = null,
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    val isFocus: Boolean = false,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
)

@OptIn(ExperimentalUuidApi::class)
private fun generateId(): String = Uuid.random().toString()
