package app.tsosu.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,
    val title: String,
    val timeOfDay: Int = 0,
)
