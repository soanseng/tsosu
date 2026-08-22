package app.tsosu.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey

@Serializable
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,
    val title: String,
    val tinyVersion: String? = null,
    val frequency: Int = 0,
    /** Comma-joined ISO weekdays (1=Mon..7=Sun); null/empty = every scheduled day. */
    val weekdays: String? = null,
    val targetDaysPerWeek: Int = 7,
    val energyLevel: Int = 0,
    val routineId: String? = null,
    val projectId: String? = null,
    val position: Double = 0.0,
    val color: String = "#4CAF50",
    val isArchived: Boolean = false,
    val reminderMinutes: Int? = null,
    val createdAt: Long,
)
