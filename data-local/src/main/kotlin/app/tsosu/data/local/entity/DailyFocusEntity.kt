package app.tsosu.data.local.entity

import androidx.room.Entity
import kotlinx.serialization.Serializable
import androidx.room.PrimaryKey

@Serializable
@Entity(tableName = "daily_focus")
data class DailyFocusEntity(
    @PrimaryKey val date: Long,
    val taskId1: String?,
    val taskId2: String?,
    val taskId3: String?,
)
