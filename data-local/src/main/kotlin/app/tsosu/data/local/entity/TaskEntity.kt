package app.tsosu.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey

@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,
    val title: String,
    val description: String = "",
    val status: Int = 0,
    val done: Boolean = false,
    val doneAt: Long? = null,
    val dueDate: Long? = null,
    val scheduledDate: Long? = null,
    val startDate: Long? = null,
    val reminderTimeMinutes: Int? = null,
    val completedDate: Long? = null,
    val cancelledDate: Long? = null,
    val priority: Int = 0,
    val projectId: String? = null,
    val position: Double = 0.0,
    val repeatAfterSeconds: Long? = null,
    val recurrenceRule: String? = null,
    val calendarEventId: String? = null,
    val estimatedMinutes: Int? = null,
    val energyLevel: Int = 1,
    val isFocus: Boolean = false,
    val tinyVersion: String? = null,
    val routineTime: Int? = null,
    val completionsCsv: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
)
