package app.tsosu.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Task(
    val id: String = generateId(),
    val serverId: Long? = null,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val dueDate: LocalDateTime? = null,
    val scheduledDate: LocalDateTime? = null,
    val startDate: LocalDateTime? = null,
    val reminderTime: LocalTime? = null,
    val completedDate: LocalDateTime? = null,
    val cancelledDate: LocalDateTime? = null,
    val priority: Priority = Priority.NONE,
    val labels: List<Label> = emptyList(),
    val projectId: String? = null,
    val position: Double = 0.0,
    val subtasks: List<Task> = emptyList(),
    val recurrenceRule: String? = null,
    val calendarEventId: String? = null,
    val estimatedMinutes: Int? = null,
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    val isFocus: Boolean = false,
    // Habit metadata when this task represents a recurring habit (unified model).
    val tinyVersion: String? = null,
    val routineTime: RoutineTime? = null,
    /** Dates of completed occurrences, oldest first — the streak source for recurring tasks. */
    val completions: List<LocalDate> = emptyList(),
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
) {
    val done: Boolean get() = status.isDone
}

@OptIn(ExperimentalUuidApi::class)
private fun generateId(): String = Uuid.random().toString()
