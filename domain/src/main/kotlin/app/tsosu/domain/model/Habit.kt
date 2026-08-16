package app.tsosu.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Habit(
    val id: String = generateId(),
    val serverId: Long? = null,
    val title: String,
    val tinyVersion: String? = null,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    /** ISO weekdays (1=Mon..7=Sun) the habit is scheduled on; empty = every scheduled day. */
    val weekdays: Set<Int> = emptySet(),
    val targetDaysPerWeek: Int = 7,
    val energyLevel: EnergyLevel = EnergyLevel.LOW,
    val routineId: String? = null,
    val projectId: String? = null,
    val position: Double = 0.0,
    val color: String = "#4CAF50",
    val isArchived: Boolean = false,
    val reminderTime: LocalTime? = null,
    val createdAt: Instant = Clock.System.now(),
)

@OptIn(ExperimentalUuidApi::class)
private fun generateId(): String = Uuid.random().toString()
