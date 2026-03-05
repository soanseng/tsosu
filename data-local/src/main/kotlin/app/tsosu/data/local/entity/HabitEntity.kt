package app.tsosu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,
    val title: String,
    val tinyVersion: String? = null,
    val frequency: Int = 0,
    val targetDaysPerWeek: Int = 7,
    val energyLevel: Int = 0,
    val routineId: String? = null,
    val position: Double = 0.0,
    val color: String = "#4CAF50",
    val isArchived: Boolean = false,
    val createdAt: Long,
)
