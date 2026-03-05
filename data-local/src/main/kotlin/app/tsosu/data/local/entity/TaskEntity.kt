package app.tsosu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,
    val title: String,
    val description: String = "",
    val done: Boolean = false,
    val doneAt: Long? = null,
    val dueDate: Long? = null,
    val priority: Int = 0,
    val projectId: String? = null,
    val position: Double = 0.0,
    val repeatAfterSeconds: Long? = null,
    val calendarEventId: String? = null,
    val estimatedMinutes: Int? = null,
    val energyLevel: Int = 1,
    val isFocus: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
)
